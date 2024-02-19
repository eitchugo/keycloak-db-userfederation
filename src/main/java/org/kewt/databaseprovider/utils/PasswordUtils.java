package org.kewt.databaseprovider.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtils {

	public static String sha512(String string) {
	    String generatedPassword = null;
	    try {
	        MessageDigest digest = MessageDigest.getInstance("SHA-512");
	        digest.update(string.getBytes(StandardCharsets.UTF_8));
	        byte[] bytes = digest.digest();
	        StringBuilder sb = new StringBuilder();
	        for(int i = 0; i < bytes.length; i++) {
	            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
	        }
	        generatedPassword = sb.toString();
	    } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
	    }
	    return generatedPassword;
	}
	
	public static void main(String[] args) {
		System.out.println(sha512("foo" + "12mudar34"));
	}

}
