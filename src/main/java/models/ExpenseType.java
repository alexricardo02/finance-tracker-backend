package models;

import jakarta.persistence.*;

@Entity
@Table(name="expensetypes")
public class ExpenseType {
	
	@Id
	@Column(name="type_id", nullable = false)
	private int typeId;
    
	@Column(name="type_name", nullable = true)
	private String typeName;

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
