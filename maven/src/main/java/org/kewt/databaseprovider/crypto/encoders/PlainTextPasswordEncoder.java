package org.kewt.databaseprovider.crypto.encoders;

import org.springframework.security.crypto.password.PasswordEncoder;

public class PlainTextPasswordEncoder implements PasswordEncoder {

	@Override
	public String encode(CharSequence rawPassword) {
		return rawPassword.toString();
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		return rawPassword != null && encodedPassword != null && rawPassword.equals(encodedPassword);
	}

}
