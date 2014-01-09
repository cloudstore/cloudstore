package co.codewizards.cloudstore.core.auth;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SignedAuthTokenDecrypter {
	private PrivateKey privateKey;

	public SignedAuthTokenDecrypter(byte[] privateKeyData) {
		assertNotNull("privateKeyData", privateKeyData);
		BouncyCastleRegistrationUtil.registerBouncyCastleIfNeeded();
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyData);
			this.privateKey = keyFactory.generatePrivate(privateKeySpec);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] decrypt(EncryptedSignedAuthToken encryptedSignedAuthToken) {
		assertNotNull("encryptedSignedAuthToken", encryptedSignedAuthToken);
		assertNotNull("encryptedSignedAuthToken.encryptedSignedAuthTokenData", encryptedSignedAuthToken.getEncryptedSignedAuthTokenData());
		assertNotNull("encryptedSignedAuthToken.encryptedSymmetricKey", encryptedSignedAuthToken.getEncryptedSymmetricKey());
		try {
			Cipher asymCipher = Cipher.getInstance("RSA/None/OAEPWITHSHA1ANDMGF1PADDING");
			asymCipher.init(Cipher.DECRYPT_MODE, privateKey);
			byte[] symKey = asymCipher.doFinal(encryptedSignedAuthToken.getEncryptedSymmetricKey());

			Cipher symCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			symCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(symKey, "AES"),
					new IvParameterSpec(encryptedSignedAuthToken.getEncryptedSignedAuthTokenDataIV()));

			byte[] signedAuthTokenData = symCipher.doFinal(encryptedSignedAuthToken.getEncryptedSignedAuthTokenData());

			return signedAuthTokenData;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
