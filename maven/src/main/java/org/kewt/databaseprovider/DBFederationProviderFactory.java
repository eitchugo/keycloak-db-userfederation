package org.kewt.databaseprovider;

import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageUtil;
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
		final SynchronizationResult syncResult = new SynchronizationResult();

		class BooleanHolder {
			private boolean value = true;
	        }

	        final BooleanHolder exists = new BooleanHolder();

		LOGGER.infof("Sync all users from database to local store: realm: %s, federation provider: %s", realmId, model.getName());

		Instant start = Instant.now();
		
		try (DatabaseConnection connection = createConnection(model, false)) {
			DatabaseUserRepository userRepository = new DatabaseUserRepository(connection, model);
		        //Collection<DatabaseUser> databaseUsers_teste = userRepository.listUsers();

		        KeycloakSession session = sessionFactory.create();
			RealmModel currentRealm = session.realms().getRealm(realmId);
			session.getContext().setRealm(currentRealm);

		        UserProvider userProvider = UserStoragePrivateUtil.userLocalStorage(session);

		        Map<Integer, UserModel> keycloakUsersById = new HashMap<>();
		        {
				Map<String, String> search = new HashMap<String, String>();
				search.put(UserModel.SEARCH, "*");
				search.put(UserModel.INCLUDE_SERVICE_ACCOUNT, "false");
				userProvider.searchForUserStream(currentRealm, search).forEach((UserModel user) -> {
					LOGGER.infof("Do we need to clear cache?");
					if (UserStorageUtil.userCache(session) != null) {
						LOGGER.infof("Clearing cache for: %s", user.getUsername());
				                UserStorageUtil.userCache(session).evict(currentRealm, user);
			                }
					LOGGER.infof("USER: %s", user.getUsername());
					if (user.getFirstAttribute(DBFederationConstants.ATTRIBUTE_DATABASE_ID) != null) {
						keycloakUsersById.put(Integer.valueOf(user.getFirstAttribute(DBFederationConstants.ATTRIBUTE_DATABASE_ID)), user);
					}
				});
		        }

		        Collection<DatabaseUser> databaseUsers = userRepository.listUsers();

		        for (final DatabaseUser user : databaseUsers) {
				try {
					// Process each user in it's own transaction to avoid global fail
					KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

						@Override
						public void run(KeycloakSession session) {
							RealmModel currentRealm = session.realms().getRealm(realmId);
							session.getContext().setRealm(currentRealm);

							LOGGER.infof("Processing username: %s", user.getUsername());
							UserModel local = keycloakUsersById.get(user.getId());

							LOGGER.infof("Check if it exists");
							// update user to keycloak
							if (local != null) {
								LOGGER.infof("User already exists in local: %s", user.getUsername());
								if (UserStorageUtil.userCache(session) != null) {
									LOGGER.infof("Clearing cache for: %s", user.getUsername());
							                UserStorageUtil.userCache(session).evict(currentRealm, local);
						                }

								exists.value = true;
								if (user.outOfSync(local)) {
									user.syncUserModel(local);
									connection.commit();
									LOGGER.infof("Updated user from database: %s", user.getUsername());

									syncResult.increaseAdded();
								}
							// add new user to keycloak
							} else {
								LOGGER.infof("Adding user from database: %s", user.getUsername());
								local = userProvider.addUser(currentRealm, user.getUsername());
							        local.setFederationLink(model.getId());
							        local.setEmail(user.getEmail());
							        local.setFirstName(user.getFirstName());
							        local.setLastName(user.getLastName());
							        local.setEnabled(true);
							        local.setEmailVerified(true);
							        local.setSingleAttribute(DBFederationConstants.ATTRIBUTE_DATABASE_ID, user.getId().toString());
								LOGGER.infof("Commiting to database: %s", user.getUsername());
								connection.commit();
								exists.value = false;
								LOGGER.infof("Added user from database: %s", user.getUsername());

								syncResult.increaseAdded();
							}
						}
					});

				// TODO: Remove user if we already added him during this transaction
				} catch (ModelException me) {
					LOGGER.error("Failed during import user from database", me);
					syncResult.increaseFailed();

			                if (!exists.value) {
						KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

							@Override
							public void run(KeycloakSession session) {

								RealmModel currentRealm = session.realms().getRealm(realmId);
								session.getContext().setRealm(currentRealm);

								UserModel local = keycloakUsersById.get(user.getId());
								String username = user.getUsername();

								if (local != null) {
									UserModel existing = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(currentRealm, username);
									if (existing != null) {
										UserCache userCache = UserStorageUtil.userCache(session);
										if (userCache != null) {
											userCache.evict(currentRealm, existing);
										}
										UserStoragePrivateUtil.userLocalStorage(session).removeUser(currentRealm, existing);
									}
								}
							}
						});
					}
				}
			}
		}

		Instant end = Instant.now();
		double timeEllapsed = Duration.between(start, end).toMillis() / 1000.0;
		LOGGER.infov("  full sync took " + timeEllapsed + " seconds");
		
		return syncResult;
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
