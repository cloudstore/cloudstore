package co.codewizards.cloudstore.core.auth;

import static java.lang.System.*;
import static org.assertj.core.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthTokenSignAndVerifyTest {
	private static SecureRandom random = new SecureRandom();

	private static final Logger logger = LoggerFactory.getLogger(AuthTokenSignAndVerifyTest.class);

	{
		logger.debug("[{}]<init>", Integer.toHexString(identityHashCode(this)));
	}

	@Test
	public void signAndVerifyWithValidPublicKey() throws Exception {
		logger.debug("[{}]signAndVerifyWithValidPublicKey: entered.", Integer.toHexString(identityHashCode(this)));
		final AuthToken authToken = AuthTokenIOTest.createAuthToken();
		final byte[] authTokenData = new AuthTokenIO().serialise(authToken);
		final KeyPair keyPair = createKeyPair();
		final SignedAuthToken signedAuthToken = new AuthTokenSigner(keyPair.getPrivate().getEncoded()).sign(authTokenData);
		assertThat(signedAuthToken).isNotNull();
		assertThat(signedAuthToken.getAuthTokenData()).isNotNull();
		assertThat(signedAuthToken.getSignature()).isNotNull();

		final AuthTokenVerifier verifier = new AuthTokenVerifier(keyPair.getPublic().getEncoded());
		verifier.verify(signedAuthToken);
	}

	@Test(expected=SignatureException.class)
	public void signAndVerifyWithDifferentPublicKey() throws Exception {
		logger.debug("[{}]signAndVerifyWithDifferentPublicKey: entered.", Integer.toHexString(identityHashCode(this)));
		final AuthToken authToken = AuthTokenIOTest.createAuthToken();
		final byte[] authTokenData = new AuthTokenIO().serialise(authToken);
		final KeyPair keyPair = createKeyPair();
		final KeyPair keyPair2 = createKeyPair();
		final SignedAuthToken signedAuthToken = new AuthTokenSigner(keyPair.getPrivate().getEncoded()).sign(authTokenData);
		assertThat(signedAuthToken).isNotNull();
		assertThat(signedAuthToken.getAuthTokenData()).isNotNull();
		assertThat(signedAuthToken.getSignature()).isNotNull();

		final AuthTokenVerifier verifier = new AuthTokenVerifier(keyPair2.getPublic().getEncoded());
		verifier.verify(signedAuthToken);
	}

	@Test(expected=SignatureException.class)
	public void signAndVerifyCorruptData() throws Exception {
		logger.debug("[{}]signAndVerifyCorruptData: entered.", Integer.toHexString(identityHashCode(this)));
		final AuthToken authToken = AuthTokenIOTest.createAuthToken();
		final byte[] authTokenData = new AuthTokenIO().serialise(authToken);
		final KeyPair keyPair = createKeyPair();
		final SignedAuthToken signedAuthToken = new AuthTokenSigner(keyPair.getPrivate().getEncoded()).sign(authTokenData);
		assertThat(signedAuthToken).isNotNull();
		assertThat(signedAuthToken.getAuthTokenData()).isNotNull();
		assertThat(signedAuthToken.getSignature()).isNotNull();

		final int index = random.nextInt(signedAuthToken.getAuthTokenData().length);
		final byte oldValue = signedAuthToken.getAuthTokenData()[index];
		do {
			signedAuthToken.getAuthTokenData()[index] = (byte) random.nextInt();
		} while (oldValue == signedAuthToken.getAuthTokenData()[index]);

		final AuthTokenVerifier verifier = new AuthTokenVerifier(keyPair.getPublic().getEncoded());
		verifier.verify(signedAuthToken);
	}

	private KeyPair createKeyPair() throws NoSuchAlgorithmException {
		final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
//		keyGen.initialize(4096, random);
		keyGen.initialize(1024, random); // much faster - we don't need high security for testing only!
		final KeyPair pair = keyGen.generateKeyPair();
		return pair;
	}
}
