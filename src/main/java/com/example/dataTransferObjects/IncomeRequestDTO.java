package com.example.dataTransferObjects;

import java.time.LocalDate;

import com.example.models.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class IncomeRequestDTO {

	@NotNull(message = "Amount is required")
	@Positive(message = "El income tiene que ser positivo")
    private Double amount;
	
	@NotNull
	private String currency;

	@NotNull(message = "La fecha es obligatoria")
    private LocalDate date;
    
	@NotNull(message = "La categoría es obligatoria")
    private Integer categoryId; 
	
	private String description;
	
	@NotNull(message = "Payment method is mandatory")
    private PaymentMethod paymentMethod;
    
	@NotNull(message = "El usuario debe existir")
    private Integer userId;
    
    
    
	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
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

	public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
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

	public IncomeRequestDTO() {
		super();
	}
    
	
}
