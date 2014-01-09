package co.codewizards.cloudstore.core.auth;

import static co.codewizards.cloudstore.core.auth.AuthTokenSigner.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class AuthTokenVerifier {
	private PublicKey publicKey;

	public AuthTokenVerifier(byte[] publicKeyData) {
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

	public void verify(SignedAuthToken signedAuthToken) {
		assertNotNull("signedAuthToken", signedAuthToken);
		assertNotNull("signedAuthToken.authTokenData", signedAuthToken.getAuthTokenData());
		assertNotNull("signedAuthToken.signature", signedAuthToken.getSignature());
		try {
			Signature verificationEngine = Signature.getInstance(SIGNATURE_ALGORITHM);
			verificationEngine.initVerify(publicKey);
			verificationEngine.update(signedAuthToken.getAuthTokenData());
			if (!verificationEngine.verify(signedAuthToken.getSignature())) {
				throw new SignatureException("Signature not valid.");
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
