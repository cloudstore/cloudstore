package co.codewizards.cloudstore.ls.rest.server.auth;

import co.codewizards.cloudstore.core.util.PasswordUtil;

// TODO implement changing passwords - sth. similar to TransientRepoPasswordManager
public class AuthManager {

	private final String password = new String(PasswordUtil.createRandomPassword(25));

	protected AuthManager() { }

	private static final class Holder {
		public static final AuthManager instance = new AuthManager();
	}

	public static AuthManager getInstance() {
		return Holder.instance;
	}

	public String getCurrentPassword() {
		return password;
	}

	public boolean isPasswordValid(String password) {
		return this.password.equals(password);
	}
}
