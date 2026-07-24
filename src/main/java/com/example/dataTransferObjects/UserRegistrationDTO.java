 package com.example.dataTransferObjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UserRegistrationDTO {
	
	@NotBlank(message = "The username is required")
	private String username;
	
	@NotBlank(message = "The password is required")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "The password must contain at least 8 characters, one letter, and one number")
    private String password; // Plain text, not hashed
	
	@NotBlank(message = "The email is required")
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

	public UserRegistrationDTO(@NotBlank(message = "The username is required") String username,
			@NotBlank(message = "The password is required") @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "The password must contain at least 8 characters, one letter, and one number") String password,
			@NotBlank(message = "The email is required") String email) {
		super();
		this.username = username;
		this.password = password;
		this.email = email;
	}
	
	public UserRegistrationDTO() {}

   
}
