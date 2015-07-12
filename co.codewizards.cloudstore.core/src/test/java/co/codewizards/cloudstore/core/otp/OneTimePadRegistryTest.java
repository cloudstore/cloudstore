package co.codewizards.cloudstore.core.otp;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.createFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.util.IOUtil;

public class OneTimePadRegistryTest {

	private String fileName = UUID.randomUUID().toString();

	private OneTimePadRegistry registry;

	@Before
	public void setUp(){
		registry = new OneTimePadRegistry(fileName);
	}

	@After
	public void tearDown(){
		IOUtil.deleteOrFail(createFile(ConfigDir.getInstance().getFile(), fileName + OneTimePadRegistry.PASSWORD_FILE_SUFFIX));
		IOUtil.deleteOrFail(createFile(ConfigDir.getInstance().getFile(), fileName + OneTimePadRegistry.RANDOM_KEY_FILE_SUFFIX));
	}

	@Test
	public void when_there_is_no_password_then_store_it_properly(){
		String password = "testPassword1234*_+&";
		registry.encryptAndStorePassword(password.toCharArray());
		char[] result = registry.readFromFileAndDecrypt();
		assertThat(new String(result)).isEqualTo(password);
	}

	@Test
	public void when_there_is_already_stored_password_then_replace_it(){
		String password = "testPassword1234*_+&";
		registry.encryptAndStorePassword(password.toCharArray());
		char[] result = registry.readFromFileAndDecrypt();

		assertThat(new String(result)).isEqualTo(password);

		String differentPassword = "differentPassword0988./$";
		registry.encryptAndStorePassword(differentPassword.toCharArray());
		char[] secondResult = registry.readFromFileAndDecrypt();

		assertThat(new String(secondResult)).isEqualTo(differentPassword);
	}
}
