package com.example.dataTransferObjects;

public class LoginResponseDTO {
	
	private String token;
	private String refreshToken;
    private UserProfileDTO profile;

    public LoginResponseDTO(String token, String refreshToken, UserProfileDTO profile) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.profile = profile;
    }

    // Getters y Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public UserProfileDTO getProfile() { return profile; }
    public void setProfile(UserProfileDTO profile) { this.profile = profile; }
    
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

}
