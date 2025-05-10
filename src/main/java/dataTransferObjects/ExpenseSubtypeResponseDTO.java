package dataTransferObjects;

public class ExpenseSubtypeResponseDTO {
	
	private Integer subtypeId;
    private String subtypeName;
    private String typeName;
    
	public Integer getSubtypeId() {
		return subtypeId;
	}
	public void setSubtypeId(Integer subtypeId) {
		this.subtypeId = subtypeId;
	}
	public String getSubtypeName() {
		return subtypeName;
	}
	public void setSubtypeName(String subtypeName) {
		this.subtypeName = subtypeName;
	}
	public String getTypeName() {
		return typeName;
	}
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
    
    

}
