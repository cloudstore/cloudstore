package co.codewizards.cloudstore.core.concurrent;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;

public class DeferrableExecutor {
	private static final Logger logger = LoggerFactory.getLogger(DeferrableExecutor.class);

	/**
	 * The {@code key} for the timeout used with {@link Config#getPropertyAsInt(String, int)}.
	 * <p>
	 * The configuration can be overridden by the system property {@link #SYSTEM_PROPERTY_TIMEOUT}.
	 */
	public static final String CONFIG_KEY_TIMEOUT = "deferrableExecutor.timeout"; //$NON-NLS-1$

	public static final String SYSTEM_PROPERTY_TIMEOUT = "cloudstore.deferrableExecutor.timeout"; //$NON-NLS-1$

	private static final int DEFAULT_TIMEOUT = 60 * 1000;

	/**
	 * The {@code key} for the expiry period used with {@link Config#getPropertyAsInt(String, int)}.
	 * <p>
	 * The configuration can be overridden by the system property {@link #SYSTEM_PROPERTY_TIMEOUT}.
	 */
	public static final String CONFIG_KEY_EXPIRY_PERIOD = "deferrableExecutor.expiryPeriod"; //$NON-NLS-1$

	public static final String SYSTEM_PROPERTY_EXPIRY_PERIOD = "cloudstore.deferrableExecutor.expiryPeriod"; //$NON-NLS-1$

	private static final int DEFAULT_EXPIRY_PERIOD = 60 * 60 * 1000;

	private final Map<String, WeakReference<String>> canonicalCallIdentifierMap = new WeakHashMap<String, WeakReference<String>>();
	private final Map<String, Future<?>> callIdentifier2Future = Collections.synchronizedMap(new HashMap<String, Future<?>>());
	private final Map<String, Date> callIdentifier2DoneDate = Collections.synchronizedMap(new WeakHashMap<String, Date>());
	private final ExecutorService executorService = Executors.newCachedThreadPool();
	private final Timer cleanUpExpiredEntriesTimer = new Timer("cleanUpExpiredEntriesTimer", true);
	private TimerTask cleanUpExpiredEntriesTimerTask;
	private int lastExpiryPeriod;

	private DeferrableExecutor() { }

	private static final class RunnableWithProgressExecutorHolder {
		private static final DeferrableExecutor instance = new DeferrableExecutor();
	}

	public static DeferrableExecutor getInstance() {
		return RunnableWithProgressExecutorHolder.instance;
	}

	// TODO maybe we should make it possible to pass the timeout from the client, because
	// the client knows its socket's read-timeout.
	@SuppressWarnings("unchecked")
	public <V> V call(String callIdentifier, final CallableProvider<V> callableProvider) throws DeferredCompletionException, ExecutionException {
		assertNotNull("callIdentifier", callIdentifier);
		assertNotNull("callableProvider", callableProvider);

		final int timeout = getConfigPropertyAsPositiveInt(SYSTEM_PROPERTY_TIMEOUT, CONFIG_KEY_TIMEOUT, DEFAULT_TIMEOUT);

		cleanUpExpiredEntries();
		callIdentifier = canonicalizeCallIdentifier(callIdentifier);
		synchronized (callIdentifier) {
			Future<?> future = callIdentifier2Future.get(callIdentifier);
			if (future == null) {
				Callable<V> callable = callableProvider.getCallable();
				future = executorService.submit(new CallableWrapper<V>(callIdentifier, callable));
				callIdentifier2Future.put(callIdentifier, future);
			}

			Object result;
			try {
				result = future.get(timeout, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				throw new DeferredCompletionException(e);
			} catch (TimeoutException e) {
				throw new DeferredCompletionException(e);
			} catch (java.util.concurrent.ExecutionException e) {
				callIdentifier2Future.remove(callIdentifier); // remove in case of failure (keep while still running)
				throw new ExecutionException(e);
			}

			callIdentifier2Future.remove(callIdentifier); // remove in case of successful completion (keep while still running)
			return (V) result;
		}
	}

	private class CallableWrapper<V> implements Callable<V> {
		private final String identifier;
		private final Callable<V> delegate;

		public CallableWrapper(String identifier, Callable<V> delegate) {
			this.identifier = assertNotNull("identifier", identifier);
			this.delegate = assertNotNull("delegate", delegate);
		}

		@Override
		public V call() throws Exception {
			try {
				return delegate.call();
			} finally {
				callIdentifier2DoneDate.put(identifier, new Date());
			}
		}

	}

	private String canonicalizeCallIdentifier(String callIdentifier) {
		synchronized (canonicalCallIdentifierMap) {
			WeakReference<String> ref = canonicalCallIdentifierMap.get(callIdentifier);
			String ci = ref == null ? null : ref.get();
			if (ci == null) {
				ci = callIdentifier;
				canonicalCallIdentifierMap.put(ci, new WeakReference<String>(ci));
			}
			return ci;
		}
	}

	private void cleanUpExpiredEntries() {
		rescheduleExpiredEntriesTimerTaskIfExpiryPeriodChanged();

		List<String> expiredCallIdentifiers = new LinkedList<String>();
		Date expireDoneBeforeDate = new Date(System.currentTimeMillis() - getExpiryPeriod());
		synchronized (callIdentifier2DoneDate) {
			for (Map.Entry<String, Date> me : callIdentifier2DoneDate.entrySet()) {
				if (me.getValue().before(expireDoneBeforeDate))
					expiredCallIdentifiers.add(me.getKey());
			}
		}
		for (String callIdentifier : expiredCallIdentifiers) {
			synchronized (callIdentifier) {
				callIdentifier2Future.remove(callIdentifier);
				callIdentifier2DoneDate.remove(callIdentifier);
			}
		}
	}

	private void rescheduleExpiredEntriesTimerTaskIfExpiryPeriodChanged() {
		synchronized (cleanUpExpiredEntriesTimer) {
			final int expiryPeriod = getExpiryPeriod();
			if (cleanUpExpiredEntriesTimerTask == null || lastExpiryPeriod != expiryPeriod) {
				if (cleanUpExpiredEntriesTimerTask != null)
					cleanUpExpiredEntriesTimerTask.cancel();

				scheduleExpiredEntriesTimerTask();
			}
		}
	}

	private void scheduleExpiredEntriesTimerTask() {
		synchronized (cleanUpExpiredEntriesTimer) {
			final int expiryPeriod = getExpiryPeriod();
			lastExpiryPeriod = expiryPeriod;

			cleanUpExpiredEntriesTimerTask = new TimerTask() {
				@Override
				public void run() {
					cleanUpExpiredEntries();
				}
			};

			cleanUpExpiredEntriesTimer.schedule(cleanUpExpiredEntriesTimerTask, expiryPeriod / 2, expiryPeriod / 2);
		}
	}

	private int getExpiryPeriod() {
		return getConfigPropertyAsPositiveInt(SYSTEM_PROPERTY_EXPIRY_PERIOD, CONFIG_KEY_EXPIRY_PERIOD, DEFAULT_EXPIRY_PERIOD);
	}

	private int getConfigPropertyAsPositiveInt(String systemProperty, String configKey, int defaultValue) {
		String value = System.getProperty(systemProperty);
		if (value == null || value.isEmpty()) {
			logger.info("System property '{}' is undefined! Looking for key '{}' in global config.", systemProperty, configKey);
			return _getConfigPropertyAsPositiveInt(configKey, defaultValue);
		}
		try {
			int result = Integer.parseInt(value);
			if (result < 0)
				throw new NumberFormatException("Only values greater than or equal to 0 are allowed!");

			return result;
		} catch (NumberFormatException x) {
			logger.warn("System property '{}' is set to the illegal value '{}'!  Looking for key '{}' in global config.", systemProperty, value, configKey);
			return _getConfigPropertyAsPositiveInt(configKey, defaultValue);
		}
	}

	private int _getConfigPropertyAsPositiveInt(String configKey, int defaultValue) {
		int value = Config.getInstance().getPropertyAsInt(configKey, defaultValue);
		if (value < 0) {
			logger.warn("Config property '{}' is set to the illegal value '{}'!  Falling back to default value: {}", configKey, value, defaultValue);
			return defaultValue;
		}
		return value;
	}
}
