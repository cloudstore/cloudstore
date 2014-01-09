package co.codewizards.cloudstore.core.auth;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

public class AuthTokenSigner {
	public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
	private PrivateKey privateKey;

	public AuthTokenSigner(byte[] privateKeyData) {
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

	public SignedAuthToken sign(byte[] authTokenData) {
		assertNotNull("authTokenData", authTokenData);
		Signature signingEngine;
		try {
			signingEngine = Signature.getInstance(SIGNATURE_ALGORITHM);
			signingEngine.initSign(privateKey);
			signingEngine.update(authTokenData);
			byte[] signature = signingEngine.sign();

//			SignedObject signedObject = new SignedObject(authTokenData, privateKey, signingEngine);

			SignedAuthToken signedAuthToken = new SignedAuthToken();
			signedAuthToken.setAuthTokenData(authTokenData);
			signedAuthToken.setSignature(signature);
			return signedAuthToken;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
