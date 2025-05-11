package dataTransferObjects;

public class IncomeTypeResponseDTO {

	private Integer typeId;
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
	public IncomeTypeResponseDTO(Integer typeId, String typeName) {
		super();
		this.typeId = typeId;
		this.typeName = typeName;
	}
	public IncomeTypeResponseDTO() {
		super();
	}
    
	
    
}
