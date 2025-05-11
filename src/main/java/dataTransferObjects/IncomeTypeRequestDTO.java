package dataTransferObjects;

import jakarta.validation.constraints.NotBlank;

public class IncomeTypeRequestDTO {

	@NotBlank(message = "Nombre es obligatorio")
	private String typeName;

	public IncomeTypeRequestDTO(@NotBlank(message = "Nombre es obligatorio") String typeName) {
		super();
		this.typeName = typeName;
	}

	public IncomeTypeRequestDTO() {
		super();
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
	
	
	
}
