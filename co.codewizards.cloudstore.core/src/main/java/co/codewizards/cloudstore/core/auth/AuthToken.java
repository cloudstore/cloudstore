package co.codewizards.cloudstore.core.auth;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.DateTime;

@XmlRootElement
public class AuthToken {
	private String password;
	private DateTime renewalDateTime;
	private DateTime expiryDateTime;

	private volatile boolean writable = true;

	/**
	 * Gets the password, currently valid for authentication at the server.
	 * @return the current password. Never <code>null</code>.
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * Sets the current password.
	 * @param password the current password. Should not be <code>null</code>.
	 * @throws IllegalStateException after {@link #makeUnmodifiable()} was invoked.
	 */
	public void setPassword(String password) {
		assertWritable();
		this.password = password;
	}

	/**
	 * Gets the timestamp from which on a new token would be returned (but the old is still valid).
	 * @return the timestamp from which on a new token can be obtained by the client. Never <code>null</code>.
	 */
	public DateTime getRenewalDateTime() {
		return renewalDateTime;
	}
	/**
	 *
	 * @param renewalDateTime
	 * @throws IllegalStateException after {@link #makeUnmodifiable()} was invoked.
	 */
	public void setRenewalDateTime(DateTime renewalDateTime) {
		assertWritable();
		this.renewalDateTime = renewalDateTime;
	}

	/**
	 * Gets the timestamp when the current token expires.
	 * <p>
	 * Trying to authenticate with the current {@link #getPassword() password} after this date + time
	 * will fail.
	 * @return the timestamp when the current token expires. Never <code>null</code>.
	 */
	public DateTime getExpiryDateTime() {
		return expiryDateTime;
	}
	/**
	 * Sets the timestamp when the current token expires.
	 * @param expiryDateTime the timestamp when the current token expires. Should not be <code>null</code>.
	 * @throws IllegalStateException after {@link #makeUnmodifiable()} was invoked.
	 */
	public void setExpiryDateTime(DateTime expiryDateTime) {
		assertWritable();
		this.expiryDateTime = expiryDateTime;
	}

	/**
	 * Makes this instance immutable.
	 * <p>
	 * After this method was invoked, all setters throw an {@link IllegalStateException}.
	 */
	public void makeUnmodifiable() {
		this.writable = false;
	}

	/**
	 * Asserts that this instance is writable.
	 * <p>
	 * This method does nothing, if this instance may be modified. After {@link #makeUnmodifiable()} was
	 * invoked, this method throws an {@link IllegalStateException}.
	 * <p>
	 * All setters must call this method before modifying the object.
	 * @throws IllegalStateException after {@link #makeUnmodifiable()} was invoked.
	 */
	private void assertWritable() {
		if (!writable)
			throw new IllegalStateException("This AuthToken is not writable!");
	}
}