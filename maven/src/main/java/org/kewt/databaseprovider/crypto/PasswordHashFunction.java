package org.kewt.databaseprovider.crypto;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.kewt.databaseprovider.DBFederationConstants;
import org.kewt.databaseprovider.crypto.encoders.DigestPasswordEncoder;
import org.kewt.databaseprovider.crypto.encoders.PlainTextPasswordEncoder;
import org.keycloak.component.ComponentModel;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm;

public enum PasswordHashFunction {
	
	PLAIN_TEXT("PLAIN_TEXT"),
	MD5("MD5"),
	SHA1("SHA1"),
	SHA256("SHA256"),
	SHA512("SHA512"),
	PBKDF2_SHA1("PBKDF2_SHA1"),
	PBKDF2_SHA256("PBKDF2_SHA256"),
	PBKDF2_SHA512("PBKDF2_SHA512"),
	BCRYPT("BCRYPT");
	
	private static Map<String, PasswordHashFunction> BY_ID;
	
	private String id;
	
	PasswordHashFunction(String id) {
		this.id = id;
	}
	
	public String getId() {
		return this.id;
	}
	
	public String digest(String password, ComponentModel model) {
		return getPasswordEncoder(model).encode(password);
	}
	
	public boolean verify(String password, String hash, ComponentModel model) {
		return getPasswordEncoder(model).matches(password, hash);
	}
	
	public PasswordEncoder getPasswordEncoder(ComponentModel model) {
		Integer strength = Integer.valueOf(model.get(DBFederationConstants.CONFIG_BCRYPT_STRENGTH, 10));
		Integer saltLength = Integer.valueOf(model.get(DBFederationConstants.CONFIG_PBKDF2_SALT_LENGTH, 16));
		Integer iterations = Integer.valueOf(model.get(DBFederationConstants.CONFIG_PBKDF2_ITERATIONS, 300000));
		switch (this) {
			case PBKDF2_SHA1:
				return new Pbkdf2PasswordEncoder("pepper", saltLength, iterations, SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA1);
			case PBKDF2_SHA256:
				return new Pbkdf2PasswordEncoder("pepper", saltLength, iterations, SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
			case PBKDF2_SHA512:
				return new Pbkdf2PasswordEncoder("pepper", saltLength, iterations, SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA512);
			case BCRYPT:
				return new BCryptPasswordEncoder(strength);
			case PLAIN_TEXT:
				return new PlainTextPasswordEncoder();
			case MD5:
				return new DigestPasswordEncoder("MD5");
			case SHA1:
				return new DigestPasswordEncoder("SHA-1");
			case SHA256:
				return new DigestPasswordEncoder("SHA-256");
			case SHA512:
				return new DigestPasswordEncoder("SHA-512");
			default:
				throw new IllegalArgumentException();
		}
	}
	
	public static Map<String, PasswordHashFunction> getMappingById() {
		if (BY_ID == null) {
			BY_ID = new TreeMap<>();
			for (PasswordHashFunction method : PasswordHashFunction.values()) {
				BY_ID.put(method.getId(), method);
			}
		}
		return BY_ID;
	}
	
	public static PasswordHashFunction getById(String id) {
		return getMappingById().get(id);
	}
	
	public static Collection<String> ids() {
		return getMappingById().keySet();
	}

}
