package co.codewizards.cloudstore.rest.server.auth;

import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.UUID;

import co.codewizards.cloudstore.core.auth.AuthToken;
import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.util.PropertiesUtil;

public class AuthRepoPasswordManager {

	private static final int DEFAULT_PASSWORD_VALIDITIY_PERIOD_MILLIS = 60 * 60 * 1000;
	private static final int DEFAULT_PASSWORD_RENEWAL_AFTER_MILLIS = 30 * 60 * 1000;
	private static final int DEFAULT_PASSWORD_EARLY_RENEWAL_PERIOD_MILLIS = 15 * 60 * 1000;
	private static final int DEFAULT_REMOVE_EXPIRED_PASSWORDS_PERIOD_MILLIS = 60 * 1000;

	public static final String SYSTEM_PROPERTY_PASSWORD_VALIDITIY_PERIOD_MILLIS = "cloudstore.passwordValidityPeriodMillis";
	public static final String SYSTEM_PROPERTY_PASSWORD_RENEWAL_AFTER_MILLIS = "cloudstore.passwordRenewalAfterMillis";
	public static final String SYSTEM_PROPERTY_PASSWORD_EARLY_RENEWAL_PERIOD_MILLIS = "cloudstore.passwordEarlyRenewalPeriodMillis";
	public static final String SYSTEM_PROPERTY_REMOVE_EXPIRED_PASSWORDS_PERIOD_MILLIS = "cloudstore.removeExpiredPasswordsPeriodMillis";

	private int passwordValidityPeriodMillis = Integer.MIN_VALUE;
	private int passwordRenewalAfterMillis = Integer.MIN_VALUE;
	private int passwordEarlyRenewalPeriodMillis = Integer.MIN_VALUE;
	private int removeExpiredPasswordsPeriodMillis = Integer.MIN_VALUE;

	private static class AuthRepoPasswordManagerHolder {
		public static final AuthRepoPasswordManager instance = new AuthRepoPasswordManager();
	}

	protected AuthRepoPasswordManager() { }

	public static AuthRepoPasswordManager getInstance() {
		return AuthRepoPasswordManagerHolder.instance;
	}

	private final Map<UUID, Map<UUID, SortedSet<AuthRepoPassword>>> serverRepositoryId2ClientRepositoryId2AuthRepoPasswordSet = new HashMap<UUID, Map<UUID,SortedSet<AuthRepoPassword>>>();
	private final SortedSet<AuthRepoPassword> authRepoPasswords = new TreeSet<AuthRepoPassword>(newestFirstAuthRepoPasswordComparator);

	private final Timer timer = new Timer();
	private final TimerTask removeExpiredAuthRepoPasswordsTimerTask = new TimerTask() {
		@Override
		public void run() {
			removeExpiredAuthRepoPasswords();
		}
	};
	{
		timer.schedule(removeExpiredAuthRepoPasswordsTimerTask, getRemoveExpiredPasswordsPeriodMillis(), getRemoveExpiredPasswordsPeriodMillis());
	}

	public synchronized AuthRepoPassword getCurrentAuthRepoPassword(UUID serverRepositoryId, UUID clientRepositoryId) {
		assertNotNull("serverRepositoryId", serverRepositoryId);
		assertNotNull("clientRepositoryId", clientRepositoryId);

		Map<UUID, SortedSet<AuthRepoPassword>> clientRepositoryId2AuthRepoPasswordSet = serverRepositoryId2ClientRepositoryId2AuthRepoPasswordSet.get(serverRepositoryId);
		if (clientRepositoryId2AuthRepoPasswordSet == null) {
			clientRepositoryId2AuthRepoPasswordSet = new HashMap<UUID, SortedSet<AuthRepoPassword>>();
			serverRepositoryId2ClientRepositoryId2AuthRepoPasswordSet.put(serverRepositoryId, clientRepositoryId2AuthRepoPasswordSet);
		}

		SortedSet<AuthRepoPassword> authRepoPasswordSet = clientRepositoryId2AuthRepoPasswordSet.get(clientRepositoryId);
		if (authRepoPasswordSet == null) {
			authRepoPasswordSet = new TreeSet<AuthRepoPassword>(newestFirstAuthRepoPasswordComparator);
			clientRepositoryId2AuthRepoPasswordSet.put(clientRepositoryId, authRepoPasswordSet);
		}

		AuthRepoPassword authRepoPassword = authRepoPasswordSet.isEmpty() ? null : authRepoPasswordSet.first();
		if (authRepoPassword != null && isAfterRenewalDateOrInEarlyRenewalPeriod(authRepoPassword))
			authRepoPassword = null;

		if (authRepoPassword == null) {
			authRepoPassword = new AuthRepoPassword(serverRepositoryId, clientRepositoryId, createAuthToken());
			authRepoPasswordSet.add(authRepoPassword);
			authRepoPasswords.add(authRepoPassword);
		}
		return authRepoPassword;
	}

	public synchronized boolean isPasswordValid(UUID serverRepositoryId, UUID clientRepositoryId, char[] password) {
		assertNotNull("serverRepositoryId", serverRepositoryId);
		assertNotNull("clientRepositoryId", clientRepositoryId);
		assertNotNull("password", password);
		Map<UUID, SortedSet<AuthRepoPassword>> clientRepositoryId2AuthRepoPasswordSet = serverRepositoryId2ClientRepositoryId2AuthRepoPasswordSet.get(serverRepositoryId);
		if (clientRepositoryId2AuthRepoPasswordSet == null)
			return false;

		SortedSet<AuthRepoPassword> authRepoPasswordSet = clientRepositoryId2AuthRepoPasswordSet.get(clientRepositoryId);
		if (authRepoPasswordSet == null)
			return false;

		for (AuthRepoPassword authRepoPassword : authRepoPasswordSet) {
			if (isExpired(authRepoPassword)) // newest first => first expired means all following expired, too!
				return false;

			if (Arrays.equals(password, authRepoPassword.getPassword()))
				return true;
		}
		return false;
	}

	private synchronized void removeExpiredAuthRepoPasswords() {
		while (!authRepoPasswords.isEmpty()) {
			AuthRepoPassword oldestAuthRepoPassword = authRepoPasswords.last();
			if (!isExpired(oldestAuthRepoPassword)) // newest first => last not yet expired means all previous not yet expired, either
				break;

			authRepoPasswords.remove(oldestAuthRepoPassword);
			UUID serverRepositoryId = oldestAuthRepoPassword.getServerRepositoryId();
			UUID clientRepositoryId = oldestAuthRepoPassword.getClientRepositoryId();

			Map<UUID, SortedSet<AuthRepoPassword>> clientRepositoryId2AuthRepoPasswordSet = serverRepositoryId2ClientRepositoryId2AuthRepoPasswordSet.get(serverRepositoryId);
			assertNotNull("clientRepositoryId2AuthRepoPasswordSet", clientRepositoryId2AuthRepoPasswordSet);

			SortedSet<AuthRepoPassword> authRepoPasswordSet = clientRepositoryId2AuthRepoPasswordSet.get(clientRepositoryId);
			assertNotNull("authRepoPasswordSet", authRepoPasswordSet);

			authRepoPasswordSet.remove(oldestAuthRepoPassword);

			if (authRepoPasswordSet.isEmpty())
				clientRepositoryId2AuthRepoPasswordSet.remove(clientRepositoryId);

			if (clientRepositoryId2AuthRepoPasswordSet.isEmpty())
				serverRepositoryId2ClientRepositoryId2AuthRepoPasswordSet.remove(serverRepositoryId);
		}
	}

	protected int getPasswordValidityPeriodMillis() {
		if (passwordValidityPeriodMillis == Integer.MIN_VALUE) {
			passwordValidityPeriodMillis = PropertiesUtil.getSystemPropertyValueAsInt(
					SYSTEM_PROPERTY_PASSWORD_VALIDITIY_PERIOD_MILLIS, DEFAULT_PASSWORD_VALIDITIY_PERIOD_MILLIS);
		}
		return passwordValidityPeriodMillis;
	}

	protected int getPasswordRenewalAfterMillis() {
		if (passwordRenewalAfterMillis == Integer.MIN_VALUE) {
			passwordRenewalAfterMillis = PropertiesUtil.getSystemPropertyValueAsInt(
					SYSTEM_PROPERTY_PASSWORD_RENEWAL_AFTER_MILLIS, DEFAULT_PASSWORD_RENEWAL_AFTER_MILLIS);
		}
		return passwordRenewalAfterMillis;
	}

	protected int getPasswordEarlyRenewalPeriodMillis() {
		if (passwordEarlyRenewalPeriodMillis == Integer.MIN_VALUE) {
			passwordEarlyRenewalPeriodMillis = PropertiesUtil.getSystemPropertyValueAsInt(
					SYSTEM_PROPERTY_PASSWORD_EARLY_RENEWAL_PERIOD_MILLIS, DEFAULT_PASSWORD_EARLY_RENEWAL_PERIOD_MILLIS);
		}
		return passwordEarlyRenewalPeriodMillis;
	}

	protected int getRemoveExpiredPasswordsPeriodMillis() {
		if (removeExpiredPasswordsPeriodMillis == Integer.MIN_VALUE) {
			removeExpiredPasswordsPeriodMillis = PropertiesUtil.getSystemPropertyValueAsInt(
					SYSTEM_PROPERTY_REMOVE_EXPIRED_PASSWORDS_PERIOD_MILLIS, DEFAULT_REMOVE_EXPIRED_PASSWORDS_PERIOD_MILLIS);
		}
		return removeExpiredPasswordsPeriodMillis;
	}

	private static final Comparator<AuthRepoPassword> newestFirstAuthRepoPasswordComparator = new Comparator<AuthRepoPassword>() {
		@Override
		public int compare(AuthRepoPassword o1, AuthRepoPassword o2) {
			Date expiryDate1 = o1.getAuthToken().getExpiryDateTime().toDate();
			Date expiryDate2 = o2.getAuthToken().getExpiryDateTime().toDate();

			if (expiryDate1.before(expiryDate2))
				return +1;

			if (expiryDate1.after(expiryDate2))
				return -1;

			int result = o1.getServerRepositoryId().compareTo(o2.getServerRepositoryId());
			if (result != 0)
				return result;

			result = o1.getClientRepositoryId().compareTo(o2.getClientRepositoryId());
			return result;
		}
	};

	private boolean isAfterRenewalDateOrInEarlyRenewalPeriod(AuthRepoPassword authRepoPassword) {
		assertNotNull("authRepoPassword", authRepoPassword);
		return System.currentTimeMillis() + getPasswordEarlyRenewalPeriodMillis() > authRepoPassword.getAuthToken().getRenewalDateTime().getMillis();
	}

	private boolean isExpired(AuthRepoPassword authRepoPassword) {
		assertNotNull("authRepoPassword", authRepoPassword);
		return System.currentTimeMillis() > authRepoPassword.getAuthToken().getExpiryDateTime().getMillis();
	}

	private AuthToken createAuthToken() {
		AuthToken authToken = new AuthToken();
		Date expiryDate = new Date(System.currentTimeMillis() + getPasswordValidityPeriodMillis());
		Date renewalDate = new Date(System.currentTimeMillis() + getPasswordRenewalAfterMillis());
		authToken.setExpiryDateTime(new DateTime(expiryDate));
		authToken.setRenewalDateTime(new DateTime(renewalDate));
		authToken.setPassword(new String(PasswordUtil.createRandomPassword(40)));
		authToken.makeUnmodifiable();
		return authToken;
	}

}
