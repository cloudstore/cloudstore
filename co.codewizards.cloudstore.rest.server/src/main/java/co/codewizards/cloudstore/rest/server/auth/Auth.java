package co.codewizards.cloudstore.rest.server.auth;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Authentication information (username + password). Can be obtained in every
 * REST service by sub-classing {@code AbstractService} and using
 * {@code AbstractService.getAuth()} or {@code AbstractService.authenticate(String)}.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class Auth
implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String userName;

	private char[] password;

	/**
	 * Create an empty instance.
	 */
	public Auth() { }

	/**
	 * Create an instance with the given values.
	 * @param userName the user-name (might be <code>null</code>).
	 * @param password the password (might be <code>null</code>).
	 */
	public Auth(String userName, char[] password)
	{
		this.userName = userName;
		this.password = password;
	}

	/**
	 * Get the user-name.
	 * @return the user-name or <code>null</code>.
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Set the user-name.
	 * @param userName the user-name or <code>null</code>.
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * <p>
	 * Get the password.
	 * </p>
 	 * <p>
	 * <b>Warning: the char-array returned by this method might be modified later</b> (overwritten with 0), e.g. if
	 * {@link #clear()} is called! If you want to use this char-array elsewhere, you must clone it immediately!
	 * </p>

	 * @return the password or <code>null</code>.
	 */
	public char[] getPassword() {
		return password;
	}

	/**
	 * <p>
	 * Set the password.
	 * </p>
	 * <p>
	 * <b>Warning: the char-array passed to this method is modified</b> (overwritten with 0), if
	 * {@link #clear()} is called! If you want to use this char-array elsewhere, you must pass
	 * a clone here!
	 * </p>
	 * @param password the password or <code>null</code>.
	 */
	public void setPassword(char[] password)
	{
		this.password = password;
	}

	/**
	 * Clear the sensitive data from this <code>Auth</code> instance. If the <code>password</code>
	 * is not <code>null</code>, it is overwritten with 0. This method is called by the
	 * {@link #finalize()} method of this class!
	 */
	public void clear()
	{
		if (password != null)
			Arrays.fill(password, (char)0);

		password = null;
	}

	@Override
	protected void finalize() throws Throwable {
		clear();
	}
}
