package models;

import jakarta.persistence.*;

@Entity
@Table(name="expensesubtypes")
public class ExpenseSubtype {
	
	@Id
	@Column(name="subtype_id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int subtypeId;
	
	@Column(name="subtype_name", nullable = true, unique = true)
    private String subtypeName;
	
	@ManyToOne
    @JoinColumn(name = "type_id")
    private ExpenseType type; // Clave foránea a ExpenseTypes

	public int getSubtypeId() {
		return subtypeId;
	}

	public void setSubtypeId(int subtypeId) {
		this.subtypeId = subtypeId;
	}

	public String getSubtypeName() {
		return subtypeName;
	}

	public void setSubtypeName(String subtypeName) {
		this.subtypeName = subtypeName;
	}

	public ExpenseType getType() {
		return type;
	}

	public void setType(ExpenseType type) {
		this.type = type;
	}

	public ExpenseSubtype(int subtypeId, String subtypeName, ExpenseType type) {
		super();
		this.subtypeId = subtypeId;
		this.subtypeName = subtypeName;
		this.type = type;
	}
	
	public ExpenseSubtype() {
	}
	

}
