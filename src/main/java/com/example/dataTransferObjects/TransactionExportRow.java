package com.example.dataTransferObjects;

import java.time.LocalDate;

public class TransactionExportRow {
    private LocalDate date;
    private String kind;
    private String category;
    private String description;
    private String paymentMethod;
    private double amount;
    private String currency;
    private Double amountPrimaryCurrency;
    private String primaryCurrency;

    public TransactionExportRow() {}

    public TransactionExportRow(LocalDate date, String kind, String category, String description,
                                 String paymentMethod, double amount, String currency,
                                 Double amountPrimaryCurrency, String primaryCurrency) {
        this.date = date;
        this.kind = kind;
        this.category = category;
        this.description = description;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.currency = currency;
        this.amountPrimaryCurrency = amountPrimaryCurrency;
        this.primaryCurrency = primaryCurrency;
    }

    public LocalDate getDate() { return date; }
    public String getKind() { return kind; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getPaymentMethod() { return paymentMethod; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Double getAmountPrimaryCurrency() { return amountPrimaryCurrency; }
    public String getPrimaryCurrency() { return primaryCurrency; }
}