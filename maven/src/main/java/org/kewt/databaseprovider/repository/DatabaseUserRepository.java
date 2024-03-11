package org.kewt.databaseprovider.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.kewt.databaseprovider.DBFederationConstants;
import org.kewt.databaseprovider.database.DatabaseConnection;
import org.kewt.databaseprovider.database.callbacks.QueryReader;
import org.kewt.databaseprovider.model.DatabaseUser;
import org.keycloak.component.ComponentModel;

public class DatabaseUserRepository {
	
	private DatabaseConnection connection;
	
	private ComponentModel model;
	
	private String usersTable;
	
	private String idColumn;
	
	private String usernameColumn;
	
	private String emailColumn;
	
	private String firstNameColumn;
	
	private String lastNameColumn;
	
	private String passwordColumn;
	
	private QueryReader<DatabaseUser> reader;
	
	public DatabaseUserRepository(DatabaseConnection connection, ComponentModel model) {
		this.connection = connection;
		this.model = model;
		this.usersTable = ObjectUtils.firstNonNull(this.model.get(DBFederationConstants.CONFIG_USERS_TABLE), "users");
		this.idColumn = ObjectUtils.firstNonNull(this.model.get(DBFederationConstants.CONFIG_ID_COLUMN), "id");
		this.usernameColumn = ObjectUtils.firstNonNull(this.model.get(DBFederationConstants.CONFIG_USERNAME_COLUMN), "username");
		this.emailColumn = ObjectUtils.firstNonNull(this.model.get(DBFederationConstants.CONFIG_EMAIL_COLUMN), "email");
		this.firstNameColumn = ObjectUtils.firstNonNull(this.model.get(DBFederationConstants.CONFIG_FIRSTNAME_COLUMN), "first_name");
		this.lastNameColumn = ObjectUtils.firstNonNull(this.model.get(DBFederationConstants.CONFIG_LASTNAME_COLUMN), "last_name");
		this.passwordColumn = ObjectUtils.firstNonNull(this.model.get(DBFederationConstants.CONFIG_PASSWORD_COLUMN), "password_hash");
		this.reader = (ResultSet rs) -> {
			Set<String> columns = new HashSet<>();
			ResultSetMetaData metadata = rs.getMetaData();
		    for (int i = 1; i <= metadata.getColumnCount(); i++) {
		    	columns.add(metadata.getColumnName(i));
		    }

			DatabaseUser user = new DatabaseUser();
			if (columns.contains(idColumn)) {
				user.setId(rs.getInt(idColumn));
			}
			if (columns.contains(usernameColumn)) {
				user.setUsername(rs.getString(usernameColumn));
			}
			if (columns.contains(emailColumn)) {
				user.setEmail(rs.getString(emailColumn));
			}
			if (columns.contains(firstNameColumn)) {
				user.setFirstName(rs.getString(firstNameColumn));
			}
			if (columns.contains(lastNameColumn)) {
				user.setLastName(rs.getString(lastNameColumn));
			}
			if (columns.contains(passwordColumn)) {
				user.setPasswordHash(rs.getString(passwordColumn));
			}
			return user;
		};
	}
	
	public List<DatabaseUser> listUsers() {
		String sql = "select * from " + usersTable;
		return connection.queryList(sql, null, reader);
	}
	
	public List<DatabaseUser> listUsers(Integer firstResult, Integer maxResults) {
		String sql = "select * from " + usersTable + " limit ? offset ?";
		return connection.queryList(sql, (PreparedStatement statement) -> {
			statement.setInt(1, firstResult);
			statement.setInt(2, maxResults);
		}, reader);
	}
	
	public List<DatabaseUser> searchUsers(String search, Integer firstResult, Integer maxResults) {
		if (search.equals("*")) {
			return listUsers(firstResult, maxResults);
		}
		String sql = "select * from " + usersTable + " where " + usernameColumn + " like ? or " + usernameColumn + " like ? or " + firstNameColumn + " like ? or " + lastNameColumn + " like ? limit ? offset ?";
		String value = "%" + search + "%";
		return connection.queryList(sql, (PreparedStatement statement) -> {
			statement.setString(1, value);
			statement.setString(2, value);
			statement.setString(3, value);
			statement.setString(4, value);
			statement.setInt(5, firstResult);
			statement.setInt(6, maxResults);
		}, reader);
	}
	
	public Integer countUsers() {
		return connection.querySingle("select count(*) from " + usersTable, null, (ResultSet rs) -> {
			return rs.getInt(1);
		});
	}
	
	public DatabaseUser getUserById(Integer id) {
		String sql = "select * from " + usersTable + " where " + idColumn + " = ?";
		return connection.querySingle(sql, (PreparedStatement statement) -> {
			statement.setInt(1, id);
		}, reader);
	}
	
	public DatabaseUser getUserByUsername(String username) {
		String sql = "select * from " + usersTable + " where " + usernameColumn + " = ?";
		return connection.querySingle(sql, (PreparedStatement statement) -> {
			statement.setString(1, username);
		}, reader);
	}
	
	public DatabaseUser getUserByEmail(String email) {
		String sql = "select * from " + usersTable + " where " + emailColumn + " = ?";
		return connection.querySingle(sql, (PreparedStatement statement) -> {
			statement.setString(1, email);
		}, reader);
	}
	
	public boolean updatePassword(Integer id, String password) {
		String sql = "update " + usersTable + " set " + passwordColumn + " = ? where " + idColumn + " = ?";
        return connection.execute(sql, (PreparedStatement statement) -> {
        	statement.setString(1, password);
			statement.setInt(2, id);
        }) > 0;
	}
	
	public Integer insert(DatabaseUser user) {
		String sql = "insert into " + usersTable + " (" + usernameColumn + ", " + emailColumn + ", " + firstNameColumn + ", " + lastNameColumn + ", " + passwordColumn + ") values (?, ?, ?, ?, ?)";
		return connection.executeAndReturnGeneratedKeys(sql, (PreparedStatement statement) -> {
			statement.setString(1, user.getUsername());
			statement.setString(2, user.getEmail());
			statement.setString(3, user.getFirstName());
			statement.setString(4, user.getLastName());
			statement.setString(5, user.getPasswordHash());
		}, (ResultSet rs) -> {
			user.setId(rs.getInt(1));
			return user.getId();
		});
	}
	
	public boolean update(DatabaseUser user) {
		String sql = "update " + usersTable + " set " + usernameColumn + " = ?, " + emailColumn + " = ?, " + firstNameColumn + " = ?, " + lastNameColumn + "= ? where " + idColumn + " = ?";
		return connection.execute(sql, (PreparedStatement statement) -> {
			statement.setString(1, user.getUsername());
			statement.setString(2, user.getEmail());
			statement.setString(3, user.getFirstName());
			statement.setString(4, user.getLastName());
			statement.setInt(5, user.getId());
		}) > 0;
	}
	
	public boolean delete(DatabaseUser user) {
		String sql = "delete from " + usersTable + " where " + idColumn + " = ?";
		return connection.execute(sql, (PreparedStatement statement) -> {
			statement.setInt(1, user.getId());
		}) > 0;
	}

}