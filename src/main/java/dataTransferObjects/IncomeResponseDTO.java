package dataTransferObjects;

import java.time.LocalDate;


public class IncomeResponseDTO {
	
	private Integer incomeId;
	private Double amount;
    private LocalDate incomeDate;
    private String incomeTypeName;
    private IncomeUserDTO user;
    private String incomeDescription;
    
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

	public IncomeUserDTO getUser() {
		return user;
	}

	public void setUser(IncomeUserDTO user) {
		this.user = user;
	}

	public String getIncomeDescription() {
		return incomeDescription;
	}

	public void setIncomeDescription(String incomeDescription) {
		this.incomeDescription = incomeDescription;
	}
    
    

}
