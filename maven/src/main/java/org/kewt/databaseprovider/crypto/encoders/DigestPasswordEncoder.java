package org.kewt.databaseprovider.crypto.encoders;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.crypto.password.PasswordEncoder;

public class DigestPasswordEncoder implements PasswordEncoder {
	
	private MessageDigest digester;
	
	public DigestPasswordEncoder(String algorithm) {
		this.digester = createDigester(algorithm);
	}

	@Override
	public String encode(CharSequence rawPassword) {
		return digest("", rawPassword);
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		String salt = extractSalt(encodedPassword);
		String rawPasswordEncoded = digest(salt, rawPassword);
		return passwordEquals(encodedPassword.toString(), rawPasswordEncoded);
	}
	
	private static MessageDigest createDigester(String algorithm) {
		try {
			return MessageDigest.getInstance(algorithm);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("No such hashing algorithm", ex);
		}
	}
	
	private String extractSalt(String encodedPassword) {
		return "";
	}
	
	private String digest(String salt, CharSequence rawPassword) {
		String saltedPassword = salt + rawPassword;
		byte[] digest = this.digester.digest(Utf8.encode(saltedPassword));
		String encoded = encode(digest);
		return salt + encoded;
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
