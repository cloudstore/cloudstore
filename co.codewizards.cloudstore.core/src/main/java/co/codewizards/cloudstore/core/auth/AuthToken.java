package co.codewizards.cloudstore.core.auth;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.DateTime;

@XmlRootElement
public class AuthToken {
	private String password;
	private DateTime renewalDateTime;
	private DateTime expiryDateTime;

	private volatile boolean writable = true;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		assertWritable();
		this.password = password;
	}

	/**
	 * Get the timestamp from which on a new token would be returned (but the old is still valid).
	 * @return
	 */
	public DateTime getRenewalDateTime() {
		return renewalDateTime;
	}
	public void setRenewalDateTime(DateTime renewalDateTime) {
		assertWritable();
		this.renewalDateTime = renewalDateTime;
	}

	public DateTime getExpiryDateTime() {
		return expiryDateTime;
	}

	public void setExpiryDateTime(DateTime expiryDateTime) {
		assertWritable();
		this.expiryDateTime = expiryDateTime;
	}

	public void makeUnmodifiable() {
		this.writable = false;
	}

	private void assertWritable() {
		if (!writable)
			throw new IllegalStateException("This AuthToken is not writable!");
	}
}