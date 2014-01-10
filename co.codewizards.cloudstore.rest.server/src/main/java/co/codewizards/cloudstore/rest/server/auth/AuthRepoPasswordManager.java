package co.codewizards.cloudstore.rest.server.auth;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import co.codewizards.cloudstore.core.auth.AuthToken;
import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.dto.EntityID;

public class AuthRepoPasswordManager {

	private static final int DEFAULT_PASSWORD_VALIDITIY_DURATION_MAX_MILLIS = 60 * 60 * 1000;
	private static final int DEFAULT_PASSWORD_VALIDITIY_DURATION_MIN_MILLIS = 10 * 60 * 1000;
	private static final int DEFAULT_REMOVE_EXPIRED_PASSWORDS_PERIOD_MILLIS = 60 * 1000;

	private int passwordValidityDurationMaxMillis = Integer.MIN_VALUE;
	private int passwordValidityDurationMinMillis = Integer.MIN_VALUE;
	private int removeExpiredPasswordsPeriodMillis = Integer.MIN_VALUE;

	private static class AuthRepoPasswordManagerHolder {
		public static final AuthRepoPasswordManager instance = new AuthRepoPasswordManager();
	}

	protected AuthRepoPasswordManager() { }

	public static AuthRepoPasswordManager getInstance() {
		return AuthRepoPasswordManagerHolder.instance;
	}

	private final Map<EntityID, Map<EntityID, SortedSet<AuthRepoPassword>>> serverRepositoryID2ClientRepositoryID2AuthRepoPasswordSet = new HashMap<EntityID, Map<EntityID,SortedSet<AuthRepoPassword>>>();
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

	public synchronized AuthRepoPassword getCurrentAuthRepoPassword(EntityID serverRepositoryID, EntityID clientRepositoryID) {
		assertNotNull("serverRepositoryID", serverRepositoryID);
		assertNotNull("clientRepositoryID", clientRepositoryID);

		Map<EntityID, SortedSet<AuthRepoPassword>> clientRepositoryID2AuthRepoPasswordSet = serverRepositoryID2ClientRepositoryID2AuthRepoPasswordSet.get(serverRepositoryID);
		if (clientRepositoryID2AuthRepoPasswordSet == null) {
			clientRepositoryID2AuthRepoPasswordSet = new HashMap<EntityID, SortedSet<AuthRepoPassword>>();
			serverRepositoryID2ClientRepositoryID2AuthRepoPasswordSet.put(serverRepositoryID, clientRepositoryID2AuthRepoPasswordSet);
		}

		SortedSet<AuthRepoPassword> authRepoPasswordSet = clientRepositoryID2AuthRepoPasswordSet.get(clientRepositoryID);
		if (authRepoPasswordSet == null) {
			authRepoPasswordSet = new TreeSet<AuthRepoPassword>(newestFirstAuthRepoPasswordComparator);
			clientRepositoryID2AuthRepoPasswordSet.put(clientRepositoryID, authRepoPasswordSet);
		}

		AuthRepoPassword authRepoPassword = authRepoPasswordSet.isEmpty() ? null : authRepoPasswordSet.first();
		if (authRepoPassword != null && isAfterRenewalDate(authRepoPassword))
			authRepoPassword = null;

		if (authRepoPassword == null) {
			authRepoPassword = new AuthRepoPassword(serverRepositoryID, clientRepositoryID, createAuthToken());
			authRepoPasswordSet.add(authRepoPassword);
			authRepoPasswords.add(authRepoPassword);
		}
		return authRepoPassword;
	}

	public synchronized boolean isPasswordValid(EntityID serverRepositoryID, EntityID clientRepositoryID, char[] password) {
		assertNotNull("serverRepositoryID", serverRepositoryID);
		assertNotNull("clientRepositoryID", clientRepositoryID);
		assertNotNull("password", password);
		Map<EntityID, SortedSet<AuthRepoPassword>> clientRepositoryID2AuthRepoPasswordSet = serverRepositoryID2ClientRepositoryID2AuthRepoPasswordSet.get(serverRepositoryID);
		if (clientRepositoryID2AuthRepoPasswordSet == null)
			return false;

		SortedSet<AuthRepoPassword> authRepoPasswordSet = clientRepositoryID2AuthRepoPasswordSet.get(clientRepositoryID);
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
			EntityID serverRepositoryID = oldestAuthRepoPassword.getServerRepositoryID();
			EntityID clientRepositoryID = oldestAuthRepoPassword.getClientRepositoryID();

			Map<EntityID, SortedSet<AuthRepoPassword>> clientRepositoryID2AuthRepoPasswordSet = serverRepositoryID2ClientRepositoryID2AuthRepoPasswordSet.get(serverRepositoryID);
			assertNotNull("clientRepositoryID2AuthRepoPasswordSet", clientRepositoryID2AuthRepoPasswordSet);

			SortedSet<AuthRepoPassword> authRepoPasswordSet = clientRepositoryID2AuthRepoPasswordSet.get(clientRepositoryID);
			assertNotNull("authRepoPasswordSet", authRepoPasswordSet);

			authRepoPasswordSet.remove(oldestAuthRepoPassword);

			if (authRepoPasswordSet.isEmpty())
				clientRepositoryID2AuthRepoPasswordSet.remove(clientRepositoryID);

			if (clientRepositoryID2AuthRepoPasswordSet.isEmpty())
				serverRepositoryID2ClientRepositoryID2AuthRepoPasswordSet.remove(serverRepositoryID);
		}
	}

	protected int getPasswordValidityDurationMaxMillis() {
		if (passwordValidityDurationMaxMillis == Integer.MIN_VALUE) {
			// TODO system property!
			passwordValidityDurationMaxMillis = DEFAULT_PASSWORD_VALIDITIY_DURATION_MAX_MILLIS;
		}
		return passwordValidityDurationMaxMillis;
	}

	protected int getPasswordValidityDurationMinMillis() {
		if (passwordValidityDurationMinMillis == Integer.MIN_VALUE) {
			// TODO system property!
			passwordValidityDurationMinMillis = DEFAULT_PASSWORD_VALIDITIY_DURATION_MIN_MILLIS;
		}
		return passwordValidityDurationMinMillis;
	}

	protected int getRemoveExpiredPasswordsPeriodMillis() {
		if (removeExpiredPasswordsPeriodMillis == Integer.MIN_VALUE) {
			// TODO system property!
			removeExpiredPasswordsPeriodMillis = DEFAULT_REMOVE_EXPIRED_PASSWORDS_PERIOD_MILLIS;
		}
		return removeExpiredPasswordsPeriodMillis;
	}

	private static final Comparator<AuthRepoPassword> newestFirstAuthRepoPasswordComparator = new Comparator<AuthRepoPassword>() {
		@Override
		public int compare(AuthRepoPassword o1, AuthRepoPassword o2) {
			Date expiryDate1 = o1.getAuthToken().getExpiryDateTime().toDate();
			Date expiryDate2 = o2.getAuthToken().getExpiryDateTime().toDate();

			if (expiryDate1.before(expiryDate2))
				return -1;

			if (expiryDate1.after(expiryDate2))
				return +1;

			int result = o1.getServerRepositoryID().toUUID().compareTo(o2.getServerRepositoryID().toUUID());
			if (result != 0)
				return result;

			result = o1.getClientRepositoryID().toUUID().compareTo(o2.getClientRepositoryID().toUUID());
			return result;
		}
	};

	private boolean isAfterRenewalDate(AuthRepoPassword authRepoPassword) {
		assertNotNull("authRepoPassword", authRepoPassword);
		return System.currentTimeMillis() > authRepoPassword.getAuthToken().getRenewalDateTime().getMillis();
	}

	private boolean isExpired(AuthRepoPassword authRepoPassword) {
		assertNotNull("authRepoPassword", authRepoPassword);
		return System.currentTimeMillis() > authRepoPassword.getAuthToken().getExpiryDateTime().getMillis();
	}

	private AuthToken createAuthToken() {
		AuthToken authToken = new AuthToken();
		Date expiryDate = new Date(System.currentTimeMillis() + getPasswordValidityDurationMaxMillis());
		Date renewalDate = new Date(System.currentTimeMillis() + getPasswordValidityDurationMinMillis());
		authToken.setExpiryDateTime(new DateTime(expiryDate));
		authToken.setRenewalDateTime(new DateTime(renewalDate));
		authToken.setPassword(new String(PasswordUtil.createRandomPassword(40, 40)));
		authToken.makeUnmodifiable();
		return authToken;
	}

}
