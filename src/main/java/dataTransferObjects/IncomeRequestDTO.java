package dataTransferObjects;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;



public class IncomeRequestDTO {

	@Positive(message = "El income tiene que ser positivo")
    private Double amount;

	@NotNull(message = "La fecha es obligatoria")
    private LocalDate incomeDate;
    
	@NotBlank(message = "El tipo es obligatorio")
    private String incomeTypeName;
    
	@NotNull(message = "El usuario debe existir")
    private Integer userId;
    
    private String incomeDescription;

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public LocalDate getIncomeDate() {
		return incomeDate;
	}

	public void setIncomeDate(LocalDate incomeDate) {
		this.incomeDate = incomeDate;
	}

	public String getIncomeTypeName() {
		return incomeTypeName;
	}

	public void setIncomeTypeName(String incomeTypeName) {
		this.incomeTypeName = incomeTypeName;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public String getIncomeDescription() {
		return incomeDescription;
	}

	public void setIncomeDescription(String incomeDescription) {
		this.incomeDescription = incomeDescription;
	}
    
	
}
