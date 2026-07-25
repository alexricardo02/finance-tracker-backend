package com.example.service;

import com.example.dataTransferObjects.UserSettingsDTO;
import com.example.events.PrimaryCurrencyChangedEvent;
import com.example.events.PrimaryCurrencyChangedListener;
import com.example.models.OutboxEvent;
import com.example.models.User;
import com.example.repository.OutboxEventRepository;
import com.example.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private PrimaryCurrencyChangedListener currencyRecalculationHandler;

    @InjectMocks private SettingsService settingsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("john", "john@test.com", "hash", new Date());
        user.setUser_id(1);
        user.setPrimaryCurrency("USD");
    }

    @Test
    void getSettings_returnsCurrentCurrency() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        UserSettingsDTO result = settingsService.getSettings("john");

        assertThat(result.getPrimaryCurrency()).isEqualTo("USD");
    }

    @Test
    void getSettings_userNotFound_throwsEntityNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settingsService.getSettings("ghost"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updatePrimaryCurrency_unsupportedCurrency_throwsIllegalArgumentException_noSideEffects() {
        assertThatThrownBy(() -> settingsService.updatePrimaryCurrency("john", "XXX"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(userRepository, outboxEventRepository, currencyRecalculationHandler);
    }

    @Test
    void updatePrimaryCurrency_nullCurrency_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> settingsService.updatePrimaryCurrency("john", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updatePrimaryCurrency_sameCurrency_isNoOp() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        UserSettingsDTO result = settingsService.updatePrimaryCurrency("john", "USD");

        assertThat(result.getPrimaryCurrency()).isEqualTo("USD");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(currencyRecalculationHandler, outboxEventRepository);
    }

    @Test
    void updatePrimaryCurrency_newCurrency_savesUser_recalculatesAndWritesOutboxEvent() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        UserSettingsDTO result = settingsService.updatePrimaryCurrency("john", "EUR");

        assertThat(result.getPrimaryCurrency()).isEqualTo("EUR");
        assertThat(user.getPrimaryCurrency()).isEqualTo("EUR");
        verify(userRepository).save(user);

        ArgumentCaptor<PrimaryCurrencyChangedEvent> eventCaptor = ArgumentCaptor.forClass(PrimaryCurrencyChangedEvent.class);
        verify(currencyRecalculationHandler).recalculate(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getNewCurrency()).isEqualTo("EUR");
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(1);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("PRIMARY_CURRENCY_CHANGED");
        assertThat(outboxCaptor.getValue().getPayload()).contains("\"newCurrency\": \"EUR\"");
    }

    @Test
    void updatePrimaryCurrency_userNotFound_throwsEntityNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settingsService.updatePrimaryCurrency("ghost", "EUR"))
                .isInstanceOf(EntityNotFoundException.class);
    }
}