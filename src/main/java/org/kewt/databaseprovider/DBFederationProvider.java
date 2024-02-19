package org.kewt.databaseprovider;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.kewt.databaseprovider.database.DatabaseConnection;
import org.kewt.databaseprovider.model.DatabaseUser;
import org.kewt.databaseprovider.model.UserAdapter;
import org.kewt.databaseprovider.repository.DatabaseUserRepository;
import org.kewt.databaseprovider.utils.PasswordUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;

public class DBFederationProvider
	implements 
		UserStorageProvider,
        UserLookupProvider,
        UserRegistrationProvider,
        UserQueryProvider,
        UserProfileProvider,
        CredentialInputValidator,
        CredentialInputUpdater,
        OnUserCache,
        EventListenerProvider,
        ImportedUserValidation {
	
	protected static final Logger LOGGER = Logger.getLogger(DBFederationProvider.class);
	
	protected static final String PASSWORD_CACHE_KEY = UserAdapter.class.getName() + ".password";
	
	protected static final boolean IMPORT_USER = true;
	
	protected KeycloakSession session;
	
	protected ComponentModel model;
	
	protected DatabaseConnection connection;
	
	protected DatabaseUserRepository userRepository;
	
	public DBFederationProvider(KeycloakSession session, ComponentModel model, DatabaseConnection connection) {
        this.session = session;
        this.model = model;
        this.connection = connection;
        this.userRepository = new DatabaseUserRepository(connection, model);
    }
	
	// UserStorageProvider

	@Override
	public void close() {
		LOGGER.infov("Closing DatabaseUserStorageProvider");
		connection.close();
	}
	
	// ImportedUserValidation
	
	@Override
	public UserModel validate(RealmModel realm, UserModel user) {
		LOGGER.infov("validate({0})", user.getUsername());
		// TODO: change to use UserDelegate?
		
		if (user.getFirstAttribute(DBFederationConstants.ATTRIBUTE_DATABASE_ID) == null) {
			LOGGER.infov("  db user not exists");
			DatabaseUser databaseUser = new DatabaseUser();
			databaseUser.setUsername(user.getUsername());
			databaseUser.setFirstName(user.getFirstName());
			databaseUser.setLastName(user.getLastName());
			databaseUser.setEmail(user.getEmail());
			databaseUser.setPasswordHash("");
			Integer databaseId = userRepository.insert(databaseUser);
			if (databaseId != null) {
				user.setSingleAttribute(DBFederationConstants.ATTRIBUTE_DATABASE_ID, databaseId.toString());
			}
		} else {
			LOGGER.infov("  db user exists");
			Integer databaseId = Integer.valueOf(user.getFirstAttribute(DBFederationConstants.ATTRIBUTE_DATABASE_ID));
			DatabaseUser databaseUser = userRepository.getUserById(databaseId);
			if (databaseUser != null) {
				if (databaseUser.outOfSync(user)) {
					LOGGER.infov("  db user out of sync");
					databaseUser.syncAttributes(user);
					userRepository.update(databaseUser);
				}
			}
		}
		return user;
	}
	
	// OnUserCache Methods
	
	@Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
//        String password = user.getFirstAttribute("PASSWORD_HASH");
//        if (password != null) {
//            user.getCachedWith().put(PASSWORD_CACHE_KEY, password);
//        }
    }
	
	// UserQueryProvider Methods
	
	@Override
    public int getUsersCount(RealmModel realm) {
		return userRepository.countUsers();
    }
	
	@Override
	public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
		return userRepository
			.searchUsers(search, firstResult, maxResults).stream()
			.map((DatabaseUser databaseUser) -> {
				return (UserModel) new UserAdapter(session, realm, model, databaseUser);
			});
    }
	
	@Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
		return Stream.empty();
    }
	
	@Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return Stream.empty();
    }
	
	// UserRegistrationProvider Methods
	
	@Override
	public UserModel addUser(RealmModel realm, String username) {
		LOGGER.infov("addUser({0})", username);
		
		UserModel local = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, username);
		if (local != null) {
			LOGGER.infov("  already exists");
			return local;
		}
		else {
			LOGGER.infov("  not exists");
			DatabaseUser databaseUser = new DatabaseUser();
			databaseUser.setUsername(username);
			databaseUser.setEmail("");
			databaseUser.setFirstName("");
			databaseUser.setLastName("");
			databaseUser.setPasswordHash("");
			userRepository.insert(databaseUser);
			return createAdapter(realm, databaseUser);
		}
	}
	
	@Override
	public boolean removeUser(RealmModel realm, UserModel user) {
		LOGGER.infov("removeUser({0})", user.getUsername());
		return true;
//		try (Connection connection = JDBCDatabase.getConnection(model)) {
//			PreparedStatement statement = connection.prepareStatement("delete from users where username = ?");
//			statement.setString(1, user.getUsername());
//			int rows = statement.executeUpdate();
//			return rows > 0;
//		} catch (SQLException e) {
//			throw new RuntimeException(e);
//		}
	}

	// CredentialInputUpdater Methods
	
	@Override
	public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
		LOGGER.infov("updateCredential({0})", user.getUsername());
		
		if (!(input instanceof UserCredentialModel)) return false;
        if (!input.getType().equals(PasswordCredentialModel.TYPE)) return false;
        UserCredentialModel credential = (UserCredentialModel) input;
        
        String salt = model.get(DBFederationConstants.CONFIG_SALT);
        String hashedPassword = PasswordUtils.sha512(salt + credential.getValue());
        return userRepository.updateCredential(user.getUsername(), hashedPassword);
	}

	@Override
	public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
		LOGGER.infov("disableCredential({0})", user.getUsername());
		if (!credentialType.equals(PasswordCredentialModel.TYPE)) return;
		
		userRepository.updateCredential(user.getUsername(), "");
	}

	@Override
	public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
		return Arrays.asList(PasswordCredentialModel.TYPE).stream();
	}

	// CredentialInputValidator Methods
	
	@Override
	public boolean supportsCredentialType(String credentialType) {
		return credentialType.equals(PasswordCredentialModel.TYPE);
	}

	@Override
	public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
		LOGGER.infov("isConfiguredFor({0}, {1})", user.getUsername(), credentialType);
		if (!supportsCredentialType(credentialType)) {
			return false;
		}
		if (user.getFirstAttribute(DBFederationConstants.ATTRIBUTE_DATABASE_ID) == null) {
			return false;
		}
		
		Integer databaseId = Integer.valueOf(user.getFirstAttribute(DBFederationConstants.ATTRIBUTE_DATABASE_ID));
		DatabaseUser databaseUser = userRepository.getUserById(databaseId);
		
		if (databaseUser == null) {
			return false;
		}
		if (databaseUser.getPasswordHash() == null || databaseUser.getPasswordHash().equals("")) {
			return false;
		}
		return true;
	}

	@Override
	public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
		LOGGER.infov("isValid({0}, ...)", user.getUsername());
		if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
			return false;
		}
		if (user.getFirstAttribute(DBFederationConstants.ATTRIBUTE_DATABASE_ID) == null) {
			return false;
		}
		Integer databaseId = Integer.valueOf(user.getFirstAttribute(DBFederationConstants.ATTRIBUTE_DATABASE_ID));
		LOGGER.infov("  databaseId: {0}", databaseId);
		DatabaseUser databaseUser = userRepository.getUserById(databaseId);
		
		if (databaseUser == null) {
			return false;
		}
		if (databaseUser.getPasswordHash() == null || databaseUser.getPasswordHash().equals("")) {
			return false;
		}
		LOGGER.infov("  user:  {0}", user.getUsername());

		UserCredentialModel credential = (UserCredentialModel) input;
		String salt = model.get(DBFederationConstants.CONFIG_SALT);
		String hashedPassword = PasswordUtils.sha512(salt + credential.getValue());
		LOGGER.infov("  input:  {0}", credential.getValue());
		LOGGER.infov("  inputHash:  {0}", hashedPassword);
		LOGGER.infov("  passwordHash:  {0}", databaseUser.getPasswordHash());
		return databaseUser.getPasswordHash().equals(hashedPassword);
	}

	// UserLookupProvider Methods
	
	@Override
	public UserModel getUserById(RealmModel realm, String id) {
		LOGGER.infov("getUserById({0})", id);
		StorageId storageId = new StorageId(id);
        String username = storageId.getExternalId();
        return getUserByUsername(realm, username);
	}

	@Override
	public UserModel getUserByUsername(RealmModel realm, String username) {
		LOGGER.infov("getUserByUsername({0})", username);
		DatabaseUser user = userRepository.getUserByUsername(username);
		if (user == null) {
			return null;
		}
		return importUserToKeycloak(realm, user);
	}

	@Override
	public UserModel getUserByEmail(RealmModel realm, String email) {
		LOGGER.infov("getUserByEmail({0})", email);
		
		DatabaseUser user = userRepository.getUserByEmail(email);
		if (user == null) {
			return null;
		}
		
		return importUserToKeycloak(realm, user);
	}
	
	// UserProfileProvider Methods
	
	@Override
	public UserProfile create(UserProfileContext context, UserModel user) {
		LOGGER.infov("createProfile({0})", user);
		return null;
	}

	@Override
	public UserProfile create(UserProfileContext context, Map<String, ?> attributes) {
		LOGGER.infov("createProfile({0})", attributes);
		return null;
	}

	@Override
	public UserProfile create(UserProfileContext context, Map<String, ?> attributes, UserModel user) {
		LOGGER.infov("createProfile({0}, {1})", attributes, user);
		return null;
	}

	@Override
	public String getConfiguration() {
		return null;
	}

	@Override
	public void setConfiguration(String configuration) {
		
	}

	// Helpers
	
//	private String getPassword(UserModel user) {
//		return user.getFirstAttribute(DBFederationConstants.ATTRIBUTE_PASSWORD_HASH);
////		if (user instanceof CachedUserModel) {
////			return (String) ((CachedUserModel) user).getCachedWith().get(PASSWORD_CACHE_KEY);
////		}
////		if (user instanceof UserAdapter) {
////			return ((UserAdapter) user).getFirstAttribute("PASSWORD_HASH");
////		}
////		return null;
//	}
	
	protected UserModel importUserToKeycloak(RealmModel realm, DatabaseUser databaseUser) {
		LOGGER.infov("importUserToKeycloak({0})", databaseUser);
		
		UserModel importedUser = null;
		UserModel existingKeycloakUser = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, databaseUser.getUsername());
		if (existingKeycloakUser != null) {
			LOGGER.infov("  already exists {0}", existingKeycloakUser);
			importedUser = existingKeycloakUser;
		} else {
			LOGGER.info("  doesn't exist");
			importedUser = createAdapter(realm, databaseUser);
		}
		
		return importedUser;
	}
	
	protected UserModel createAdapter(RealmModel realm, DatabaseUser databaseUser) {
		LOGGER.infov("createAdapter({0})", databaseUser);
		UserModel local = UserStoragePrivateUtil.userLocalStorage(session).addUser(realm, databaseUser.getUsername());
        local.setFederationLink(model.getId());
        local.setEmail(databaseUser.getEmail());
        local.setFirstName(databaseUser.getFirstName());
        local.setLastName(databaseUser.getLastName());
        local.setEnabled(true);
        local.setSingleAttribute(DBFederationConstants.ATTRIBUTE_DATABASE_ID, databaseUser.getId().toString());
        return local;
	}

	@Override
	public void onEvent(Event event) {
		// Do Nothing
	}

	@Override
	public void onEvent(AdminEvent event, boolean includeRepresentation) {
		LOGGER.infov("onAdminEvent({0}, {1})", event.getId(), event.getOperationType());
	}

}