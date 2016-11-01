package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.ls.core.invoke.filter.ExtMethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.filter.InvocationFilterRegistry;

public class InvokeMethodExecutor {

	private static final Logger logger = LoggerFactory.getLogger(InvokeMethodExecutor.class);

	private static final AtomicInteger nextInstanceId = new AtomicInteger();
	private final int instanceId = nextInstanceId.getAndIncrement();

	private final Executor executor = Executors.newCachedThreadPool();
	private final Map<Uid, InvocationRunnable> delayedResponseId2InvocationRunnable = Collections.synchronizedMap(new HashMap<Uid, InvocationRunnable>());
	private final SortedSet<DelayedResponseIdScheduledEviction> delayedResponseIdScheduledEvictions = Collections.synchronizedSortedSet(new TreeSet<DelayedResponseIdScheduledEviction>());

	private final Timer evictOldDataTimer = new Timer(String.format("InvokeMethodExecutor[%d].evictOldDataTimer", instanceId), true);
	private final TimerTask evictOldDataTimerTask = new TimerTask() {
		@Override
		public void run() {
			try {
				final List<Uid> delayedResponseIdsToEvict = new LinkedList<>();
				synchronized (delayedResponseIdScheduledEvictions) {
					for (final Iterator<DelayedResponseIdScheduledEviction> it = delayedResponseIdScheduledEvictions.iterator(); it.hasNext(); ) {
						final DelayedResponseIdScheduledEviction delayedResponseIdScheduledEviction = it.next();

						if (System.currentTimeMillis() < delayedResponseIdScheduledEviction.getScheduledEvictionTimestamp())
							break;

						delayedResponseIdsToEvict.add(delayedResponseIdScheduledEviction.getDelayedResponseId());
						it.remove();
					}
				}

				synchronized (delayedResponseId2InvocationRunnable) {
					for (Uid delayedResponseId : delayedResponseIdsToEvict)
						delayedResponseId2InvocationRunnable.remove(delayedResponseId);
				}
			} catch (final Throwable t) {
				logger.error("evictOldDataTimerTask.run: " + t, t);
			}
		}
	};

	public InvokeMethodExecutor() {
		evictOldDataTimer.schedule(evictOldDataTimerTask, 60000L, 60000L);
	}

	public MethodInvocationResponse execute(final ExtMethodInvocationRequest extMethodInvocationRequest) throws Exception {
		assertNotNull("extMethodInvocationRequest", extMethodInvocationRequest);

		InvocationFilterRegistry.getInstance().assertCanInvoke(extMethodInvocationRequest);

		final InvocationRunnable invocationRunnable = new InvocationRunnable(extMethodInvocationRequest);
		executor.execute(invocationRunnable);

		synchronized (invocationRunnable) {
			MethodInvocationResponse methodInvocationResponse = invocationRunnable.getMethodInvocationResponse();
			if (methodInvocationResponse != null)
				return methodInvocationResponse;

			Throwable error = invocationRunnable.getError();
			if (error != null)
				throwError(error);

			try {
				invocationRunnable.wait(45000L);
			} catch (InterruptedException e) {
				logger.debug("performMethodInvocation: " + e, e);
			}

			methodInvocationResponse = invocationRunnable.getMethodInvocationResponse();
			if (methodInvocationResponse != null)
				return methodInvocationResponse;

			error = invocationRunnable.getError();
			if (error != null)
				throwError(error);

			final Uid delayedResponseId = invocationRunnable.getDelayedResponseId();
			assertNotNull("delayedResponseId", delayedResponseId);
			delayedResponseId2InvocationRunnable.put(delayedResponseId, invocationRunnable);

			return new DelayedMethodInvocationResponse(delayedResponseId);
		}
	}

	public MethodInvocationResponse getDelayedResponse(final Uid delayedResponseId) throws Exception {
		assertNotNull("delayedResponseId", delayedResponseId);

		long schedEvTiSt = System.currentTimeMillis() + 240000; // scheduled eviction in 4 minutes
		final InvocationRunnable invocationRunnable = delayedResponseId2InvocationRunnable.get(delayedResponseId);
		if (invocationRunnable == null)
			throw new IllegalArgumentException("delayedResponseId unknown: " + delayedResponseId);

		synchronized (invocationRunnable) {
			MethodInvocationResponse methodInvocationResponse = invocationRunnable.getMethodInvocationResponse();
			Throwable error = invocationRunnable.getError();

			if (methodInvocationResponse != null) {
				delayedResponseIdScheduledEvictions.add(new DelayedResponseIdScheduledEviction(schedEvTiSt, delayedResponseId));
				return methodInvocationResponse;
			}
			if (error != null) {
				delayedResponseIdScheduledEvictions.add(new DelayedResponseIdScheduledEviction(schedEvTiSt, delayedResponseId));
				throwError(error);
			}

			try {
				invocationRunnable.wait(45000L);
			} catch (InterruptedException e) {
				logger.debug("performMethodInvocation: " + e, e);
			}

			schedEvTiSt = System.currentTimeMillis() + 240000; // scheduled eviction in 4 minutes

			methodInvocationResponse = invocationRunnable.getMethodInvocationResponse();
			if (methodInvocationResponse != null) {
				delayedResponseIdScheduledEvictions.add(new DelayedResponseIdScheduledEviction(schedEvTiSt, delayedResponseId));
				return methodInvocationResponse;
			}
			error = invocationRunnable.getError();
			if (error != null) {
				delayedResponseIdScheduledEvictions.add(new DelayedResponseIdScheduledEviction(schedEvTiSt, delayedResponseId));
				throwError(error);
			}

			return new DelayedMethodInvocationResponse(delayedResponseId);
		}
	}

	private static void throwError(final Throwable error) throws Exception {
		assertNotNull("error", error);
		if (error instanceof RuntimeException)
			throw (RuntimeException) error;
		else if (error instanceof Error)
			throw (Error) error;
		else
			throw new RuntimeException(error);
	}

	private class InvocationRunnable implements Runnable {
		private final Logger logger = LoggerFactory.getLogger(InvocationRunnable.class);

		private final ExtMethodInvocationRequest extMethodInvocationRequest;
		private MethodInvocationResponse methodInvocationResponse;
		private Throwable error;
		private Uid delayedResponseId;

		public InvocationRunnable(final ExtMethodInvocationRequest extMethodInvocationRequest) {
			this.extMethodInvocationRequest = assertNotNull("extMethodInvocationRequest", extMethodInvocationRequest);
		}

		@Override
		public void run() {
			final ObjectManager objectManager = extMethodInvocationRequest.getObjectManager();
			final MethodInvocationRequest methodInvocationRequest = extMethodInvocationRequest.getMethodInvocationRequest();
			final ClassManager classManager = extMethodInvocationRequest.getObjectManager().getClassManager();
			final Class<?> clazz = extMethodInvocationRequest.getTargetClass();
			final Object object = methodInvocationRequest.getObject();
			final String methodName = methodInvocationRequest.getMethodName();

			final String[] argumentTypeNames = methodInvocationRequest.getArgumentTypeNames();
			final Class<?>[] argumentTypes = argumentTypeNames == null ? null : classManager.getClassesOrFail(argumentTypeNames);

			final Object[] arguments = methodInvocationRequest.getArguments();

			objectManager.getReferenceCleanerRegistry().preInvoke(extMethodInvocationRequest);

			Throwable error = null;
			Object resultObject = null;
			try {

				final InvocationType invocationType = methodInvocationRequest.getInvocationType();
				switch (invocationType) {
					case CONSTRUCTOR:
						resultObject = invokeConstructor(clazz, arguments);
						break;
					case OBJECT:
						resultObject = invoke(object, methodName, argumentTypes, arguments);
						break;
					case STATIC:
						resultObject = invokeStatic(clazz, methodName, arguments);
						break;
					default:
						throw new IllegalStateException("Unknown InvocationType: " + invocationType);
				}

			} catch (final Throwable x) {
				resultObject = null;
				error = x;
				synchronized (this) {
					this.error = x;
				}
				logger.debug("run: " + x, x);
			} finally {
				objectManager.getReferenceCleanerRegistry().postInvoke(extMethodInvocationRequest, resultObject, error);
			}

			synchronized (this) {
				if (this.error == null) {
					methodInvocationResponse = MethodInvocationResponse.forInvocation(resultObject, filterWritableArguments(arguments));
					assertNotNull("methodInvocationResponse", methodInvocationResponse);
				}

				this.notifyAll(); // note: other threads only continue running, *after* this synchronized block is finished entirely!

				if (delayedResponseId != null) {
					// We give the client 15 minutes to fetch the result.
					delayedResponseIdScheduledEvictions.add(new DelayedResponseIdScheduledEviction(
							System.currentTimeMillis() + 900L * 1000L, delayedResponseId));
				}
			}
		}

		public synchronized MethodInvocationResponse getMethodInvocationResponse() {
			return methodInvocationResponse;
		}

		public synchronized Throwable getError() {
			return error;
		}

		public synchronized Uid getDelayedResponseId() {
			if (delayedResponseId == null)
				delayedResponseId = new Uid();

			return delayedResponseId;
		}
	}

	private Object[] filterWritableArguments(final Object[] arguments) {
		if (arguments == null || arguments.length == 0)
			return null;

		boolean atLeastOneWritable = false;
		final Object[] result = new Object[arguments.length];
		for (int i = 0; i < arguments.length; ++i) {
			final Object argument = arguments[i];
			if (argument != null && argument.getClass().isArray()) {
				result[i] = argument;
				atLeastOneWritable = true;
			}
		}
		return atLeastOneWritable ? result : null;
	}
}
