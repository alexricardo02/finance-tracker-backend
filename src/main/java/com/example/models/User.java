package com.example.models;

import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="users")
public class User {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="user_id", nullable = false)
	private int userId;
	
	@Column(name="username", nullable = false, unique = true)
    private String username;
	
	@Column(name="password_hash", nullable = false)
    private String passwordHash;
	
	@Column(name="email", nullable = false, unique = true)
    private String email;
	
	@Column(name="creation_date", nullable = false)
    private Date creationDate;
	
	@OneToMany(mappedBy="user")
	private List<Expense> expenses;
	
	@OneToMany(mappedBy="user")
	private List<Income> incomes;
	
	@OneToMany(mappedBy="user", cascade = CascadeType.ALL)
	private List<Category> categories;
	

	public List<Category> getCategories() {
		return categories;
	}
	
    public List<Expense> getExpenses() {
		return expenses;
	}

	public void setExpenses(List<Expense> expenses) {
		this.expenses = expenses;
	}

	public List<Income> getIncomes() {
		return incomes;
	}

	public void setIncomes(List<Income> incomes) {
		this.incomes = incomes;
	}

	public int getUserId() {
        return userId;
    }

    public void setUser_id(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setCategories(List<Category> categories) {
		this.categories = categories;
	}

    public String getPassword_hash() {
        return passwordHash;
    }

    public void setPassword_hash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getCreation_date() {
        return creationDate;
    }

    public void setCreation_date(Date creationDate) {
        this.creationDate = creationDate;
    }

    public User() {
    }

    public User(String username, String email, String passwordHash, Date creationDate) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.creationDate = creationDate;
    }
    
    public User(String username, String email, String passwordHash, Date creationDate, List<Expense> expenses, List<Income> incomes) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.creationDate = creationDate;
        this.expenses = expenses;
        this.incomes = incomes;
    }
}
