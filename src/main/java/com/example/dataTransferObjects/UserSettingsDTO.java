package com.example.dataTransferObjects;

import jakarta.validation.constraints.NotBlank;

public class UserSettingsDTO {

    @NotBlank(message = "Currency is required")
    private String primaryCurrency;

    public UserSettingsDTO() {}

    public UserSettingsDTO(String primaryCurrency) {
        this.primaryCurrency = primaryCurrency;
    }

    public String getPrimaryCurrency() { return primaryCurrency; }
    public void setPrimaryCurrency(String primaryCurrency) { this.primaryCurrency = primaryCurrency; }
}