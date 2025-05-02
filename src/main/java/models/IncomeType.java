package models;

import jakarta.persistence.*;

@Entity
@Table(name="incometypes")
public class IncomeType {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="type_id", nullable = false)
	private Integer typeId;
	
	@Column(name="type_name", nullable = false)
    private String typeName;

	public Integer getTypeId() {
		return typeId;
	}

	public void setTypeId(Integer typeId) {
		this.typeId = typeId;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public IncomeType(Integer typeId, String typeName) {
		super();
		this.typeId = typeId;
		this.typeName = typeName;
	}
	
	public IncomeType() {
	}


}
