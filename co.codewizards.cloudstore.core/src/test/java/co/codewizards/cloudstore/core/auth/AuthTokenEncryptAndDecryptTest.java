package co.codewizards.cloudstore.core.auth;

import static org.assertj.core.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.junit.Test;

public class AuthTokenEncryptAndDecryptTest {
	private static SecureRandom random = new SecureRandom();

	@Test
	public void encryptAndDecrypt() throws Exception {
		KeyPair keyPairSender = createKeyPair();
		KeyPair keyPairReceiver = createKeyPair();


		// On sender's side:

		AuthToken authToken1 = AuthTokenIOTest.createAuthToken();
		byte[] authTokenData1 = new AuthTokenIO().serialise(authToken1);

		SignedAuthToken signedAuthToken1 = new AuthTokenSigner(keyPairSender.getPrivate().getEncoded()).sign(authTokenData1);
		assertThat(signedAuthToken1).isNotNull();
		assertThat(signedAuthToken1.getAuthTokenData()).isNotNull();
		assertThat(signedAuthToken1.getSignature()).isNotNull();

		byte[] signedAuthTokenData1 = new SignedAuthTokenIO().serialise(signedAuthToken1);

		EncryptedSignedAuthToken encryptedSignedAuthToken =
				new SignedAuthTokenEncrypter(keyPairReceiver.getPublic().getEncoded()).encrypt(signedAuthTokenData1);

		assertThat(encryptedSignedAuthToken).isNotNull();
		assertThat(encryptedSignedAuthToken.getEncryptedSignedAuthTokenData()).isNotNull();
		assertThat(encryptedSignedAuthToken.getEncryptedSymmetricKey()).isNotNull();


		// On receiver's side:
		byte[] signedAuthTokenData2 =
				new SignedAuthTokenDecrypter(keyPairReceiver.getPrivate().getEncoded()).decrypt(encryptedSignedAuthToken);

		assertThat(signedAuthTokenData2).isEqualTo(signedAuthTokenData1);

		SignedAuthToken signedAuthToken2 = new SignedAuthTokenIO().deserialise(signedAuthTokenData2);
		assertThat(signedAuthToken2).isNotNull();
		assertThat(signedAuthToken2.getAuthTokenData()).isNotNull().isEqualTo(signedAuthToken1.getAuthTokenData());
		assertThat(signedAuthToken2.getSignature()).isNotNull().isEqualTo(signedAuthToken1.getSignature());

		AuthTokenVerifier verifier = new AuthTokenVerifier(keyPairSender.getPublic().getEncoded());
		verifier.verify(signedAuthToken2);
	}

	private KeyPair createKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(4096, random);
		KeyPair pair = keyGen.generateKeyPair();
		return pair;
	}
}
