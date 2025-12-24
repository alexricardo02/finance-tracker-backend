package com.example.dataTransferObjects;

import java.time.LocalDate;


public class IncomeResponseDTO {
	
	private Integer incomeId;
	private Double amount;
	private String currency;
    private LocalDate date;
    private String type;
    private Integer userId;
    private String description;
    
    public static class IncomeUserDTO {
        private Integer userId;
        private String username;
        
		public Integer getUserId() {
			return userId;
		}
		public void setUserId(Integer userId) {
			this.userId = userId;
		}
		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
         
    }
    
    
    

	public Integer getIncomeId() {
		return incomeId;
	}

	public void setIncomeId(Integer incomeId) {
		this.incomeId = incomeId;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public IncomeResponseDTO(Integer incomeId, Double amount, String currency, LocalDate date, String type,
			String description, Integer userId) {
		super();
		this.incomeId = incomeId;
		this.amount = amount;
		this.currency = currency;
		this.date = date;
		this.type = type;
		this.description = description;
		this.userId = userId;
	}
	

	public IncomeResponseDTO() {
		super();
	}
    
    

}
