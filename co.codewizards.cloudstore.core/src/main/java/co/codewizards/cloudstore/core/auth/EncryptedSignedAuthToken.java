package co.codewizards.cloudstore.core.auth;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class EncryptedSignedAuthToken {
	private byte[] encryptedSignedAuthTokenData;
	private byte[] encryptedSignedAuthTokenDataIV;
	private byte[] encryptedSymmetricKey;

	public byte[] getEncryptedSignedAuthTokenData() {
		return encryptedSignedAuthTokenData;
	}
	public void setEncryptedSignedAuthTokenData(byte[] authTokenData) {
		this.encryptedSignedAuthTokenData = authTokenData;
	}

	public byte[] getEncryptedSignedAuthTokenDataIV() {
		return encryptedSignedAuthTokenDataIV;
	}
	public void setEncryptedSignedAuthTokenDataIV(byte[] symmetricIv) {
		this.encryptedSignedAuthTokenDataIV = symmetricIv;
	}

	public byte[] getEncryptedSymmetricKey() {
		return encryptedSymmetricKey;
	}
	public void setEncryptedSymmetricKey(byte[] signature) {
		this.encryptedSymmetricKey = signature;
	}

}
