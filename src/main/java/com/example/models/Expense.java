package com.example.models;

import java.time.LocalDate;


import jakarta.persistence.*;

@Entity
@Table(name="expenses")
public class Expense {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id", nullable = false)
    private Integer id;
	
	@Column(name="amount", nullable = false)
    private double amount;
	
	@Column(name="currency", nullable = false)
	private String currency;
	
	@Column(name="date", nullable = false)
    private LocalDate date;
	
	@Column(length = 100)
    private String type;
	
	@Column(name = "expense_description", columnDefinition = "text")
    private String description;
    
    @ManyToOne
	@JoinColumn(name="user_id")
    private User user;
   

    public Expense() {
    }

    public Expense(int id, double amount, String currency, LocalDate date, String type, String description) {
        this.id = id;
        this.amount = amount;
        this.currency = currency;
        this.date = date;
        this.type = type;
        this.description = description;
    }
    
    

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public int getExpenseID() {
		return id;
	}

	public void setExpenseID(int expenseID) {
		this.id = expenseID;
	}

	public double getExpenseAmount() {
		return amount;
	}

	public void setExpenseAmount(double expenseAmount) {
		this.amount = expenseAmount;
	}

	public LocalDate getExpenseDate() {
		return date;
	}

	public void setExpenseDate(LocalDate expenseDate) {
		this.date = expenseDate;
	}

	public String getExpenseType() {
		return type;
	}

	public void setExpenseType(String expenseType) {
		this.type = expenseType;
	}

	public String getExpenseDescription() {
		return description;
	}

	public void setExpenseDescription(String expenseDescription) {
		this.description = expenseDescription;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
    
    


}
