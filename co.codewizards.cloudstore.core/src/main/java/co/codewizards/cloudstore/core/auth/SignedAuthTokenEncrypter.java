package co.codewizards.cloudstore.core.auth;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;

public class SignedAuthTokenEncrypter {
	private static final Logger logger = LoggerFactory.getLogger(SignedAuthTokenEncrypter.class);

	public static final int DEFAULT_KEY_SIZE = 128;
	public static final String CONFIG_KEY_KEY_SIZE = "authTokenEncryption.keySize";

	private static SecureRandom random = new SecureRandom();

	private PublicKey publicKey;

	public SignedAuthTokenEncrypter(byte[] publicKeyData) {
		assertNotNull("publicKeyData", publicKeyData);
		BouncyCastleRegistrationUtil.registerBouncyCastleIfNeeded();
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyData);
			this.publicKey = keyFactory.generatePublic(publicKeySpec);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public EncryptedSignedAuthToken encrypt(byte[] signedAuthTokenData) {
		try {
			byte[] symKey = new byte[getKeySize() / 8];
			random.nextBytes(symKey);

			Cipher asymCipher = Cipher.getInstance("RSA/None/OAEPWITHSHA1ANDMGF1PADDING");
			asymCipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] symKeyEncrypted = asymCipher.doFinal(symKey);

			Cipher symCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//			symCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(symKey, "AES"), new IvParameterSpec(new byte[symKey.length]));
			symCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(symKey, "AES"));
			// We do not really need an IV, because we use a random key ONCE.
			// An IV is essentially important for security, if the key is used multiple times.
			// However, it doesn't cause us much trouble to transmit the IV and it may add
			// additional security due to the added complexity if it is not 0. Maybe the NSA
			// can attack easier, if the IV is 0. Very unlikely, but still. Hence we do not
			// enforce it to be 0 (which we could to save a few bytes in the transfer).
			// Marco :-)
			byte[] symIV = symCipher.getIV();
			byte[] signedAuthTokenDataEncrypted = symCipher.doFinal(signedAuthTokenData);

			EncryptedSignedAuthToken result = new EncryptedSignedAuthToken();
			result.setEncryptedSignedAuthTokenData(signedAuthTokenDataEncrypted);
			result.setEncryptedSignedAuthTokenDataIV(symIV);
			result.setEncryptedSymmetricKey(symKeyEncrypted);
			return result;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected int getKeySize() {
		int keySize = Config.getInstance().getPropertyAsInt(CONFIG_KEY_KEY_SIZE, DEFAULT_KEY_SIZE);
		if (keySize < 64) {
			logger.warn("Config key '{}': keySize {} is out of range! Using default {} instead!", CONFIG_KEY_KEY_SIZE, keySize, DEFAULT_KEY_SIZE);
			return DEFAULT_KEY_SIZE;
		}
		return keySize;
	}
}
