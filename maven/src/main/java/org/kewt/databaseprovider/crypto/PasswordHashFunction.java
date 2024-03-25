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
	MD5("DIGEST_MD5"),
	SHA1("DIGEST_SHA1"),
	SHA256("DIGEST_SHA256"),
	SHA512("DIGEST_SHA512"),
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
		String digestSalt = getString(model, DBFederationConstants.CONFIG_DIGEST_SALT, "");
		Integer digestIterations = getInteger(model, DBFederationConstants.CONFIG_DIGEST_ITERATIONS, 1);
		Integer bcryptStrength = getInteger(model, DBFederationConstants.CONFIG_BCRYPT_STRENGTH, 10);
		Integer pbkdf2SaltLength = getInteger(model, DBFederationConstants.CONFIG_PBKDF2_SALT_LENGTH, 16);
		Integer pbkdf2Iterations = getInteger(model, DBFederationConstants.CONFIG_PBKDF2_ITERATIONS, 300000);
		switch (this) {
			case PBKDF2_SHA1:
				return new Pbkdf2PasswordEncoder("pepper", pbkdf2SaltLength, pbkdf2Iterations, SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA1);
			case PBKDF2_SHA256:
				return new Pbkdf2PasswordEncoder("pepper", pbkdf2SaltLength, pbkdf2Iterations, SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
			case PBKDF2_SHA512:
				return new Pbkdf2PasswordEncoder("pepper", pbkdf2SaltLength, pbkdf2Iterations, SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA512);
			case BCRYPT:
				return new BCryptPasswordEncoder(bcryptStrength);
			case PLAIN_TEXT:
				return new PlainTextPasswordEncoder();
			case MD5:
				return new DigestPasswordEncoder("MD5", digestSalt, digestIterations);
			case SHA1:
				return new DigestPasswordEncoder("SHA-1", digestSalt, digestIterations);
			case SHA256:
				return new DigestPasswordEncoder("SHA-256", digestSalt, digestIterations);
			case SHA512:
				return new DigestPasswordEncoder("SHA-512", digestSalt, digestIterations);
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
	
	// Private Methods
	
	private String getString(ComponentModel model, String key, String defaultValue) {
		return model.get(key, defaultValue);
	}
	
	private Integer getInteger(ComponentModel model, String key, Integer defaultValue) {
		return Integer.valueOf(model.get(key, defaultValue.toString()));
	}

}
