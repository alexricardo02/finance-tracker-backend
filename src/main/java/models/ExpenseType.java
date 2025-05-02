package models;

import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name="expensetypes")
public class ExpenseType {
	
	@Id
	@Column(name="type_id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer typeId;
    
	@Column(name="type_name", nullable = true, unique = true)
	private String typeName;
	
	// EN DUDA
	@OneToMany(mappedBy = "expenseType", cascade = CascadeType.ALL)
    private List<ExpenseSubtype> subtypes;

	public int getTypeId() {
		return typeId;
	}

	public void setTypeId(int typeId) {
		this.typeId = typeId;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public ExpenseType(int typeId, String typeName) {
		super();
		this.typeId = typeId;
		this.typeName = typeName;
	}
	
	public ExpenseType() {
	}

}
