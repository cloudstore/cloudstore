package co.codewizards.cloudstore.core.auth;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.DateTime;

@XmlRootElement
public class AuthToken {
	private String password;
	private DateTime expiryDateTime;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public DateTime getExpiryDateTime() {
		return expiryDateTime;
	}

	public void setExpiryDateTime(DateTime expiryDateTime) {
		this.expiryDateTime = expiryDateTime;
	}
}