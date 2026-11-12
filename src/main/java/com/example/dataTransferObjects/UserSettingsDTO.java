package com.example.dataTransferObjects;

import com.example.validation.SupportedCurrency;
import jakarta.validation.constraints.NotBlank;

public class UserSettingsDTO {

    @NotBlank(message = "Currency is required")
    @SupportedCurrency
    private String primaryCurrency;

    public UserSettingsDTO() {}

    public UserSettingsDTO(String primaryCurrency) {
        this.primaryCurrency = primaryCurrency;
    }

    public String getPrimaryCurrency() { return primaryCurrency; }
    public void setPrimaryCurrency(String primaryCurrency) { this.primaryCurrency = primaryCurrency; }
}