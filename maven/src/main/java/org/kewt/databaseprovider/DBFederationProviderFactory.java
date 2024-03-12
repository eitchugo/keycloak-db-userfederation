package org.kewt.databaseprovider;

import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jboss.logging.Logger;
import org.kewt.databaseprovider.crypto.PasswordHashFunction;
import org.kewt.databaseprovider.database.DatabaseConnection;
import org.kewt.databaseprovider.database.DatabaseException;
import org.kewt.databaseprovider.model.DatabaseUser;
import org.kewt.databaseprovider.repository.DatabaseUserRepository;
import org.keycloak.Config.Scope;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;

public class DBFederationProviderFactory implements UserStorageProviderFactory<DBFederationProvider>, ImportSynchronization {
	
	protected static final Logger LOGGER = Logger.getLogger(DBFederationProviderFactory.class);
	
	protected static final String PROVIDER_ID = "db-federation";
	
	protected static final List<ProviderConfigProperty> CONFIGURATION;
	
	static {
		CONFIGURATION = ProviderConfigurationBuilder.create()
			// Connection Settings
			.property()
				.name(DBFederationConstants.CONFIG_JDBC_URL)
				.label("user-federation-provider.db.jdbcUrl")
				.helpText("user-federation-provider.db.jdbcUrlHelp")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("jdbc:mysql://host:port/database")
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_JDBC_USERNAME)
				.label("user-federation-provider.db.username")
				.helpText("user-federation-provider.db.usernameHelp")
				.type(ProviderConfigProperty.STRING_TYPE)
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_JDBC_PASSWORD)
				.label("user-federation-provider.db.password")
				.helpText("user-federation-provider.db.passwordHelp")
				.type(ProviderConfigProperty.PASSWORD)
				.secret(true)
				.add()
			// Database Settings
			.property()
				.name(DBFederationConstants.CONFIG_USERS_TABLE)
				.label("user-federation-provider.db.usersTable")
				.helpText("user-federation-provider.db.usersTableHelp")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("users")
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_ID_COLUMN)
				.label("user-federation-provider.db.columnId")
				.helpText("user-federation-provider.db.columnIdHelp")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("id")
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_USERNAME_COLUMN)
				.label("user-federation-provider.db.columnUsername")
				.helpText("user-federation-provider.db.columnUsernameHelp")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("username")
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_EMAIL_COLUMN)
				.label("user-federation-provider.db.columnEmail")
				.helpText("user-federation-provider.db.columnEmailHelp")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("email")
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_FIRSTNAME_COLUMN)
				.label("user-federation-provider.db.columnFirstName")
				.helpText("user-federation-provider.db.columnFirstNameHelp")
				.type(ProviderConfigProperty.STRING_TYPE)
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_LASTNAME_COLUMN)
				.label("user-federation-provider.db.columnLastName")
				.helpText("user-federation-provider.db.columnLastNameHelp")
				.type(ProviderConfigProperty.STRING_TYPE)
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_PASSWORD_COLUMN)
				.label("user-federation-provider.db.columnPassword")
				.helpText("user-federation-provider.db.columnPasswordHelp")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("password")
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_PASSWORD_HASH_FUNCTION)
				.label("user-federation-provider.db.passwordHashFunction")
				.helpText("user-federation-provider.db.passwordHashFunctionHelp")
				.type(ProviderConfigProperty.LIST_TYPE)
				.defaultValue(PasswordHashFunction.BCRYPT.getId())
				.options(PasswordHashFunction.ids().toArray(String[]::new))
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_DIGEST_SALT)
				.label("user-federation-provider.db.digestSalt")
				.helpText("user-federation-provider.db.digestSaltHelp")
				.type(ProviderConfigProperty.PASSWORD)
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_BCRYPT_STRENGTH)
				.label("user-federation-provider.db.bcryptStrength")
				.helpText("user-federation-provider.db.bcryptStrengthHelp")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue(10)
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_PBKDF2_SALT_LENGTH)
				.label("user-federation-provider.db.pbkdf2SaltLength")
				.helpText("user-federation-provider.db.pbkdf2SaltLengthHelp")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue(16)
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_PBKDF2_ITERATIONS)
				.label("user-federation-provider.db.pbkdf2Iterations")
				.helpText("user-federation-provider.db.pbkdf2IterationsHelp")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue(300000)
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_SYNC_MODE)
				.label("user-federation-provider.db.syncMode")
				.helpText("user-federation-provider.db.syncModeHelp")
				.type(ProviderConfigProperty.LIST_TYPE)
				.defaultValue(DBFederationConstants.SYNC_READONLY)
				.options(DBFederationConstants.SYNC_OPTIONS)
				.add()
			.build();
	}

	// UserStorageProviderFactory Methods
	
	@Override
	public String getId() {
		return PROVIDER_ID;
	}
	
	@Override
	public void init(Scope config) {
		LOGGER.infov("Initializing DBFederationProviderFactory");
	}
	
	@Override
	public DBFederationProvider create(KeycloakSession session, ComponentModel model) {
		LOGGER.infov("Creating DBFederationProvider");
		DatabaseConnection connection = createConnection(model, true);
		return new DBFederationProvider(session, model, connection);
	}
	
	@Override
	public String getHelpText() {
		return "Database Federation";
	}
	
	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return CONFIGURATION;
	}
	
	@Override
	public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
		LOGGER.infov("Validating database configuration");
		
		try (DatabaseConnection database = createConnection(model, true)) {
			Integer value = database.querySingle("SELECT 1", null, (ResultSet rs) -> {
				return 1;
			});
			if (value == 1) {
				LOGGER.infov("Database configuration successful");
			}
		} catch (DatabaseException e) {
			throw new ComponentValidationException("Unable to connect to database", e);
		}
	}
	
	@Override
	public void close() {
		LOGGER.infov("Closing DatabaseUserStorageProviderFactory");
	}
	
	// ImportSynchronization
	
	@Override
	public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
		LOGGER.infov("Full Sync started");
		
		Instant start = Instant.now();
		SynchronizationResult result = new SynchronizationResult();
		
		Collection<DatabaseUser> databaseUsers = new ArrayList<>();
		try (DatabaseConnection connection = createConnection(model, false)) {
			DatabaseUserRepository userRepository = new DatabaseUserRepository(connection, model);
			databaseUsers = userRepository.listUsers();
		}
			
        for (DatabaseUser user : databaseUsers) {
        	LOGGER.debugv("  processing {0}", user.getUsername());
        	
        	try {
	        	KeycloakModelUtils.runJobInTransaction(sessionFactory, (KeycloakSession session) -> {
	        		RealmModel realm = session.realms().getRealm(realmId);
			        session.getContext().setRealm(realm);
			        UserProvider userProvider = UserStoragePrivateUtil.userLocalStorage(session);
			        
			        UserModel local = userProvider.searchForUserByUserAttributeStream(realm, DBFederationConstants.ATTRIBUTE_DATABASE_ID, user.getId().toString()).findFirst().orElse(null);
		        	if (local != null) {
		        		if (user.outOfSync(local)) {
		        			user.syncUserModel(local);
		        			result.increaseUpdated();
		        		}
		        	} else {
		        		local = userProvider.addUser(realm, user.getUsername());
				        local.setFederationLink(model.getId());
				        local.setEmail(user.getEmail());
				        local.setFirstName(user.getFirstName());
				        local.setLastName(user.getLastName());
				        local.setEnabled(true);
				        local.setEmailVerified(true);
				        local.setSingleAttribute(DBFederationConstants.ATTRIBUTE_DATABASE_ID, user.getId().toString());
				        result.increaseAdded();
		        	}
		    	});
        	} catch (Exception e) {
        		LOGGER.debug(new RuntimeException("Failed syncing user " + user.getUsername()));
        		result.increaseFailed();
        	}
        }
		
		Instant end = Instant.now();
		double timeEllapsed = Duration.between(start, end).toMillis() / 1000.0;
		LOGGER.infov("Full Sync ended in " + timeEllapsed + " seconds");
		
		return result;
	}
	
	@Override
	public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
		LOGGER.infov("syncSince()");
		return SynchronizationResult.empty();
	}
	
	private DatabaseConnection createConnection(ComponentModel model, boolean autoCommit) {
		String connectionUrl = model.get(DBFederationConstants.CONFIG_JDBC_URL);
		String username = model.get(DBFederationConstants.CONFIG_JDBC_USERNAME);
		String password = model.get(DBFederationConstants.CONFIG_JDBC_PASSWORD);
		return new DatabaseConnection(connectionUrl, username, password, autoCommit);
	}

}
