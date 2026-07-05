package com.example.models;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;

@Entity
@Table(name = "expenses", indexes = { @Index(name = "idx_expense_user", columnList = "user_id"),
		@Index(name = "idx_expense_category", columnList = "category_id"),
		@Index(name = "idx_expense_date", columnList = "date") })
@EntityListeners(AuditingEntityListener.class)
@org.hibernate.annotations.Where(clause = "deleted_at IS NULL")
public class Expense {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Integer id;

	@Column(name = "amount", nullable = false)
	private double amount;

	@Column(name = "currency", nullable = false)
	private String currency;

	@Column(name = "date", nullable = false)
	private LocalDate date;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "category_id", nullable = true)
	private Category category;

	@Column(name = "expense_description", columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method")
	private PaymentMethod paymentMethod;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(name = "updated_at")
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Column(name = "deleted_by")
	private String deletedBy;

	public Expense() {
	}

	public Expense(int id, double amount, String currency, LocalDate date, Category category, String type,
			String description, PaymentMethod paymentMethod) {
		this.id = id;
		this.amount = amount;
		this.currency = currency;
		this.date = date;
		this.category = category;
		this.description = description;
		this.paymentMethod = paymentMethod;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}

	public String getDeletedBy() {
		return deletedBy;
	}

	public void setDeletedBy(String deletedBy) {
		this.deletedBy = deletedBy;
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

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
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

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(PaymentMethod paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

}
