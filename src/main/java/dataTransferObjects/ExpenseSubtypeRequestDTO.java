package dataTransferObjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class ExpenseSubtypeRequestDTO {
	
	@NotBlank(message = "El subtipo es obligatorio")
	private String subtypeName;
	
	@NotBlank(message = "Type ID is required")
    @Positive(message = "Type ID must be positive")
	private Integer typeId; // Maps to ExpenseType

}
