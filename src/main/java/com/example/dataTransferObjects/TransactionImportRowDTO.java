package com.example.dataTransferObjects;

import com.example.models.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class TransactionImportRowDTO {

    @NotBlank
    @Pattern(regexp = "income|expense", message = "kind must be 'income' or 'expense'")
    private String kind;

    @NotNull @Positive
    private Double amount;

    @NotBlank
    private String currency;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotBlank
    private String categoryName;

    private String description;

    private PaymentMethod paymentMethod = PaymentMethod.OTHER;

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}