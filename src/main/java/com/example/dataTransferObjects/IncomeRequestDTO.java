package com.example.dataTransferObjects;

import java.time.LocalDate;


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

	public IncomeRequestDTO(@Positive(message = "El income tiene que ser positivo") Double amount,
			@NotNull(message = "La fecha es obligatoria") LocalDate date,
			@NotNull(message = "El tipo es obligatorio") Integer categoryId,
			@NotNull(message = "El usuario debe existir") Integer userId, String description) {
		super();
		this.amount = amount;
		this.date = date;
		this.categoryId = categoryId;
		this.userId = userId;
		this.description = description;
	}

	public IncomeRequestDTO() {
		super();
	}
    
	
}
