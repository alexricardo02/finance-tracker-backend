package models;

import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Table(name="incomes")
public class Income {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="income_id", nullable = false)
	private int incomeId;
	
	@Column(name="amount", nullable = false)
    private double incomeAmount;
	
	@Column(name="date", nullable = false)
    private LocalDate  incomeDate;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "type_id", nullable = false)
    private IncomeType incomeType;
	
	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="user_id", nullable = false)
	private User user;
	
	@Column(name="description")
    private String incomeDescription;

    public Income(double incomeAmount, LocalDate incomeDate, User user, IncomeType incomeType, String incomeDescription) {
        this.incomeAmount = incomeAmount;
        this.incomeDate = incomeDate;
        this.user = user;
        this.incomeType = incomeType;
        this.incomeDescription = incomeDescription;
    }

    public Income() {
    }

	public int getIncomeId() {
		return incomeId;
	}

	public void setIncomeId(int incomeId) {
		this.incomeId = incomeId;
	}

	public double getIncomeAmount() {
		return incomeAmount;
	}

	public void setIncomeAmount(double incomeAmount) {
		this.incomeAmount = incomeAmount;
	}

	public LocalDate getIncomeDate() {
		return incomeDate;
	}

	public void setIncomeDate(LocalDate incomeDate) {
		this.incomeDate = incomeDate;
	}

	public IncomeType getIncomeType() {
		return incomeType;
	}

	public void setIncomeType(IncomeType incomeType) {
		this.incomeType = incomeType;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getIncomeDescription() {
		return incomeDescription;
	}

	public void setIncomeDescription(String incomeDescription) {
		this.incomeDescription = incomeDescription;
	}


}
