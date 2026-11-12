package com.example.dataTransferObjects;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UserUpdateDTO {
    @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$", message = "Username must be 3-50 characters: letters, digits, '.', '_' or '-'")
    private String username;
    
    @Email
    private String email;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public UserUpdateDTO(@Size(min = 3, max = 50) String username, @Email String email) {
		super();
		this.username = username;
		this.email = email;
	}

	public UserUpdateDTO() {
		super();
	}

}
