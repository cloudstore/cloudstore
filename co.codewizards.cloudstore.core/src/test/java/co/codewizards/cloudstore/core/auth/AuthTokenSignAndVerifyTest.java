package co.codewizards.cloudstore.core.auth;

import static org.assertj.core.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.junit.Test;

public class AuthTokenSignAndVerifyTest {
	private static SecureRandom random = new SecureRandom();

	@Test
	public void signAndVerifyWithValidPublicKey() throws Exception {
		AuthToken authToken = AuthTokenIOTest.createAuthToken();
		byte[] authTokenData = new AuthTokenIO().serialise(authToken);
		KeyPair keyPair = createKeyPair();
		SignedAuthToken signedAuthToken = new AuthTokenSigner(keyPair.getPrivate().getEncoded()).sign(authTokenData);
		assertThat(signedAuthToken).isNotNull();
		assertThat(signedAuthToken.getAuthTokenData()).isNotNull();
		assertThat(signedAuthToken.getSignature()).isNotNull();

		AuthTokenVerifier verifier = new AuthTokenVerifier(keyPair.getPublic().getEncoded());
		verifier.verify(signedAuthToken);
	}

	@Test(expected=SignatureException.class)
	public void signAndVerifyWithDifferencePublicKey() throws Exception {
		AuthToken authToken = AuthTokenIOTest.createAuthToken();
		byte[] authTokenData = new AuthTokenIO().serialise(authToken);
		KeyPair keyPair = createKeyPair();
		KeyPair keyPair2 = createKeyPair();
		SignedAuthToken signedAuthToken = new AuthTokenSigner(keyPair.getPrivate().getEncoded()).sign(authTokenData);
		assertThat(signedAuthToken).isNotNull();
		assertThat(signedAuthToken.getAuthTokenData()).isNotNull();
		assertThat(signedAuthToken.getSignature()).isNotNull();

		AuthTokenVerifier verifier = new AuthTokenVerifier(keyPair2.getPublic().getEncoded());
		verifier.verify(signedAuthToken);
	}

	@Test(expected=SignatureException.class)
	public void signAndVerifyCorruptData() throws Exception {
		AuthToken authToken = AuthTokenIOTest.createAuthToken();
		byte[] authTokenData = new AuthTokenIO().serialise(authToken);
		KeyPair keyPair = createKeyPair();
		SignedAuthToken signedAuthToken = new AuthTokenSigner(keyPair.getPrivate().getEncoded()).sign(authTokenData);
		assertThat(signedAuthToken).isNotNull();
		assertThat(signedAuthToken.getAuthTokenData()).isNotNull();
		assertThat(signedAuthToken.getSignature()).isNotNull();

		int index = random.nextInt(signedAuthToken.getAuthTokenData().length);
		byte oldValue = signedAuthToken.getAuthTokenData()[index];
		do {
			signedAuthToken.getAuthTokenData()[index] = (byte) random.nextInt();
		} while (oldValue == signedAuthToken.getAuthTokenData()[index]);

		AuthTokenVerifier verifier = new AuthTokenVerifier(keyPair.getPublic().getEncoded());
		verifier.verify(signedAuthToken);
	}

	private KeyPair createKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(4096, random);
		KeyPair pair = keyGen.generateKeyPair();
		return pair;
	}
}
