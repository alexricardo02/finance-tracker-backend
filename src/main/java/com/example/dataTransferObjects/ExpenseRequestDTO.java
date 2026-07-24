package com.example.dataTransferObjects;

import com.example.models.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ExpenseRequestDTO {

	@Positive(message = "The amount must be positive")
	private Double amount;

	private String currency;

	@NotNull(message = "The date is required")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate date;

	@NotNull(message = "The category is required")
	private Integer categoryId; // Added field

	private String description;
	
	@NotNull(message = "Payment method is mandatory")
    private PaymentMethod paymentMethod;

	@NotNull(message = "User ID is required")
	@Positive(message = "User ID must be positive")
	private Integer userId; // Avoid exposing User entity

	public ExpenseRequestDTO() {
		super();
	}

	public ExpenseRequestDTO(@Positive(message = "The amount must be positive") Double amount,
			@NotBlank(message = "The date is required") LocalDate date,
			@NotNull(message = "The category is required") Integer categoryId,
			@Positive(message = "User ID must be positive") Integer userId) {
		super();
		this.amount = amount;
		this.date = date;
		this.categoryId = categoryId;
		this.userId = userId;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Integer getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Integer categoryId) {
		this.categoryId = categoryId;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(PaymentMethod paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	@Override
	public String toString() {
		return "ExpenseRequestDTO [amount=" + amount + ", currency=" + currency + ", date=" + date + ", categoryId="
				+ categoryId + ", description=" + description + ", userId=" + userId + "]";
	}

}
