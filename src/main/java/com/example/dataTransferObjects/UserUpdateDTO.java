package com.example.dataTransferObjects;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UserUpdateDTO {
    @Size(min=3, max=50)
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
