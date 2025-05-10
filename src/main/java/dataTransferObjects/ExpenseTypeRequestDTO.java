package dataTransferObjects;

import jakarta.validation.constraints.NotBlank;

public class ExpenseTypeRequestDTO {
	
	@NotBlank(message = "El tipo es obligatorio")
	private String typeName;
	
	

}
