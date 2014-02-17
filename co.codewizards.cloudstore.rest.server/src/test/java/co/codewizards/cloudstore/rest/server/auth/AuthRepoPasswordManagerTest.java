package co.codewizards.cloudstore.rest.server.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AuthRepoPasswordManagerTest {
	private static final int PASSWORD_VALIDITIY_DURATION_MAX_MILLIS = 10000;
	private static final int PASSWORD_VALIDITIY_DURATION_MIN_MILLIS = 5000;
	private static final int REMOVE_EXPIRED_PASSWORDS_PERIOD_MILLIS = 1000;

	private AuthRepoPasswordManager authRepoPasswordManager;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty(AuthRepoPasswordManager.SYSTEM_PROPERTY_PASSWORD_VALIDITIY_PERIOD_MILLIS, Integer.toString(PASSWORD_VALIDITIY_DURATION_MAX_MILLIS));
		System.setProperty(AuthRepoPasswordManager.SYSTEM_PROPERTY_PASSWORD_RENEWAL_AFTER_MILLIS, Integer.toString(PASSWORD_VALIDITIY_DURATION_MIN_MILLIS));
		System.setProperty(AuthRepoPasswordManager.SYSTEM_PROPERTY_PASSWORD_EARLY_RENEWAL_PERIOD_MILLIS, Integer.toString(0));
		System.setProperty(AuthRepoPasswordManager.SYSTEM_PROPERTY_REMOVE_EXPIRED_PASSWORDS_PERIOD_MILLIS, Integer.toString(REMOVE_EXPIRED_PASSWORDS_PERIOD_MILLIS));
	}

	@AfterClass
	public static void afterClass() {
		System.getProperties().remove(AuthRepoPasswordManager.SYSTEM_PROPERTY_PASSWORD_VALIDITIY_PERIOD_MILLIS);
		System.getProperties().remove(AuthRepoPasswordManager.SYSTEM_PROPERTY_PASSWORD_RENEWAL_AFTER_MILLIS);
		System.getProperties().remove(AuthRepoPasswordManager.SYSTEM_PROPERTY_PASSWORD_EARLY_RENEWAL_PERIOD_MILLIS);
		System.getProperties().remove(AuthRepoPasswordManager.SYSTEM_PROPERTY_REMOVE_EXPIRED_PASSWORDS_PERIOD_MILLIS);
	}

	@Before
	public void before() {
		authRepoPasswordManager = new AuthRepoPasswordManager();
	}

	@Test
	public void getCurrentAuthRepoPasswordForDifferentRepos() {
		UUID serverRepositoryId1 = UUID.randomUUID();
		UUID clientRepositoryId1 = UUID.randomUUID();
		UUID serverRepositoryId2 = UUID.randomUUID();
		UUID clientRepositoryId2 = UUID.randomUUID();

		AuthRepoPassword authRepoPassword11a = authRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId1, clientRepositoryId1);
		AuthRepoPassword authRepoPassword11b = authRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId1, clientRepositoryId1);
		assertThat(authRepoPassword11a).isNotNull();
		assertThat(authRepoPassword11a.getPassword()).isNotNull();
		assertThat(authRepoPassword11b).isSameAs(authRepoPassword11a);

		AuthRepoPassword authRepoPassword12 = authRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId1, clientRepositoryId2);
		assertThat(authRepoPassword12).isNotNull();
		assertThat(authRepoPassword12).isNotSameAs(authRepoPassword11a);
		assertThat(authRepoPassword12.getPassword()).isNotNull().isNotEqualTo(authRepoPassword11a.getPassword());

		AuthRepoPassword authRepoPassword21 = authRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId2, clientRepositoryId1);
		assertThat(authRepoPassword21).isNotNull();
		assertThat(authRepoPassword21).isNotSameAs(authRepoPassword11a);
		assertThat(authRepoPassword21.getPassword()).isNotNull().isNotEqualTo(authRepoPassword11a.getPassword());
		assertThat(authRepoPassword21.getPassword()).isNotEqualTo(authRepoPassword12.getPassword());

		AuthRepoPassword authRepoPassword22 = authRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId2, clientRepositoryId2);
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
		AuthRepoPassword authRepoPassword = authRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId, clientRepositoryId);
		assertThat(authRepoPassword).isNotNull();
		assertThat(authRepoPassword.getPassword()).isNotNull();

		while (true) {
			AuthRepoPassword authRepoPassword2 = authRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId, clientRepositoryId);
			if (System.currentTimeMillis() > beginTimestamp + PASSWORD_VALIDITIY_DURATION_MIN_MILLIS) {
				// Fetch it again to make sure, we're REALLY after the time - it might have changed just while the if-clause was evaluated.
				authRepoPassword2 = authRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId, clientRepositoryId);
				assertThat(authRepoPassword2).isNotNull();
				assertThat(authRepoPassword2).isNotSameAs(authRepoPassword);
				assertThat(authRepoPassword2.getPassword()).isNotNull().isNotEqualTo(authRepoPassword.getPassword());
				break;
			}
			else {
				assertThat(authRepoPassword2).isSameAs(authRepoPassword);
			}
			Thread.sleep(500);
		}
	}

	@Test
	public void isValidOverTime() throws Exception {
		UUID serverRepositoryId = UUID.randomUUID();
		UUID clientRepositoryId = UUID.randomUUID();
		Set<AuthRepoPassword> authRepoPasswords = new HashSet<AuthRepoPassword>();

		long beginTimestamp = System.currentTimeMillis();
		long expectedLoopBeginTimestamp = beginTimestamp;
		int validCount = 0;
		int invalidCount = 0;
		while (System.currentTimeMillis() <= beginTimestamp + 33000) {
			{
				AuthRepoPassword authRepoPassword = authRepoPasswordManager.getCurrentAuthRepoPassword(serverRepositoryId, clientRepositoryId);
				assertThat(authRepoPassword).isNotNull();
				assertThat(authRepoPassword.getPassword()).isNotNull();
				authRepoPasswords.add(authRepoPassword);
			}

			validCount = 0;
			invalidCount = 0;
			for (AuthRepoPassword authRepoPassword : authRepoPasswords) {
				if (authRepoPasswordManager.isPasswordValid(serverRepositoryId, clientRepositoryId, authRepoPassword.getPassword()))
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
		assertThat(authRepoPasswords).hasSize(7);
		assertThat(validCount).isEqualTo(2);
		assertThat(invalidCount).isEqualTo(5);
	}

}
