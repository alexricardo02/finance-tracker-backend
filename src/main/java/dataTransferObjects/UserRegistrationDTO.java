 package dataTransferObjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UserRegistrationDTO {
	
	@NotBlank(message = "El nombre de usuario es obligatorio")
	private String username;
	
	@NotBlank(message = "La contraseña es obligatoria")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "La contraseña debe tener al menos 8 caracteres, una letra y un número")
    private String password; // Texto plano, no hash
	
	@NotBlank(message = "El email es obligatorio")
    private String email;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

    

}
