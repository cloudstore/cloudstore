package co.codewizards.cloudstore.core.otp;

import java.security.SecureRandom;

/**
 * Simple implementation of one-time pad encryption technique.
 *
 * @author Wojtek Wilk - wilk.wojtek at gmail.com
 */
public class OneTimePadEncryptor {

	private final SecureRandom random = new SecureRandom();

	public Result encrypt(byte[] message){
		byte[] randomKey = new byte[message.length];
		random.nextBytes(randomKey);

		byte[] encrypted = new byte[message.length];
		for(int i=0; i<message.length; i++){
			encrypted[i] = (byte) (message[i] ^ randomKey[i]);
		}
		return new Result(randomKey, encrypted);
	}

	public byte[] decrypt(byte[] encryptedMessage, byte[] randomKey){
		if(encryptedMessage.length != randomKey.length){
			throw new IllegalArgumentException("message and key have to be of the same length");
		}
		byte[] decodedMessage = new byte[encryptedMessage.length];

		for(int i=0;i<encryptedMessage.length; i++){
			decodedMessage[i] = (byte) (encryptedMessage[i] ^ randomKey[i]);
		}
		return decodedMessage;
	}

	public static class Result{
		private final byte[] randomKey;
		private final byte[] encryptedMessage;

		public Result(byte[] randomKey, byte[] encryptedMessage){
			this.randomKey = randomKey;
			this.encryptedMessage = encryptedMessage;
		}

		public byte[] getRandomKey() {
			return randomKey;
		}

		public byte[] getEncryptedMessage() {
			return encryptedMessage;
		}
	}
}
