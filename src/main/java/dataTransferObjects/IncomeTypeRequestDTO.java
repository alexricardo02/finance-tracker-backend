package dataTransferObjects;

import jakarta.validation.constraints.NotBlank;

public class IncomeTypeRequestDTO {

	@NotBlank(message = "Nombre es obligatorio")
	private String typeName;
}
