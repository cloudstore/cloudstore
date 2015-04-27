package co.codewizards.cloudstore.rest.server.auth;

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
import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.util.AssertUtil;
import co.codewizards.cloudstore.core.util.PasswordUtil;

public class TransientRepoPasswordManager {

	private static final int DEFAULT_VALIDITIY_PERIOD = 60 * 60 * 1000;
	private static final int DEFAULT_RENEWAL_PERIOD = 30 * 60 * 1000;
	private static final int DEFAULT_EARLY_RENEWAL_PERIOD = 15 * 60 * 1000;
	private static final int DEFAULT_EXPIRY_TIMER_PERIOD = 60 * 1000;

	public static final String CONFIG_KEY_VALIDITIY_PERIOD = "transientRepoPassword.validityPeriod";
	public static final String CONFIG_KEY_RENEWAL_PERIOD = "transientRepoPassword.renewalPeriod";
	public static final String CONFIG_KEY_EARLY_RENEWAL_PERIOD = "transientRepoPassword.earlyRenewalPeriod";
	public static final String CONFIG_KEY_EXPIRY_TIMER_PERIOD = "transientRepoPassword.expiryTimerPeriod";

	private int validityPeriod = Integer.MIN_VALUE;
	private int renewalPeriod = Integer.MIN_VALUE;
	private int earlyRenewalPeriod = Integer.MIN_VALUE;
	private int expiryTimerPeriod = Integer.MIN_VALUE;

	private static class TransientRepoPasswordManagerHolder {
		public static final TransientRepoPasswordManager instance = new TransientRepoPasswordManager();
	}

	protected TransientRepoPasswordManager() { }

	public static TransientRepoPasswordManager getInstance() {
		return TransientRepoPasswordManagerHolder.instance;
	}

	private final Map<UUID, Map<UUID, SortedSet<TransientRepoPassword>>> serverRepositoryId2ClientRepositoryId2AuthRepoPasswordSet = new HashMap<UUID, Map<UUID,SortedSet<TransientRepoPassword>>>();
	private final SortedSet<TransientRepoPassword> transientRepoPasswords = new TreeSet<TransientRepoPassword>(newestFirstAuthRepoPasswordComparator);

	private final Timer timer = new Timer();
	private final TimerTask removeExpiredAuthRepoPasswordsTimerTask = new TimerTask() {
		@Override
		public void run() {
			removeExpiredAuthRepoPasswords();
		}
	};
	{
		timer.schedule(removeExpiredAuthRepoPasswordsTimerTask, getExpiryTimerPeriod(), getExpiryTimerPeriod());
	}

	public synchronized TransientRepoPassword getCurrentAuthRepoPassword(final UUID serverRepositoryId, final UUID clientRepositoryId) {
		AssertUtil.assertNotNull("serverRepositoryId", serverRepositoryId);
		AssertUtil.assertNotNull("clientRepositoryId", clientRepositoryId);

		Map<UUID, SortedSet<TransientRepoPassword>> clientRepositoryId2AuthRepoPasswordSet = serverRepositoryId2ClientRepositoryId2AuthRepoPasswordSet.get(serverRepositoryId);
		if (clientRepositoryId2AuthRepoPasswordSet == null) {
			clientRepositoryId2AuthRepoPasswordSet = new HashMap<UUID, SortedSet<TransientRepoPassword>>();
			serverRepositoryId2ClientRepositoryId2AuthRepoPasswordSet.put(serverRepositoryId, clientRepositoryId2AuthRepoPasswordSet);
		}

		SortedSet<TransientRepoPassword> authRepoPasswordSet = clientRepositoryId2AuthRepoPasswordSet.get(clientRepositoryId);
		if (authRepoPasswordSet == null) {
			authRepoPasswordSet = new TreeSet<TransientRepoPassword>(newestFirstAuthRepoPasswordComparator);
			clientRepositoryId2AuthRepoPasswordSet.put(clientRepositoryId, authRepoPasswordSet);
		}

		TransientRepoPassword transientRepoPassword = authRepoPasswordSet.isEmpty() ? null : authRepoPasswordSet.first();
		if (transientRepoPassword != null && isAfterRenewalDateOrInEarlyRenewalPeriod(transientRepoPassword))
			transientRepoPassword = null;

		if (transientRepoPassword == null) {
			transientRepoPassword = new TransientRepoPassword(serverRepositoryId, clientRepositoryId, createAuthToken());
			authRepoPasswordSet.add(transientRepoPassword);
			transientRepoPasswords.add(transientRepoPassword);
		}
		return transientRepoPassword;
	}

	public synchronized boolean isPasswordValid(final UUID serverRepositoryId, final UUID clientRepositoryId, final char[] password) {
		AssertUtil.assertNotNull("serverRepositoryId", serverRepositoryId);
		AssertUtil.assertNotNull("clientRepositoryId", clientRepositoryId);
		AssertUtil.assertNotNull("password", password);
		final Map<UUID, SortedSet<TransientRepoPassword>> clientRepositoryId2AuthRepoPasswordSet = serverRepositoryId2ClientRepositoryId2AuthRepoPasswordSet.get(serverRepositoryId);
		if (clientRepositoryId2AuthRepoPasswordSet == null)
			return false;

		final SortedSet<TransientRepoPassword> authRepoPasswordSet = clientRepositoryId2AuthRepoPasswordSet.get(clientRepositoryId);
		if (authRepoPasswordSet == null)
			return false;

		for (final TransientRepoPassword transientRepoPassword : authRepoPasswordSet) {
			if (isExpired(transientRepoPassword)) // newest first => first expired means all following expired, too!
				return false;

			if (Arrays.equals(password, transientRepoPassword.getPassword()))
				return true;
		}
		return false;
	}

	private synchronized void removeExpiredAuthRepoPasswords() {
		while (!transientRepoPasswords.isEmpty()) {
			final TransientRepoPassword oldestAuthRepoPassword = transientRepoPasswords.last();
			if (!isExpired(oldestAuthRepoPassword)) // newest first => last not yet expired means all previous not yet expired, either
				break;

			transientRepoPasswords.remove(oldestAuthRepoPassword);
			final UUID serverRepositoryId = oldestAuthRepoPassword.getServerRepositoryId();
			final UUID clientRepositoryId = oldestAuthRepoPassword.getClientRepositoryId();

			final Map<UUID, SortedSet<TransientRepoPassword>> clientRepositoryId2AuthRepoPasswordSet = serverRepositoryId2ClientRepositoryId2AuthRepoPasswordSet.get(serverRepositoryId);
			AssertUtil.assertNotNull("clientRepositoryId2AuthRepoPasswordSet", clientRepositoryId2AuthRepoPasswordSet);

			final SortedSet<TransientRepoPassword> authRepoPasswordSet = clientRepositoryId2AuthRepoPasswordSet.get(clientRepositoryId);
			AssertUtil.assertNotNull("authRepoPasswordSet", authRepoPasswordSet);

			authRepoPasswordSet.remove(oldestAuthRepoPassword);

			if (authRepoPasswordSet.isEmpty())
				clientRepositoryId2AuthRepoPasswordSet.remove(clientRepositoryId);

			if (clientRepositoryId2AuthRepoPasswordSet.isEmpty())
				serverRepositoryId2ClientRepositoryId2AuthRepoPasswordSet.remove(serverRepositoryId);
		}
	}

	protected int getValidityPeriod() {
		if (validityPeriod == Integer.MIN_VALUE) {
			validityPeriod = Config.getInstance().getPropertyAsInt(
					CONFIG_KEY_VALIDITIY_PERIOD, DEFAULT_VALIDITIY_PERIOD);
		}
		return validityPeriod;
	}

	protected int getRenewalPeriod() {
		if (renewalPeriod == Integer.MIN_VALUE) {
			renewalPeriod = Config.getInstance().getPropertyAsInt(
					CONFIG_KEY_RENEWAL_PERIOD, DEFAULT_RENEWAL_PERIOD);
		}
		return renewalPeriod;
	}

	protected int getEarlyRenewalPeriod() {
		if (earlyRenewalPeriod == Integer.MIN_VALUE) {
			earlyRenewalPeriod = Config.getInstance().getPropertyAsInt(
					CONFIG_KEY_EARLY_RENEWAL_PERIOD, DEFAULT_EARLY_RENEWAL_PERIOD);
		}
		return earlyRenewalPeriod;
	}

	protected int getExpiryTimerPeriod() {
		if (expiryTimerPeriod == Integer.MIN_VALUE) {
			expiryTimerPeriod = Config.getInstance().getPropertyAsInt(
					CONFIG_KEY_EXPIRY_TIMER_PERIOD, DEFAULT_EXPIRY_TIMER_PERIOD);
		}
		return expiryTimerPeriod;
	}

	private static final Comparator<TransientRepoPassword> newestFirstAuthRepoPasswordComparator = new Comparator<TransientRepoPassword>() {
		@Override
		public int compare(final TransientRepoPassword o1, final TransientRepoPassword o2) {
			final Date expiryDate1 = o1.getAuthToken().getExpiryDateTime().toDate();
			final Date expiryDate2 = o2.getAuthToken().getExpiryDateTime().toDate();

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

	private boolean isAfterRenewalDateOrInEarlyRenewalPeriod(final TransientRepoPassword transientRepoPassword) {
		AssertUtil.assertNotNull("authRepoPassword", transientRepoPassword);
		return System.currentTimeMillis() + getEarlyRenewalPeriod() > transientRepoPassword.getAuthToken().getRenewalDateTime().getMillis();
	}

	private boolean isExpired(final TransientRepoPassword transientRepoPassword) {
		AssertUtil.assertNotNull("authRepoPassword", transientRepoPassword);
		return System.currentTimeMillis() > transientRepoPassword.getAuthToken().getExpiryDateTime().getMillis();
	}

	private AuthToken createAuthToken() {
		final AuthToken authToken = new AuthToken();
		final Date expiryDate = new Date(System.currentTimeMillis() + getValidityPeriod());
		final Date renewalDate = new Date(System.currentTimeMillis() + getRenewalPeriod());
		authToken.setExpiryDateTime(new DateTime(expiryDate));
		authToken.setRenewalDateTime(new DateTime(renewalDate));
		authToken.setPassword(new String(PasswordUtil.createRandomPassword(40)));
		authToken.makeUnmodifiable();
		return authToken;
	}

}
