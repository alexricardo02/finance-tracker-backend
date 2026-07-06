package com.example.events;

public class PrimaryCurrencyChangedEvent {

    private final Integer userId;
    private final String username;
    private final String newCurrency;

    public PrimaryCurrencyChangedEvent(Integer userId, String username, String newCurrency) {
        this.userId = userId;
        this.username = username;
        this.newCurrency = newCurrency;
    }

    public Integer getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getNewCurrency() { return newCurrency; }
}