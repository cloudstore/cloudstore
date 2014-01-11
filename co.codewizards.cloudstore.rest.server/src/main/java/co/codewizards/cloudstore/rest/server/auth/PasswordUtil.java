package co.codewizards.cloudstore.rest.server.auth;

import java.security.SecureRandom;

public final class PasswordUtil {
	private PasswordUtil() { }

	private static SecureRandom random = new SecureRandom();

	private static final char[] PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-*/=.!?,;#$()[]{}~".toCharArray();

	public static char[] createRandomPassword(int length) {
		if (length < 8)
			throw new IllegalArgumentException("length < 8");

		char[] result = new char[length];

		for (int i = 0; i < result.length; ++i)
			result[i] = PASSWORD_CHARS[random.nextInt(PASSWORD_CHARS.length)];

		return result;
	}

}
