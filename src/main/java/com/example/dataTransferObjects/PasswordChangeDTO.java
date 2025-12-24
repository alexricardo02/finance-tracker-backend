package com.example.dataTransferObjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordChangeDTO {
    @NotBlank @Size(min=8)
    private String currentPassword;
    
    @NotBlank @Size(min=8)
    private String newPassword;

	public String getCurrentPassword() {
		return currentPassword;
	}

	public void setCurrentPassword(String currentPassword) {
		this.currentPassword = currentPassword;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	public PasswordChangeDTO(@NotBlank @Size(min = 8) String currentPassword,
			@NotBlank @Size(min = 8) String newPassword) {
		super();
		this.currentPassword = currentPassword;
		this.newPassword = newPassword;
	}

	public PasswordChangeDTO() {
		super();
	}
    
    
    
}
