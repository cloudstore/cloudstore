package co.codewizards.cloudstore.core.auth;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SignedAuthToken {
	private byte[] authTokenData;
	private byte[] signature;

	public byte[] getAuthTokenData() {
		return authTokenData;
	}

	public void setAuthTokenData(byte[] authTokenData) {
		this.authTokenData = authTokenData;
	}

	public byte[] getSignature() {
		return signature;
	}

	public void setSignature(byte[] signature) {
		this.signature = signature;
	}
}
