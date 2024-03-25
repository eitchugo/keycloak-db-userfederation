package org.kewt.databaseprovider.model;

import java.util.Objects;

import org.keycloak.models.UserModel;

public class DatabaseUser {
	
	private Integer id;
	
	private String username;
	
	private String email;
	
	private String passwordHash;
	
	private String firstName;
	
	private String lastName;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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
	
	public String getPasswordHash() {
		return passwordHash;
	}
	
	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	@Override
	public String toString() {
		return "DatabaseUser[username="+ username + ",email=" + email + ",firstName=" + firstName + ",lastName=" + lastName + "]";
	}
	
	public boolean outOfSync(UserModel user) {
		return
			!(Objects.equals(username, user.getUsername())) ||
			!(Objects.equals(email, user.getEmail())) ||
			!(Objects.equals(firstName, user.getFirstName())) ||
			!(Objects.equals(lastName, user.getLastName()));
	}
	
	public void syncToUserModel(UserModel user) {
		user.setUsername(this.username);
		user.setEmail(this.email);
		user.setFirstName(this.firstName);
		user.setLastName(this.lastName);
	}

}
