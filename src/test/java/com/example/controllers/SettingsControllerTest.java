package com.example.controllers;

import com.example.dataTransferObjects.UserSettingsDTO;
import com.example.service.SettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsControllerTest {

    @Mock private SettingsService settingsService;

    @InjectMocks private SettingsController settingsController;

    @Test
    void getSettings_delegatesToService_returnsSettingsDTO() {
        Principal principal = () -> "john";
        when(settingsService.getSettings("john")).thenReturn(new UserSettingsDTO("EUR"));

        UserSettingsDTO result = settingsController.getSettings(principal);

        assertThat(result.getPrimaryCurrency()).isEqualTo("EUR");
        verify(settingsService).getSettings("john");
    }

    @Test
    void updatePrimaryCurrency_validCurrency_delegatesToService_returnsUpdatedSettings() {
        Principal principal = () -> "john";
        UserSettingsDTO dto = new UserSettingsDTO("GBP");
        when(settingsService.updatePrimaryCurrency("john", "GBP")).thenReturn(new UserSettingsDTO("GBP"));

        UserSettingsDTO result = settingsController.updatePrimaryCurrency(principal, dto);

        assertThat(result.getPrimaryCurrency()).isEqualTo("GBP");
        verify(settingsService).updatePrimaryCurrency("john", "GBP");
    }

    @Test
    void updatePrimaryCurrency_sameCurrency_stillDelegatesToService() {
        // WHY: the service is responsible for the no-op check — the controller
        // must not short-circuit it so the service can return the correct DTO.
        Principal principal = () -> "john";
        UserSettingsDTO dto = new UserSettingsDTO("USD");
        when(settingsService.updatePrimaryCurrency("john", "USD")).thenReturn(new UserSettingsDTO("USD"));

        UserSettingsDTO result = settingsController.updatePrimaryCurrency(principal, dto);

        assertThat(result.getPrimaryCurrency()).isEqualTo("USD");
        verify(settingsService).updatePrimaryCurrency("john", "USD");
    }
}
