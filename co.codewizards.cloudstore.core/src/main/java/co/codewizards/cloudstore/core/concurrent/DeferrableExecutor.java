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

public class DeferrableExecutor {
	private static final long EXPIRY_PERIOD_MS = 60L * 60L * 1000L;

	private final Map<String, WeakReference<String>> canonicalCallIdentifierMap = new WeakHashMap<String, WeakReference<String>>();
	private final Map<String, Future<?>> callIdentifier2Future = Collections.synchronizedMap(new HashMap<String, Future<?>>());
	private final Map<String, Date> callIdentifier2DoneDate = Collections.synchronizedMap(new WeakHashMap<String, Date>());
	private final ExecutorService executorService = Executors.newCachedThreadPool();
	private final Timer cleanUpExpiredEntriesTimer = new Timer("cleanUpExpiredEntriesTimer", true);
	private final TimerTask cleanUpExpiredEntriesTimerTask = new TimerTask() {
		@Override
		public void run() {
			cleanUpExpiredEntries();
		}
	};

	private DeferrableExecutor() {
		cleanUpExpiredEntriesTimer.schedule(cleanUpExpiredEntriesTimerTask, EXPIRY_PERIOD_MS / 2, EXPIRY_PERIOD_MS / 2);
	}

	private static final class RunnableWithProgressExecutorHolder {
		private static final DeferrableExecutor instance = new DeferrableExecutor();
	}

	public static DeferrableExecutor getInstance() {
		return RunnableWithProgressExecutorHolder.instance;
	}

	@SuppressWarnings("unchecked")
	public <V> V call(String callIdentifier, CallableProvider<V> callableProvider, long timeout, TimeUnit timeUnit) throws DeferredCompletionException, ExecutionException {
		assertNotNull("callIdentifier", callIdentifier);
		assertNotNull("callableProvider", callableProvider);
		if (timeout <= 0) throw new IllegalArgumentException("timeout <= 0");
		assertNotNull("timeUnit", timeUnit);

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
				result = future.get(timeout, timeUnit);
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
		List<String> expiredCallIdentifiers = new LinkedList<String>();
		Date expireDoneBeforeDate = new Date(System.currentTimeMillis() - EXPIRY_PERIOD_MS);
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
}
