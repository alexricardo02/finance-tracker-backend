package dataTransferObjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class ExpenseSubtypeRequestDTO {
	
	@NotBlank(message = "El subtipo es obligatorio")
	private String subtypeName;
	
	@NotBlank(message = "Type ID is required")
    @Positive(message = "Type ID must be positive")
	private Integer typeId; // Maps to ExpenseType

	public String getSubtypeName() {
		return subtypeName;
	}

	public void setSubtypeName(String subtypeName) {
		this.subtypeName = subtypeName;
	}

	public Integer getTypeId() {
		return typeId;
	}

	public void setTypeId(Integer typeId) {
		this.typeId = typeId;
	}

	public ExpenseSubtypeRequestDTO(@NotBlank(message = "El subtipo es obligatorio") String subtypeName,
			@NotBlank(message = "Type ID is required") @Positive(message = "Type ID must be positive") Integer typeId) {
		super();
		this.subtypeName = subtypeName;
		this.typeId = typeId;
	}

	public ExpenseSubtypeRequestDTO() {
		super();
	}
	
	

}
