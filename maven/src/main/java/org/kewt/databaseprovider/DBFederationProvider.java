package org.kewt.databaseprovider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.kewt.databaseprovider.crypto.PasswordHashFunction;
import org.kewt.databaseprovider.database.DatabaseConnection;
import org.kewt.databaseprovider.model.DatabaseUser;
import org.kewt.databaseprovider.model.ReadOnlyUserDelegate;
import org.kewt.databaseprovider.model.WritableUserDelegate;
import org.kewt.databaseprovider.repository.DatabaseUserRepository;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserCountMethodsProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

//AbstractUserAdapterFederatedStorage

public class DBFederationProvider
	implements 
		UserStorageProvider,
		UserLookupProvider,
		UserRegistrationProvider,
		UserCountMethodsProvider,
		UserQueryProvider,
		ImportedUserValidation,
		CredentialInputValidator,
		CredentialInputUpdater {
	
	protected static final Logger LOGGER = Logger.getLogger(DBFederationProvider.class);
	
	protected KeycloakSession session;
	
	protected ComponentModel model;
	
	protected DatabaseConnection connection;
	
	protected DatabaseUserRepository userRepository;
	
	protected Collection<WritableUserDelegate> delegates;
	
	public DBFederationProvider(KeycloakSession session, ComponentModel model, DatabaseConnection connection) {
        this.session = session;
        this.model = model;
        this.connection = connection;
        this.userRepository = new DatabaseUserRepository(connection, model);
        this.delegates = new ArrayList<>();
    }
	
	// UserStorageProvider

	@Override
	public void close() {
		LOGGER.debugv("close:");
		String syncMode = model.get(DBFederationConstants.CONFIG_SYNC_MODE);
 		if (DBFederationConstants.SYNC_READWRITE.equals(syncMode) ||
 			DBFederationConstants.SYNC_READWRITEDELETE.equals(syncMode)) {
 			for (WritableUserDelegate delegate : delegates) {
 				if (delegate.isDirty()) {
 					LOGGER.debugv("  updating {0}", delegate.getUsername());
 					userRepository.update(delegate.getDatabaseUser());
 				}
 			}
 		}
		delegates.clear();
		connection.close();
	}
	
	// UserLookupProvider

	@Override
	public UserModel getUserById(RealmModel realm, String id) {
		LOGGER.debugv("getUserById: {0}", id);
		Integer databaseId = Integer.valueOf(id);
		DatabaseUser databaseUser = userRepository.getUserById(databaseId);
    	if (databaseUser != null) {
    		return createAdapter(realm, databaseUser);
    	}
        return null;
	}

	@Override
	public UserModel getUserByUsername(RealmModel realm, String username) {
		LOGGER.debugv("getUserByUsername: {0}", username);
    	DatabaseUser databaseUser = userRepository.getUserByUsername(username);
    	if (databaseUser != null) {
    		return createAdapter(realm, databaseUser);
    	}
        return null;
	}

	@Override
	public UserModel getUserByEmail(RealmModel realm, String email) {
		LOGGER.debugv("getUserByEmail: {0}", email);
		DatabaseUser databaseUser = userRepository.getUserByEmail(email);
    	if (databaseUser != null) {
    		return createAdapter(realm, databaseUser);
    	}
        return null;
	}
    
	// UserRegistrationProvider Methods

 	@Override
 	public UserModel addUser(RealmModel realm, String username) {
 		LOGGER.debugv("addUser: {0}", username);
 		
 		if (isSyncWriteMode()) {
 			DatabaseUser databaseUser = new DatabaseUser();
 	 		databaseUser.setUsername(username);
 	 		databaseUser.setEmail("");
 	 		databaseUser.setFirstName("");
 	 		databaseUser.setLastName("");
 	 		databaseUser.setPasswordHash("");
 	 		userRepository.insert(databaseUser);
 	 		return createAdapter(realm, databaseUser);
 		} else {
 			return null;
 		}
 	}

 	@Override
 	public boolean removeUser(RealmModel realm, UserModel user) {
 		LOGGER.debugv("removeUser: {0}", user);
 		if (isSyncDeleteMode()) {
 			Integer databaseId = getDatabaseId(user);
 			if (databaseId != null) {
 				DatabaseUser databaseUser = userRepository.getUserById(databaseId);
 				if (databaseUser != null) {
 					return userRepository.delete(databaseUser);
 				}
 			}
 		}
 		return true;
 	}
 	
 	// UserQueryProvider Methods
 	
 	@Override
 	public int getUsersCount(RealmModel realm) {
 		LOGGER.debugv("getUsersCount:");
 		return 0;
 	}
 	
    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
    	LOGGER.debugv("searchForUserStream: {0}", search);
    	return Stream.empty();
	}
    
	@Override
	public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
		LOGGER.debugv("searchForUserStream: {0}", params);
		return Stream.empty();
	}

	@Override
	public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
		LOGGER.debugv("getGroupMembersStream: {0}", group);
		return Stream.empty();
	}

	@Override
	public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
		LOGGER.debugv("searchForUserByUserAttributeStream: {0}", attrName);
		return Stream.empty();
	}
	
	// ImportedUserValidation
	
	@Override
	public UserModel validate(RealmModel realm, UserModel user) {
		LOGGER.debugv("validate: {0}", user.getUsername());
		Integer databaseId = getDatabaseId(user);
		
		if (databaseId != null) {
			DatabaseUser databaseUser = userRepository.getUserById(databaseId);
			if (databaseUser == null) {
				return null;
			}
			if (databaseUser.outOfSync(user)) {
				LOGGER.debugv("syncing local model: {0}", user.getUsername());
				databaseUser.syncToUserModel(user);
			}
			
			String syncMode = model.get(DBFederationConstants.CONFIG_SYNC_MODE);
			if (DBFederationConstants.SYNC_READWRITE.equals(syncMode) ||
				DBFederationConstants.SYNC_READWRITEDELETE.equals(syncMode)) {
				return createWritableDelegate(user, databaseUser);
			} else {
				return createReadOnlyDelegate(user, databaseUser);
			}
		}
		return user;
	}
    
    // CredentialInputValidator Methods

	@Override
	public boolean supportsCredentialType(String credentialType) {
		return credentialType.equals(PasswordCredentialModel.TYPE);
	}

	@Override
	public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
		LOGGER.debugv("isConfiguredFor: {0}", user);
		if (!credentialType.equals(PasswordCredentialModel.TYPE)) {
			return false;
		}
		Integer databaseId = getDatabaseId(user);
		if (databaseId == null) {
			return false;
		}
		DatabaseUser databaseUser = userRepository.getUserById(databaseId);
		if (databaseUser == null || databaseUser.getPasswordHash() == null || databaseUser.getPasswordHash().equals("")) {
			return false;
		}
		return true;
	}

	@Override
	public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
		LOGGER.debugv("isValid: {0}, {1}", user, input);
		if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
			return false;
		}
		Integer databaseId = getDatabaseId(user);
		if (databaseId == null) {
			return false;
		}
		DatabaseUser databaseUser = userRepository.getUserById(databaseId);
		if (databaseUser == null || databaseUser.getPasswordHash() == null || databaseUser.getPasswordHash().equals("")) {
			return false;
		}
		
		UserCredentialModel credential = (UserCredentialModel) input;
		PasswordHashFunction hash = PasswordHashFunction.getById(model.get(DBFederationConstants.CONFIG_PASSWORD_HASH_FUNCTION));
		return hash.verify(credential.getValue(), databaseUser.getPasswordHash(), model);
	}
	
	// CredentialInputUpdater Methods
	
	@Override
	public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
		LOGGER.debugv("updateCredential: {0}", user.getUsername());
		
 		if (isSyncWriteMode()) {
 			if (!(input instanceof UserCredentialModel)) {
 				return false;
 			}
 	        if (!input.getType().equals(PasswordCredentialModel.TYPE)) {
 	        	return false;
 	        }
 	        Integer databaseId = getDatabaseId(user);
 			if (databaseId == null) {
 				return false;
 			}
 	        UserCredentialModel credential = (UserCredentialModel) input;
 	        // String salt = model.get(DBFederationConstants.CONFIG_SALT);
 	        PasswordHashFunction hash = PasswordHashFunction.getById(model.get(DBFederationConstants.CONFIG_PASSWORD_HASH_FUNCTION));
 			String hashedPassword = hash.digest(credential.getValue(), model);
 	        DatabaseUser databaseUser = userRepository.getUserById(databaseId);
 	        if (databaseUser == null) {
 	        	return false;
 	        }
 	        return userRepository.updatePassword(databaseUser.getId(), hashedPassword);
 		} else {
 			throw new IllegalArgumentException("Cannot change password in READONLY mode");
 		}
	}

	@Override
	public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
		LOGGER.debugv("disableCredential: {0}", user.getUsername());
		
 		if (isSyncWriteMode()) {
			if (!credentialType.equals(PasswordCredentialModel.TYPE)) {
				return;
			}
			Integer databaseId = getDatabaseId(user);
			if (databaseId == null) {
				return;
			}
	        DatabaseUser databaseUser = userRepository.getUserById(databaseId);
	        if (databaseUser == null) {
	        	return;
	        }
	        userRepository.updatePassword(databaseUser.getId(), "");
 		} else {
 			throw new IllegalArgumentException("Cannot reset password in READONLY mode");
 		}
	}

	@Override
	public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
		return Arrays.asList(PasswordCredentialModel.TYPE).stream();
	}
	
	// Private Methods
	
    protected UserModel createAdapter(RealmModel realm, DatabaseUser databaseUser) {
    	LOGGER.debugv("createAdapter: {0}", databaseUser);
    	UserModel local = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, databaseUser.getUsername());
    	if (local == null) {
    		local = UserStoragePrivateUtil.userLocalStorage(session).addUser(realm, databaseUser.getUsername());
    		local.setFederationLink(model.getId());
    		local.setEmail(databaseUser.getEmail());
    		local.setFirstName(databaseUser.getFirstName());
    		local.setLastName(databaseUser.getLastName());
    		local.setSingleAttribute(DBFederationConstants.ATTRIBUTE_DATABASE_ID, databaseUser.getId().toString());
    		local.setEnabled(true);
    		local.setEmailVerified(true);
    	}
    	return createWritableDelegate(local, databaseUser);
    }
    
    protected WritableUserDelegate createWritableDelegate(UserModel local, DatabaseUser databaseUser) {
    	LOGGER.debugv("createWritableDelegate: {0} {1}", local, databaseUser);
    	WritableUserDelegate delegate = new WritableUserDelegate(local, databaseUser);
    	delegates.add(delegate);
    	return delegate;
    }
    
    protected ReadOnlyUserDelegate createReadOnlyDelegate(UserModel local, DatabaseUser databaseUser) {
    	LOGGER.debugv("createReadOnlyDelegate: {0} {1}", local, databaseUser);
    	ReadOnlyUserDelegate delegate = new ReadOnlyUserDelegate(local);
    	return delegate;
    }
    
    protected Integer getDatabaseId(UserModel user) {
    	String databaseId = user.getFirstAttribute(DBFederationConstants.ATTRIBUTE_DATABASE_ID);
    	try {
    		return databaseId != null ? Integer.valueOf(databaseId) : null; 
    	} catch (NumberFormatException e) {
    		return null;
    	}
    }

    protected boolean isSyncWriteMode() {
    	String syncMode = model.get(DBFederationConstants.CONFIG_SYNC_MODE);
    	return DBFederationConstants.SYNC_READWRITE.equals(syncMode) || DBFederationConstants.SYNC_READWRITEDELETE.equals(syncMode);
    }
    
    protected boolean isSyncDeleteMode() {
    	String syncMode = model.get(DBFederationConstants.CONFIG_SYNC_MODE);
 		return DBFederationConstants.SYNC_READWRITEDELETE.equals(syncMode);
    }
}