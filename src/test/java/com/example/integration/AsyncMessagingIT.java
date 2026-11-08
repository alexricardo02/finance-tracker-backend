package com.example.integration;

import com.example.config.OutboxRelayJob;
import com.example.config.RabbitMQConfig;
import com.example.models.OutboxEvent;
import com.example.repository.OutboxEventRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class AsyncMessagingIT extends AbstractApiIntegrationTest {

    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private RabbitTemplate rabbitTemplate;

    private ResponseEntity<String> forgotPassword(String email) {
        return call(HttpMethod.POST, "/api/users/forgot-password", null, Map.of("email", email));
    }

    private OutboxRelayJob relayJob() {
        OutboxRelayJob job = new OutboxRelayJob();
        ReflectionTestUtils.setField(job, "outboxEventRepository", outboxEventRepository);
        ReflectionTestUtils.setField(job, "rabbitTemplate", rabbitTemplate);
        return job;
    }

    private String currencyChangedPayload(ApiUser user, String currency) {
        return String.format("{\"userId\": %d, \"username\": \"%s\", \"newCurrency\": \"%s\"}",
                user.id(), user.username(), currency);
    }

    @Test
    void passwordResetEmail_isSentWithTokenAfterRequestReturns() throws Exception {
        ApiUser user = newUser();
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean sent = new AtomicBoolean();
        doAnswer(invocation -> {
            release.await(10, TimeUnit.SECONDS);
            sent.set(true);
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));

        ResponseEntity<String> response = forgotPassword(user.email());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(sent).isFalse();
        release.countDown();
        await().atMost(Duration.ofSeconds(5)).untilTrue(sent);

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        String token = jdbc.queryForObject("SELECT token FROM password_reset_tokens WHERE user_id = ?", String.class, user.id());
        assertThat(mail.getValue().getTo()).containsExactly(user.email());
        assertThat(mail.getValue().getText()).contains("/reset-password?token=" + token);
    }

    @Test
    void passwordResetEmail_smtpFailure_doesNotAffectResponse() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThat(forgotPassword(newUser().email()).getStatusCode().value()).isEqualTo(200);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(mailSender).send(any(SimpleMailMessage.class)));
    }

    @Test
    void currencyChange_writesOutboxEventAndRecalculates() {
        ApiUser user = newUser();
        int income = createIncome(user, 100, "x");
        stubFrankfurter("USD", "EUR", TX_DATE, 0.5);

        ResponseEntity<String> response = call(HttpMethod.PUT, "/api/settings/currency", user, Map.of("primaryCurrency", "EUR"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT amount_primary_currency FROM incomes WHERE income_id = ?", Double.class, income)).isEqualTo(50.0);
        assertThat(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc())
                .filteredOn(event -> event.getPayload().contains(user.username()))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getEventType()).isEqualTo("PRIMARY_CURRENCY_CHANGED");
                    assertThat(event.getPayload()).contains("\"newCurrency\": \"EUR\"", "\"userId\": " + user.id());
                });
    }

    @Test
    void currencyChange_sameCurrency_writesNoOutboxEvent() {
        ApiUser user = newUser();

        call(HttpMethod.PUT, "/api/settings/currency", user, Map.of("primaryCurrency", "USD"));

        assertThat(outboxEventRepository.findAll())
                .noneMatch(event -> event.getPayload().contains(user.username()));
    }

    @Test
    void outboxRelay_publishesPendingEventAndMarksItProcessed() {
        ApiUser user = newUser();
        OutboxEvent event = outboxEventRepository.save(
                new OutboxEvent("PRIMARY_CURRENCY_CHANGED", currencyChangedPayload(user, "USD")));

        relayJob().processOutboxEvents();

        assertThat(outboxEventRepository.findById(event.getId()).orElseThrow().isProcessed()).isTrue();
    }

    @Disabled("BUG-7: relayed outbox message cannot be converted to PrimaryCurrencyChangedEvent and is dead-lettered")
    @Test
    void relayedCurrencyEvent_isConsumedAndRecalculatesTransactions() {
        ApiUser user = newUser();
        int income = createIncome(user, 100, "x");
        stubFrankfurter("USD", "EUR", TX_DATE, 0.5);
        outboxEventRepository.save(new OutboxEvent("PRIMARY_CURRENCY_CHANGED", currencyChangedPayload(user, "EUR")));

        relayJob().processOutboxEvents();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("SELECT amount_primary_currency FROM incomes WHERE income_id = ?", Double.class, income))
                        .isEqualTo(50.0));
    }

    @Test
    void poisonMessage_isRoutedToDeadLetterQueue() {
        String marker = "poison-" + UUID.randomUUID();
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        rabbitTemplate.send(RabbitMQConfig.EXCHANGE, RabbitMQConfig.CURRENCY_ROUTING_KEY,
                new Message(marker.getBytes(StandardCharsets.UTF_8), properties));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Message dead = rabbitTemplate.receive(RabbitMQConfig.DLQ);
            assertThat(dead).isNotNull();
            assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).contains(marker);
        });
    }

    @Test
    void listener_hasActiveConsumerOnCurrencyQueue() {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Integer consumers = rabbitTemplate.execute(channel ->
                    channel.consumerCount(RabbitMQConfig.CURRENCY_QUEUE) > 0 ? 1 : 0);
            assertThat(consumers).isEqualTo(1);
        });
    }
}
