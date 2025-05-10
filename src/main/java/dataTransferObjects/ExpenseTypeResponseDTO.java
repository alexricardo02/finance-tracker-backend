package dataTransferObjects;

import java.util.List;

public class ExpenseTypeResponseDTO {
	
	private Integer typeId;
    private String typeName;
    private List<ExpenseSubtypeResponseDTO> subtypes;
    
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
	public List<ExpenseSubtypeResponseDTO> getSubtypes() {
		return subtypes;
	}
	public void setSubtypes(List<ExpenseSubtypeResponseDTO> subtypes) {
		this.subtypes = subtypes;
	}

    
    
}
