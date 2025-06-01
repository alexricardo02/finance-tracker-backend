package dataTransferObjects;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class ExpenseRequestDTO {
	
	@Positive(message = "El monto debe ser positivo")
	private Double amount;
	
	@NotBlank(message = "La fecha es obligatoria")
    private LocalDate date;

    @NotBlank(message = "El tipo es obligatorio")
    private String typeName;  // Added field
    
	@NotBlank(message = "El subtipo es obligatorio")
    private String subtypeName;
	
	@NotBlank(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Integer userId; // Avoid exposing User entity
	
	
    
	public ExpenseRequestDTO() {
		super();
	}
	public ExpenseRequestDTO(@Positive(message = "El monto debe ser positivo") Double amount,
			@NotBlank(message = "La fecha es obligatoria") LocalDate date,
			@NotBlank(message = "El tipo es obligatorio") String typeName,
			@NotBlank(message = "El subtipo es obligatorio") String subtypeName,
			@NotBlank(message = "User ID is required") @Positive(message = "User ID must be positive") Integer userId) {
		super();
		this.amount = amount;
		this.date = date;
		this.typeName = typeName;
		this.subtypeName = subtypeName;
		this.userId = userId;
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
	public String getSubtypeName() {
		return subtypeName;
	}
	public void setSubtypeName(String subtypeName) {
		this.subtypeName = subtypeName;
	}
}
