package org.kewt.databaseprovider.model;

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

public class WritableUserDelegate extends UserModelDelegate {

	protected static final Logger LOGGER = Logger.getLogger(WritableUserDelegate.class);
	
	protected DatabaseUser databaseUser;
	
	protected boolean dirty;
	
	public WritableUserDelegate(UserModel delegate, DatabaseUser databaseUser) {
		super(delegate);
		this.databaseUser = databaseUser;
	}
	
	@Override
	public void setUsername(String username) {
		LOGGER.infov("  setUsername: {0}", username);
		super.setUsername(username);
		databaseUser.setUsername(username);
		dirty = true;
	}
	
	@Override
	public void setEmail(String email) {
		LOGGER.infov("  setEmail: {0}", email);
		super.setEmail(email);
		databaseUser.setEmail(email);
		dirty = true;
	}
	
	@Override
	public void setFirstName(String firstName) {
		LOGGER.infov("  setFirstName: {0}", firstName);
		super.setFirstName(firstName);
		databaseUser.setFirstName(firstName);
		dirty = true;
	}
	
	@Override
	public void setLastName(String lastName) {
		LOGGER.infov("  setLastName: {0}", lastName);
		super.setLastName(lastName);
		databaseUser.setLastName(lastName);
		dirty = true;
	}
	
	@Override
	public void setAttribute(String name, List<String> values) {
		LOGGER.infov("  setAttribute: {0}, {1}", name, values);
		switch (name) {
			case "email":
				setEmail(values.get(0));
				break;
			case "firstName":
				setFirstName(values.get(0));
				break;
			case "lastName":
				setLastName(values.get(0));
				break;
			default:
				super.setAttribute(name, values);
				break;
		}
	}
	
	@Override
	public void setSingleAttribute(String name, String value) {
		LOGGER.infov("  setSingleAttribute: {0}, {1}", name, value);
		switch (name) {
			case "email":
				setEmail(value);
				break;
			case "firstName":
				setFirstName(value);
				break;
			case "lastName":
				setLastName(value);
				break;
			default:
				super.setSingleAttribute(name, value);
				break;
		}
	}
	
	public boolean isDirty() {
		return dirty;
	}
	
	public DatabaseUser getDatabaseUser() {
		return databaseUser;
	}
	
	@Override
	public String toString() {
		return "DatabaseUserDelegate[username" + getUsername() + ",email=" + getEmail() + ",firstName=" + getFirstName() + ",lastName=" + getLastName() + "]";
	}

}
