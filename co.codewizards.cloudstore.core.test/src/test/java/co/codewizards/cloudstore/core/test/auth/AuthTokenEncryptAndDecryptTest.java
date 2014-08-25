package co.codewizards.cloudstore.core.test.auth;

import static java.lang.System.*;
import static org.assertj.core.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.auth.AuthToken;
import co.codewizards.cloudstore.core.auth.AuthTokenIO;
import co.codewizards.cloudstore.core.auth.AuthTokenSigner;
import co.codewizards.cloudstore.core.auth.AuthTokenVerifier;
import co.codewizards.cloudstore.core.auth.EncryptedSignedAuthToken;
import co.codewizards.cloudstore.core.auth.SignedAuthToken;
import co.codewizards.cloudstore.core.auth.SignedAuthTokenDecrypter;
import co.codewizards.cloudstore.core.auth.SignedAuthTokenEncrypter;
import co.codewizards.cloudstore.core.auth.SignedAuthTokenIO;

public class AuthTokenEncryptAndDecryptTest {
	private static final Logger logger = LoggerFactory.getLogger(AuthTokenEncryptAndDecryptTest.class);
	private static SecureRandom random = new SecureRandom();

	{
		logger.debug("[{}]<init>", Integer.toHexString(identityHashCode(this)));
	}

	@Test
	public void encryptAndDecrypt() throws Exception {
		logger.debug("[{}]encryptAndDecrypt: entered.", Integer.toHexString(identityHashCode(this)));
		final KeyPair keyPairSender = createKeyPair();
		final KeyPair keyPairReceiver = createKeyPair();


		// On sender's side:
		final AuthToken authToken1 = AuthTokenIOTest.createAuthToken();
		final byte[] authTokenData1 = new AuthTokenIO().serialise(authToken1);

		final SignedAuthToken signedAuthToken1 = new AuthTokenSigner(keyPairSender.getPrivate().getEncoded()).sign(authTokenData1);
		assertThat(signedAuthToken1).isNotNull();
		assertThat(signedAuthToken1.getAuthTokenData()).isNotNull();
		assertThat(signedAuthToken1.getSignature()).isNotNull();

		final byte[] signedAuthTokenData1 = new SignedAuthTokenIO().serialise(signedAuthToken1);

		final EncryptedSignedAuthToken encryptedSignedAuthToken =
				new SignedAuthTokenEncrypter(keyPairReceiver.getPublic().getEncoded()).encrypt(signedAuthTokenData1);

		assertThat(encryptedSignedAuthToken).isNotNull();
		assertThat(encryptedSignedAuthToken.getEncryptedSignedAuthTokenData()).isNotNull();
		assertThat(encryptedSignedAuthToken.getEncryptedSymmetricKey()).isNotNull();


		// On receiver's side:
		final byte[] signedAuthTokenData2 =
				new SignedAuthTokenDecrypter(keyPairReceiver.getPrivate().getEncoded()).decrypt(encryptedSignedAuthToken);

		assertThat(signedAuthTokenData2).isEqualTo(signedAuthTokenData1);

		final SignedAuthToken signedAuthToken2 = new SignedAuthTokenIO().deserialise(signedAuthTokenData2);
		assertThat(signedAuthToken2).isNotNull();
		assertThat(signedAuthToken2.getAuthTokenData()).isNotNull().isEqualTo(signedAuthToken1.getAuthTokenData());
		assertThat(signedAuthToken2.getSignature()).isNotNull().isEqualTo(signedAuthToken1.getSignature());

		final AuthTokenVerifier verifier = new AuthTokenVerifier(keyPairSender.getPublic().getEncoded());
		verifier.verify(signedAuthToken2);
	}

	private KeyPair createKeyPair() throws NoSuchAlgorithmException {
		final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(1024, random); // Productively, we should always use 4096 by default! But for testing, this is fine and much faster.
		final KeyPair pair = keyGen.generateKeyPair();
		return pair;
	}
}
