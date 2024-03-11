package org.kewt.databaseprovider.model;

import java.util.List;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

public class ReadOnlyUserDelegate extends UserModelDelegate {

	public ReadOnlyUserDelegate(UserModel delegate) {
		super(delegate);
	}
	
	@Override
	public void setUsername(String username) {
		raiseException();
	}
	
	@Override
	public void setEmail(String email) {
		raiseException();
	}
	
	@Override
	public void setFirstName(String firstName) {
		raiseException();
	}
	
	@Override
	public void setLastName(String lastName) {
		raiseException();
	}
	
	@Override
	public void setAttribute(String name, List<String> values) {
		raiseException();
	}
	
	@Override
	public void setSingleAttribute(String name, String value) {
		raiseException();
	}
	
	private void raiseException() {
		throw new IllegalArgumentException("Cannot modify user in read only mode");
	}

}
