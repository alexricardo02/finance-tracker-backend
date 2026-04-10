package com.example.dataTransferObjects;

public class LoginResponseDTO {
	
	private String token;
    private UserProfileDTO profile;

    public LoginResponseDTO(String token, UserProfileDTO profile) {
        this.token = token;
        this.profile = profile;
    }

    // Getters y Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public UserProfileDTO getProfile() { return profile; }
    public void setProfile(UserProfileDTO profile) { this.profile = profile; }

}
