package org.kewt.databaseprovider;

import java.util.Arrays;
import java.util.List;

public class DBFederationConstants {
	
	public static final String CONFIG_JDBC_URL = "jdbc_url";
	
	public static final String CONFIG_JDBC_USERNAME = "jdbc_user";
	
	public static final String CONFIG_JDBC_PASSWORD = "jdbc_password";
	
	public static final String CONFIG_SALT = "salt";
	
	public static final String CONFIG_USERS_TABLE = "users_table";
	
	public static final String CONFIG_ID_COLUMN = "id_column";
	
	public static final String CONFIG_USERNAME_COLUMN = "username_column";
	
	public static final String CONFIG_EMAIL_COLUMN = "email_column";
	
	public static final String CONFIG_FIRSTNAME_COLUMN = "firstname_column";
	
	public static final String CONFIG_LASTNAME_COLUMN = "lastname_column";
	
	public static final String CONFIG_PASSWORD_COLUMN = "password_column";
	
	public static final String CONFIG_PASSWORD_HASH_FUNCTION = "password_hash_function";
	
	public static final String CONFIG_BCRYPT_STRENGTH = "bcrypt_strength";
	
	public static final String CONFIG_PBKDF2_SALT_LENGTH = "pbkdf2_salt_length";
	
	public static final String CONFIG_PBKDF2_ITERATIONS  = "pbkdf2_iterations";
	
	public static final String CONFIG_SYNC_MODE  = "sync_mode";
	
	public static final String SYNC_READONLY = "READ_ONLY";
	
	public static final String SYNC_READWRITE = "READ_WRITE";
	
	public static final String SYNC_READWRITEDELETE = "READ_WRITE_DELETE";
	
	public static final List<String> SYNC_OPTIONS = Arrays.asList(SYNC_READONLY, SYNC_READWRITE, SYNC_READWRITEDELETE);
	
	public static final String ATTRIBUTE_DATABASE_ID = "DATABASE_ID";

}
