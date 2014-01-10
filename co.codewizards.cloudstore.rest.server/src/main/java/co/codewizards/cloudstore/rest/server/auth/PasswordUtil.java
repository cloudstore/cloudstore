package co.codewizards.cloudstore.rest.server.auth;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PasswordUtil {
	private static final Logger logger = LoggerFactory.getLogger(PasswordUtil.class);

	private PasswordUtil() { }

	private static SecureRandom random = new SecureRandom();

	private static final char[] PASSWORD_CHARS;
	static {
		long beginTimestamp = System.currentTimeMillis();
		List<Character> passwordChars = new ArrayList<Character>();

		for (char c = 'A'; c <= 'Z'; ++c)
			passwordChars.add(c);

		for (char c = 'a'; c <= 'z'; ++c)
			passwordChars.add(c);

		for (char c = '0'; c <= '9'; ++c)
			passwordChars.add(c);

		final char[] specialChars = {
				'^', '°', '!', '"', '$', '%', '&', '/', '(', ')', '=', '?', '\'', '#', '+', '-', ',', '.', ';', // ':', MUST NOT contain ':' because this is a separator
				'_', '{', '}', '[', ']', '<', '>', '|', '*', '~'
				};
		for (char c : specialChars)
			passwordChars.add(c);

		PASSWORD_CHARS = new char[passwordChars.size()];
		int i = -1;
		for (Character character : passwordChars) {
			PASSWORD_CHARS[++i] = character;
		}
		logger.trace("Initialising PASSWORD_CHARS took {} ms: {}", System.currentTimeMillis() - beginTimestamp, Arrays.toString(PASSWORD_CHARS));
	}

	public static char[] createRandomPassword(int minLength, int maxLength) {
		if (minLength < 8)
			throw new IllegalArgumentException("minLength < 8");

		if (maxLength < minLength)
			throw new IllegalArgumentException("maxLength < minLength");

		int length = minLength + random.nextInt(maxLength - minLength + 1);
		char[] result = new char[length];

		for (int i = 0; i < result.length; ++i)
			result[i] = PASSWORD_CHARS[random.nextInt(PASSWORD_CHARS.length)];

		return result;
	}

}
