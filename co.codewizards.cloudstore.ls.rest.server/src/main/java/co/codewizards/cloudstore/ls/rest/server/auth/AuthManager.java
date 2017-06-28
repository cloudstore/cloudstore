package co.codewizards.cloudstore.ls.rest.server.auth;

import java.util.Arrays;

import co.codewizards.cloudstore.core.util.PasswordUtil;

// TODO implement changing passwords - sth. similar to TransientRepoPasswordManager
public class AuthManager {

	private final char[] password = PasswordUtil.createRandomPassword(25);

	protected AuthManager() { }

	private static final class Holder {
		public static final AuthManager instance = new AuthManager();
	}

	public static AuthManager getInstance() {
		return Holder.instance;
	}

	public char[] getCurrentPassword() {
		return password;
	}

	public boolean isPasswordValid(char[] password) {
		return Arrays.equals(this.password, password);
	}
}
