package com.example.models;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import java.time.Instant;

@Entity
@Table(name = "incomes", indexes = { @Index(name = "idx_income_user", columnList = "user_id"),
		@Index(name = "idx_income_category", columnList = "category_id"),
		@Index(name = "idx_income_date", columnList = "date") })
@EntityListeners(AuditingEntityListener.class)
@org.hibernate.annotations.Where(clause = "deleted_at IS NULL")
public class Income {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "income_id", nullable = false)
	private Integer incomeId;

	@Column(name = "amount", nullable = false)
	private double amount;

	@Column(name = "currency", nullable = false)
	private String currency;

	@Column(name = "date", nullable = false)
	private LocalDate date;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "category_id", nullable = true)
	private Category category;

	@Column(name = "description")
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

	public Income(Integer incomeId, double amount, String currency, LocalDate date, User user, Category category,
			String description, PaymentMethod paymentMethod) {
		this.incomeId = incomeId;
		this.amount = amount;
		this.currency = currency;
		this.date = date;
		this.user = user;
		this.category = category;
		this.description = description;
		this.paymentMethod = paymentMethod;
	}

	public Income() {
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

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(PaymentMethod paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

}
