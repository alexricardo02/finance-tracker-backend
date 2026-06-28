package com.example.dataTransferObjects;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ExpenseRequestDTO {

	@Positive(message = "El monto debe ser positivo")
	private Double amount;

	private String currency;

	@NotNull(message = "La fecha es obligatoria")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate date;

	@NotNull(message = "La categoría es obligatoria")
	private Integer categoryId; // Added field

	private String description;

	@NotNull(message = "User ID is required")
	@Positive(message = "User ID must be positive")
	private Integer userId; // Avoid exposing User entity

	public ExpenseRequestDTO() {
		super();
	}

	public ExpenseRequestDTO(@Positive(message = "El monto debe ser positivo") Double amount,
			@NotBlank(message = "La fecha es obligatoria") LocalDate date,
			@NotNull(message = "La categoría es obligatoria") Integer categoryId,
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

	@Override
	public String toString() {
		return "ExpenseRequestDTO [amount=" + amount + ", currency=" + currency + ", date=" + date + ", categoryId="
				+ categoryId + ", description=" + description + ", userId=" + userId + "]";
	}

}
