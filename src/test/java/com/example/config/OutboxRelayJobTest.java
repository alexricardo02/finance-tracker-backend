package com.example.config;

import com.example.models.OutboxEvent;
import com.example.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayJobTest {

    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private OutboxRelayJob outboxRelayJob;

    @Test
    void processOutboxEvents_noPendingEvents_doesNothing() {
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of());

        outboxRelayJob.processOutboxEvents();

        verifyNoInteractions(rabbitTemplate);
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void processOutboxEvents_pendingEvent_sendsToCorrectExchangeAndRoutingKey() {
        // WHY: pins the exact exchange name and routing key so that reverting to
        // placeholder strings ("tu_exchange_name", "tu_routing_key") causes an
        // immediate test failure rather than a silent misconfiguration in production.
        OutboxEvent event = new OutboxEvent("PRIMARY_CURRENCY_CHANGED", "{\"userId\":1}");
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));

        outboxRelayJob.processOutboxEvents();

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.CURRENCY_ROUTING_KEY,
                "{\"userId\":1}");
        assertThat(event.isProcessed()).isTrue();
        verify(outboxEventRepository).save(event);
    }

    @Test
    void processOutboxEvents_rabbitThrows_eventStaysUnprocessed_noExceptionPropagates() {
        OutboxEvent event = new OutboxEvent("PRIMARY_CURRENCY_CHANGED", "{\"userId\":1}");
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        doThrow(new RuntimeException("broker unreachable"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        // WHY: a scheduled job must never let an exception escape, or Spring's @Scheduled
        // will stop invoking it on subsequent runs.
        outboxRelayJob.processOutboxEvents();

        assertThat(event.isProcessed()).isFalse();
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void processOutboxEvents_multipleEvents_oneFailureDoesNotBlockOthers() {
        OutboxEvent failing = new OutboxEvent("TYPE_A", "payload-a");
        OutboxEvent succeeding = new OutboxEvent("TYPE_B", "payload-b");
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(failing, succeeding));

        doThrow(new RuntimeException("fail"))
                .doNothing()
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        outboxRelayJob.processOutboxEvents();

        assertThat(failing.isProcessed()).isFalse();
        assertThat(succeeding.isProcessed()).isTrue();
        verify(outboxEventRepository, times(1)).save(succeeding);
        verify(outboxEventRepository, never()).save(failing);
    }
}
