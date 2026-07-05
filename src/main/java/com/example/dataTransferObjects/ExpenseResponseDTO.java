package com.example.dataTransferObjects;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public class ExpenseResponseDTO {

	private Integer id;
	private Double amount;
	private String currency;
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate date;
	private Integer categoryId;
	private String categoryName;
	private String description;
	private Integer userId;

	public ExpenseResponseDTO() {
		super();
	}
	
	public ExpenseResponseDTO(int id, double amount, String currency, LocalDate date, Integer categoryId, String categoryName, String description) {
        this.id = id;
        this.amount = amount;
        this.currency = currency;
        this.date = date;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.description = description;
    }

	public ExpenseResponseDTO(Integer id, Double amount, String currency, LocalDate date, String categoryName,
			Integer categoryId, String description, Integer userId) {
		super();
		this.id = id;
		this.amount = amount;
		this.currency = currency;
		this.date = date;
		this.categoryId = categoryId;
		this.categoryName = categoryName;
		this.description = description;
		this.userId = userId;
	}

	public Integer getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Integer categoryId) {
		this.categoryId = categoryId;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

}
