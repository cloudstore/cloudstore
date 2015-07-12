package co.codewizards.cloudstore.core.otp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

import co.codewizards.cloudstore.core.otp.OneTimePadEncryptor.Result;

public class OneTimePadEncryptorTest {

	private OneTimePadEncryptor encryptor;

	@Before
	public void setUp(){
		encryptor = new OneTimePadEncryptor();
	}

	@Test
	public void when_message_is_encrypted_then_it_can_be_properly_decrypted_with_the_same_key(){
		String message = "message";
		byte[] messageBytes = message.getBytes(Charset.forName("UTF-8"));

		Result result = encryptor.encrypt(messageBytes);

		byte[] decryptedBytes = encryptor.decrypt(result.getEncryptedMessage(), result.getRandomKey());
		String decryptedMessage = new String(decryptedBytes, Charset.forName("UTF-8"));

		assertThat(decryptedMessage).isEqualTo(message);
		assertThat(decryptedBytes).isEqualTo(messageBytes).isNotEqualTo(result.getEncryptedMessage());
	}

	@Test(expected = IllegalArgumentException.class)
	public void when_decrypting_and_encrypted_message_and_key_have_different_length_then_throw_IAE(){
		encryptor.decrypt(new byte[3], new byte[2]);
	}

}
