package com.example.dataTransferObjects;

import java.util.Date;

public class UserProfileDTO {

	private Integer userId;
    private String username;
    private String email;
    private Date creationDate;

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

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

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public UserProfileDTO(Integer userId, String username, String email, Date creationDate) {
		super();
		this.userId = userId;
		this.username = username;
		this.email = email;
		this.creationDate = creationDate;
	}

	public UserProfileDTO() {
		super();
	}

    

}
