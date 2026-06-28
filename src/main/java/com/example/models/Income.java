package com.example.models;

import java.time.LocalDate;


import jakarta.persistence.*;

@Entity
@Table(name="incomes", indexes = {
		@Index(name = "idx_income_user", columnList = "user_id"),
		@Index(name = "idx_income_category", columnList = "category_id"),
		@Index(name = "idx_income_date", columnList = "date")
})
public class Income {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="income_id", nullable = false)
	private Integer incomeId;
	
	@Column(name="amount", nullable = false)
    private double amount;
	
	@Column(name="currency", nullable = false)
	private String currency;
	
	@Column(name="date", nullable = false)
    private LocalDate date;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="category_id", nullable = true)
    private Category category;
	
	@Column(name="description")
    private String description;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="user_id", nullable = false)
	private User user;
	

    public Income(Integer incomeId, double amount, String currency, LocalDate date, User user, Category category, String type, String description) {
        this.incomeId = incomeId;
    	this.amount = amount;
        this.currency = currency;
        this.date = date;
        this.user = user;
        this.category = category;
        this.description = description;
    }

    public Income() {
    }
    
    public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public int getIncomeId() {
		return incomeId;
	}

	public void setIncomeId(int incomeId) {
		this.incomeId = incomeId;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}


}
