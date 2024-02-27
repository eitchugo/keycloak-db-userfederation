package org.kewt.databaseprovider;

import java.sql.ResultSet;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;
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
	
	protected static final Logger LOGGER = Logger.getLogger(DBFederationProvider.class);
	
	protected static final String PROVIDER_ID = "db-federation";
	
	protected static final List<ProviderConfigProperty> CONFIGURATION;
	
	static {
		CONFIGURATION = ProviderConfigurationBuilder.create()
			// Connection Settings
			.property()
				.name("connection_settings")
				.label("")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("# CONNECTION SETTINGS")
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_JDBC_URL)
				.label("JDBC URL")
				.helpText("Database JDBC connection string")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("jdbc:mysql://host:port/database")
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_JDBC_USERNAME)
				.label("Username")
				.helpText("Database username")
				.type(ProviderConfigProperty.STRING_TYPE)
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_JDBC_PASSWORD)
				.label("Password")
				.helpText("Database password")
				.type(ProviderConfigProperty.PASSWORD)
				.secret(true)
				.add()
			.property()
				.name("database_settings")
				.label("")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("# DATABASE SETTINGS")
				.add()
			// Database Settings
			.property()
				.name(DBFederationConstants.CONFIG_USERS_TABLE)
				.label("Users Table")
				.helpText("Users Table")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("users")
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_ID_COLUMN)
				.label("Id Column")
				.helpText("ID Column")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("id")
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_USERNAME_COLUMN)
				.label("Username Column")
				.helpText("Username Column")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("username")
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_EMAIL_COLUMN)
				.label("Email Column")
				.helpText("Email Column")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("email")
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_FIRSTNAME_COLUMN)
				.label("First Name Column")
				.helpText("First Name Column")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("first_name")
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_LASTNAME_COLUMN)
				.label("Last Name Column")
				.helpText("Last Name Column")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("last_name")
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_PASSWORD_COLUMN)
				.label("Password Column")
				.helpText("Password Column")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("password_hash")
				.add()
			.property()
				.name(DBFederationConstants.CONFIG_SALT)
				.label("Password Salt")
				.helpText("Password Salt")
				.type(ProviderConfigProperty.PASSWORD)
				.secret(true)
				.add()
			.property()
				.name("sync_settings")
				.label("")
				.type(ProviderConfigProperty.STRING_TYPE)
				.defaultValue("### SYNC SETTINGS")
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
			LOGGER.infov("Database configuration successful");
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
		LOGGER.infov("sync:");
		
		AtomicInteger added = new AtomicInteger();
		AtomicInteger removed = new AtomicInteger();
		AtomicInteger updated = new AtomicInteger();
		AtomicInteger failed = new AtomicInteger();
		
		KeycloakModelUtils.runJobInTransaction(sessionFactory, (KeycloakSession session) -> {
			try (DatabaseConnection connection = createConnection(model, false)) {
				DatabaseUserRepository userRepository = new DatabaseUserRepository(connection, model);
				
				RealmModel realm = session.realms().getRealm(realmId);
		        session.getContext().setRealm(realm);
		        UserProvider userProvider = UserStoragePrivateUtil.userLocalStorage(session);
		        
		        for (DatabaseUser user : userRepository.listUsers()) {
		        	LOGGER.infov("  processing {0}", user);
		        	UserModel local = userProvider.searchForUserByUserAttributeStream(realm, DBFederationConstants.ATTRIBUTE_DATABASE_ID, user.getId().toString()).findFirst().orElse(null);
		        	if (local != null) {
		        		if (user.outOfSync(local)) {
		        			user.syncUserModel(local);
		        			updated.incrementAndGet();
		        		}
		        	} else {
		        		local = UserStoragePrivateUtil.userLocalStorage(session).addUser(realm, user.getUsername());
				        local.setFederationLink(model.getId());
				        local.setEmail(user.getEmail());
				        local.setFirstName(user.getFirstName());
				        local.setLastName(user.getLastName());
				        local.setEnabled(true);
				        local.setSingleAttribute(DBFederationConstants.ATTRIBUTE_DATABASE_ID, user.getId().toString());
				        added.incrementAndGet();
		        	}
		        }
			}
		});
		
		SynchronizationResult result = new SynchronizationResult();
		result.setAdded(added.get());
		result.setRemoved(removed.get());
		result.setUpdated(updated.get());
		result.setFailed(failed.get());
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
