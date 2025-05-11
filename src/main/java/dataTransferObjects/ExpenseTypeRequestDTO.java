package dataTransferObjects;

import jakarta.validation.constraints.NotBlank;

public class ExpenseTypeRequestDTO {
	
	@NotBlank(message = "El tipo es obligatorio")
	private String typeName;

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public ExpenseTypeRequestDTO(@NotBlank(message = "El tipo es obligatorio") String typeName) {
		super();
		this.typeName = typeName;
	}

	public ExpenseTypeRequestDTO() {
		super();
	}

}
