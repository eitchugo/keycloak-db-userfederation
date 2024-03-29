package org.kewt.databaseprovider.crypto.encoders;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.crypto.password.PasswordEncoder;

public class DigestPasswordEncoder implements PasswordEncoder {

	private MessageDigest digester;
	
	private String fixedSalt;
	
	private Integer iterations;
	
	public DigestPasswordEncoder(String algorithm, String fixedSalt, Integer iterations) {
		this.digester = createDigester(algorithm);
		this.fixedSalt = fixedSalt != null ? fixedSalt : "";
		this.iterations = iterations != null ? iterations : 1;
	}
	
	// PasswordEncoder Methods

	@Override
	public String encode(CharSequence rawPassword) {
		return digest(fixedSalt, rawPassword);
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		String salt = extractSalt(encodedPassword);
		String rawPasswordEncoded = digest(salt, rawPassword);
		return passwordEquals(encodedPassword.toString(), rawPasswordEncoded);
	}
	
	// Private Methods
	
	private static MessageDigest createDigester(String algorithm) {
		try {
			return MessageDigest.getInstance(algorithm);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("No such hashing algorithm", ex);
		}
	}
	
	private String extractSalt(String encodedPassword) {
		return fixedSalt;
	}
	
	private String digest(String salt, CharSequence rawPassword) {
		String saltedPassword = (salt != null ? salt : "") + rawPassword;
		byte[] digest = this.digester.digest(Utf8.encode(saltedPassword));
		for (int i = 1; i < iterations; i++) {
			digest = this.digester.digest(Utf8.encode(encode(digest)));
		}
		String encoded = encode(digest);
		return encoded;
	}
	
	private String encode(byte[] digest) {
		return new String(Hex.encode(digest));
	}
	
	private boolean passwordEquals(String expected, String actual) {
		byte[] expectedBytes = bytesUtf8(expected);
		byte[] actualBytes = bytesUtf8(actual);
		return MessageDigest.isEqual(expectedBytes, actualBytes);
	}
	
	private static byte[] bytesUtf8(String s) {
		return (s != null) ? Utf8.encode(s) : null;
	}

}
