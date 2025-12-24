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

    @NotBlank(message = "El tipo es obligatorio")
    private String typeName;  // Added field
    
	private String description;
	
	@NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Integer userId; // Avoid exposing User entity
	
    
	public ExpenseRequestDTO() {
		super();
	}
	
	//PROVISORIO
	public ExpenseRequestDTO(@NotBlank(message = "El subtipo es obligatorio") String typeName,
			@Positive(message = "El monto debe ser positivo") Double amount,
			@NotBlank(message = "La fecha es obligatoria") LocalDate date,
			@NotBlank(message = "El currency es obligatorio") String currency,
			String description) {
		super();
		this.amount = amount;
		this.currency = currency;
		this.date = date;
		this.typeName = typeName;
		this.description = description;
	}
	
	public ExpenseRequestDTO(@Positive(message = "El monto debe ser positivo") Double amount,
			@NotBlank(message = "La fecha es obligatoria") LocalDate date,
			@NotBlank(message = "El tipo es obligatorio") String typeName,
			@Positive(message = "User ID must be positive") Integer userId) {
		super();
		this.amount = amount;
		this.date = date;
		this.typeName = typeName;
		this.userId = userId;
	}
	
	
	
	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getTypeName() {
		return typeName;
	}
	public void setTypeName(String typeName) {
		this.typeName = typeName;
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
		return "ExpenseRequestDTO [amount=" + amount + ", currency=" + currency + ", date=" + date + ", typeName="
				+ typeName + ", description=" + description + ", userId=" + userId + "]";
	}
	
	
	
	
}
