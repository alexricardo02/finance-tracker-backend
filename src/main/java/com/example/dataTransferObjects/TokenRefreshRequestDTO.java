package com.example.dataTransferObjects;
import jakarta.validation.constraints.NotBlank;

public class TokenRefreshRequestDTO {
    @NotBlank(message = "El Refresh Token es obligatorio")
    private String refreshToken;

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}