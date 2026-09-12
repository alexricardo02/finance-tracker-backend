package com.example.dataTransferObjects;

import java.time.LocalDate;

import com.example.models.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class IncomeRequestDTO {

	@NotNull(message = "Amount is required")
	@Positive(message = "The income amount must be positive")
    private Double amount;
	
	@NotNull
	private String currency;

	@NotNull(message = "The date is required")
    private LocalDate date;
    
	@NotNull(message = "The category is required")
    private Integer categoryId; 
	
	private String description;
	
	@NotNull(message = "Payment method is mandatory")
    private PaymentMethod paymentMethod;
    
    // H-3 fix: removed userId field.
    // The client MUST NOT supply a userId — it would allow any authenticated user to claim
    // another user's ID (Broken Object Level Authorization / IDOR).
    // The owning user is always resolved from Principal.getName() in the controller,
    // then looked up in the DB inside the service. No client input is trusted for ownership.
    
    
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
