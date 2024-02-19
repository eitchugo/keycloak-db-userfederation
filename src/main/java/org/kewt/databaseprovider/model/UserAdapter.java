package org.kewt.databaseprovider.model;

import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.kewt.databaseprovider.DBFederationConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

public class UserAdapter extends AbstractUserAdapterFederatedStorage {
	
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(UserAdapter.class);
	
	private final DatabaseUser databaseUser;
	
    public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, DatabaseUser databaseUser) {
    	super(session, realm, model);
    	this.storageId = new StorageId(storageProviderModel.getId(), databaseUser.getUsername());
    	this.databaseUser = databaseUser;
    }
    
    @Override
	public String getUsername() {
    	return databaseUser.getUsername();
	}

	@Override
	public void setUsername(String username) {
		databaseUser.setUsername(username);
	}
	
	@Override
	public String getEmail() {
		return databaseUser.getEmail();
	}
	
	@Override
	public void setEmail(String email) {
		databaseUser.setEmail(email);
	}
	
	@Override
	public String getFirstName() {
		return databaseUser.getFirstName();
	}
	
	@Override
	public void setFirstName(String firstName) {
		databaseUser.setFirstName(firstName);
	}
	
	@Override
	public String getLastName() {
		return databaseUser.getLastName();
	}
	
	@Override
	public void setLastName(String lastName) {
		databaseUser.setLastName(lastName);
	}
	
	@Override
	public Map<String, List<String>> getAttributes() {
		MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
		attributes.add(UserModel.USERNAME, getUsername());
		attributes.add(UserModel.EMAIL, getEmail());
		attributes.add(UserModel.FIRST_NAME, getFirstName());
		attributes.add(UserModel.LAST_NAME, getLastName());
		attributes.add(DBFederationConstants.ATTRIBUTE_DATABASE_ID, String.valueOf(databaseUser.getId()));
		return attributes;
	}

	@Override
	public String toString() {
		return "UserAdapter[username=" + databaseUser.getUsername() + "]";
	}

}
