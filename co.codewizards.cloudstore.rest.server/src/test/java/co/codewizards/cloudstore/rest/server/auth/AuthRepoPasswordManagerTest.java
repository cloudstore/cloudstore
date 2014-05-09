package co.codewizards.cloudstore.rest.server.auth;

import static org.assertj.core.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.core.config.Config;

public class AuthRepoPasswordManagerTest {
	private static final int PASSWORD_VALIDITIY_DURATION_MAX_MILLIS = 10000;
	private static final int PASSWORD_VALIDITIY_DURATION_MIN_MILLIS = 5000;
	private static final int REMOVE_EXPIRED_PASSWORDS_PERIOD_MILLIS = 1000;

	private TransientRepoPasswordManager transientRepoPasswordManager;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + TransientRepoPasswordManager.CONFIG_KEY_VALIDITIY_PERIOD, Integer.toString(PASSWORD_VALIDITIY_DURATION_MAX_MILLIS));
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + TransientRepoPasswordManager.CONFIG_KEY_RENEWAL_PERIOD, Integer.toString(PASSWORD_VALIDITIY_DURATION_MIN_MILLIS));
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + TransientRepoPasswordManager.CONFIG_KEY_EARLY_RENEWAL_PERIOD, Integer.toString(0));
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + TransientRepoPasswordManager.CONFIG_KEY_EXPIRY_TIMER_PERIOD, Integer.toString(REMOVE_EXPIRED_PASSWORDS_PERIOD_MILLIS));
	}

	@AfterClass
	public static void afterClass() {
		System.getProperties().remove(Config.SYSTEM_PROPERTY_PREFIX + TransientRepoPasswordManager.CONFIG_KEY_VALIDITIY_PERIOD);
		System.getProperties().remove(Config.SYSTEM_PROPERTY_PREFIX + TransientRepoPasswordManager.CONFIG_KEY_RENEWAL_PERIOD);
		System.getProperties().remove(Config.SYSTEM_PROPERTY_PREFIX + TransientRepoPasswordManager.CONFIG_KEY_EARLY_RENEWAL_PERIOD);
		System.getProperties().remove(Config.SYSTEM_PROPERTY_PREFIX + TransientRepoPasswordManager.CONFIG_KEY_EXPIRY_TIMER_PERIOD);
	}

	@Before
	public void before() {
		transientRepoPasswordManager = new TransientRepoPasswordManager();
	}

	@Test
	public void getCurrentAuthRepoPasswordForDifferentRepos() {
		UUID serverRepositoryId1 = UUID.randomUUID();
		UUID clientRepositoryId1 = UUID.randomUUID();
		UUID serverRepositoryId2 = UUID.randomUUID();
		UUID clientRepositoryId2 = UUID.randomUUID();

		TransientRepoPassword authRepoPassword11a = transientRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId1, clientRepositoryId1);
		TransientRepoPassword authRepoPassword11b = transientRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId1, clientRepositoryId1);
		assertThat(authRepoPassword11a).isNotNull();
		assertThat(authRepoPassword11a.getPassword()).isNotNull();
		assertThat(authRepoPassword11b).isSameAs(authRepoPassword11a);

		TransientRepoPassword authRepoPassword12 = transientRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId1, clientRepositoryId2);
		assertThat(authRepoPassword12).isNotNull();
		assertThat(authRepoPassword12).isNotSameAs(authRepoPassword11a);
		assertThat(authRepoPassword12.getPassword()).isNotNull().isNotEqualTo(authRepoPassword11a.getPassword());

		TransientRepoPassword authRepoPassword21 = transientRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId2, clientRepositoryId1);
		assertThat(authRepoPassword21).isNotNull();
		assertThat(authRepoPassword21).isNotSameAs(authRepoPassword11a);
		assertThat(authRepoPassword21.getPassword()).isNotNull().isNotEqualTo(authRepoPassword11a.getPassword());
		assertThat(authRepoPassword21.getPassword()).isNotEqualTo(authRepoPassword12.getPassword());

		TransientRepoPassword authRepoPassword22 = transientRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId2, clientRepositoryId2);
		assertThat(authRepoPassword22).isNotNull();
		assertThat(authRepoPassword22).isNotSameAs(authRepoPassword11a);
		assertThat(authRepoPassword22.getPassword()).isNotNull().isNotEqualTo(authRepoPassword11a.getPassword());
		assertThat(authRepoPassword22.getPassword()).isNotEqualTo(authRepoPassword12.getPassword());
		assertThat(authRepoPassword22.getPassword()).isNotEqualTo(authRepoPassword21.getPassword());
	}

	@Test
	public void getCurrentAuthRepoPasswordForSameReposOverTime() throws Exception {
		UUID serverRepositoryId = UUID.randomUUID();
		UUID clientRepositoryId = UUID.randomUUID();
		long beginTimestamp = System.currentTimeMillis();
		TransientRepoPassword transientRepoPassword = transientRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId, clientRepositoryId);
		assertThat(transientRepoPassword).isNotNull();
		assertThat(transientRepoPassword.getPassword()).isNotNull();

		while (true) {
			TransientRepoPassword authRepoPassword2 = transientRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId, clientRepositoryId);
			if (System.currentTimeMillis() > beginTimestamp + PASSWORD_VALIDITIY_DURATION_MIN_MILLIS) {
				// Fetch it again to make sure, we're REALLY after the time - it might have changed just while the if-clause was evaluated.
				authRepoPassword2 = transientRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId, clientRepositoryId);
				assertThat(authRepoPassword2).isNotNull();
				assertThat(authRepoPassword2).isNotSameAs(transientRepoPassword);
				assertThat(authRepoPassword2.getPassword()).isNotNull().isNotEqualTo(transientRepoPassword.getPassword());
				break;
			}
			else {
				assertThat(authRepoPassword2).isSameAs(transientRepoPassword);
			}
			Thread.sleep(500);
		}
	}

	@Test
	public void isValidOverTime() throws Exception {
		UUID serverRepositoryId = UUID.randomUUID();
		UUID clientRepositoryId = UUID.randomUUID();
		Set<TransientRepoPassword> transientRepoPasswords = new HashSet<TransientRepoPassword>();

		long beginTimestamp = System.currentTimeMillis();
		long expectedLoopBeginTimestamp = beginTimestamp;
		int validCount = 0;
		int invalidCount = 0;
		while (System.currentTimeMillis() <= beginTimestamp + 33000) {
			{
				TransientRepoPassword transientRepoPassword = transientRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId, clientRepositoryId);
				assertThat(transientRepoPassword).isNotNull();
				assertThat(transientRepoPassword.getPassword()).isNotNull();
				transientRepoPasswords.add(transientRepoPassword);
			}

			validCount = 0;
			invalidCount = 0;
			for (TransientRepoPassword transientRepoPassword : transientRepoPasswords) {
				if (transientRepoPasswordManager.isPasswordValid(serverRepositoryId, clientRepositoryId, transientRepoPassword.getPassword()))
					++validCount;
				else
					++invalidCount;
			}

			if (System.currentTimeMillis() > beginTimestamp + PASSWORD_VALIDITIY_DURATION_MAX_MILLIS + 300) // + 300 ms reserve
				assertThat(invalidCount).isGreaterThanOrEqualTo(1);

			assertThat(validCount).isGreaterThanOrEqualTo(1).isLessThanOrEqualTo(2);

			expectedLoopBeginTimestamp += 505; // 5 ms reserve
			long difference = expectedLoopBeginTimestamp - System.currentTimeMillis();
			if (difference > 0)
				Thread.sleep(difference);

			System.out.println("difference=" + difference + " now=" + System.currentTimeMillis());
		}
		assertThat(transientRepoPasswords).hasSize(7);
		assertThat(validCount).isEqualTo(2);
		assertThat(invalidCount).isEqualTo(5);
	}

}
