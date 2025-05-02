package models;

import java.time.LocalDate;
import java.util.Date;

import jakarta.persistence.*;

@Entity
@Table(name="expenses")
public class Expense {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id", nullable = false)
    private int expenseID;
	
	@Column(name="amount", nullable = false)
    private double expenseAmount;
	
	@Column(name="date", nullable = false)
    private LocalDate expenseDate;
	
	@ManyToOne
    @JoinColumn(name = "subtype_id")
    private ExpenseSubtype  expenseSubtype;
    
    @ManyToOne
	@JoinColumn(name="user_id")
    private User user;

    public Expense() {
    }

    public Expense(int expenseID, double expenseAmount, LocalDate expenseDate, ExpenseType type, ExpenseSubtype expenseSubtype) {
        this.expenseID = expenseID;
        this.expenseAmount = expenseAmount;
        this.expenseDate = expenseDate;
        this.expenseSubtype = expenseSubtype;
    }

	public int getExpenseID() {
		return expenseID;
	}

	public void setExpenseID(int expenseID) {
		this.expenseID = expenseID;
	}

	public double getExpenseAmount() {
		return expenseAmount;
	}

	public void setExpenseAmount(double expenseAmount) {
		this.expenseAmount = expenseAmount;
	}

	public LocalDate getExpenseDate() {
		return expenseDate;
	}

	public void setExpenseDate(LocalDate expenseDate) {
		this.expenseDate = expenseDate;
	}

	public ExpenseSubtype getExpenseSubtype() {
		return expenseSubtype;
	}

	public void setExpenseSubtype(ExpenseSubtype expenseSubtype) {
		this.expenseSubtype = expenseSubtype;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
    
    


}
