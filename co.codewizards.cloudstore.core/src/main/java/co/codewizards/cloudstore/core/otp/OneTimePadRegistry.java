package co.codewizards.cloudstore.core.otp;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.createFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.otp.OneTimePadEncryptor.Result;
import co.codewizards.cloudstore.core.util.IOUtil;

/**
 * Registry for passwords that need to be encrypted with OTP technique.
 * You can, using methods of this class, encrypt with OTP and store a password
 *  in a file in ConfigDir, and later retrieve from the file and decrypt it.
 * Encrypted password is stored in a file named fileNamePrefix + PASSWORD_FILE_SUFFIX
 * Random key in a file named fileNamePrefix + RANDOM_KEY_FILE_SUFFIX
 *
 * @author Wojtek Wilk - wilk.wojtek at gmail.com
 */
public class OneTimePadRegistry {

	public static final String PASSWORD_FILE_SUFFIX = "Password";
	public static final String RANDOM_KEY_FILE_SUFFIX = "RandomKey";

	private final String fileNamePrefix;
	private final OneTimePadEncryptor encryptor = new OneTimePadEncryptor();

	public OneTimePadRegistry(final String fileNamePrefix){
		this.fileNamePrefix = fileNamePrefix;
	}

	public void encryptAndStorePassword(final char[] password){
		final Result result = encryptor.encrypt(toBytes(password));
		try {
			writeToFile(result.getEncryptedMessage(), PASSWORD_FILE_SUFFIX);
			writeToFile(result.getRandomKey(), RANDOM_KEY_FILE_SUFFIX);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public char[] readFromFileAndDecrypt(){
		try {
			final byte[] encryptedPassword = readFromFile(PASSWORD_FILE_SUFFIX);
			final byte[] randomKey = readFromFile(RANDOM_KEY_FILE_SUFFIX);
			final byte[] decryptedPassword = encryptor.decrypt(encryptedPassword, randomKey);
			return toChars(decryptedPassword);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private byte[] readFromFile(String fileNameSuffix) throws IOException {
		final String fileName = fileNamePrefix + fileNameSuffix;
		final File file =  createFile(ConfigDir.getInstance().getFile(), fileName);
		return IOUtil.getBytesFromFile(file);
	}

	private void writeToFile(byte[] bytes, String fileNameSuffix) throws IOException{
		final File file = createFile(ConfigDir.getInstance().getFile(), fileNamePrefix + fileNameSuffix);
		try(final OutputStream os = file.createOutputStream()){
			os.write(bytes);
		}
	}

	/**
	 * based on @link <a href="http://stackoverflow.com/questions/5513144/converting-char-to-byte#answer-9670279">this answer</a>
	 */
	private byte[] toBytes(final char[] chars) {
	    final CharBuffer charBuffer = CharBuffer.wrap(chars);
	    final ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
	    final byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
	            byteBuffer.position(), byteBuffer.limit());
	    clearSensitiveData(charBuffer.array(), byteBuffer.array());
	    return bytes;
	}

	private char[] toChars(final byte[] bytes){
		final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		final CharBuffer charBuffer = StandardCharsets.UTF_8.decode(byteBuffer);
		final char[] chars = Arrays.copyOfRange(charBuffer.array(),
				charBuffer.position(), charBuffer.limit());
		clearSensitiveData(charBuffer.array(), byteBuffer.array());
	    return chars;
	}

	private void clearSensitiveData(final char[] chars, final byte[] bytes){
		Arrays.fill(chars, '\u0000');
		Arrays.fill(bytes, (byte) 0);
	}
}
