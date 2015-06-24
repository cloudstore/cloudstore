package co.codewizards.cloudstore.ls.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.ls.core.invoke.ClassManager;
import co.codewizards.cloudstore.ls.core.invoke.DelayedMethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.InvocationType;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRefWithRefId;
import co.codewizards.cloudstore.ls.core.invoke.filter.ExtMethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.filter.InvocationFilterRegistry;
import co.codewizards.cloudstore.ls.core.provider.MediaTypeConst;
import co.codewizards.cloudstore.ls.rest.server.InverseInvoker;

@Path("InvokeMethod")
@Consumes(MediaTypeConst.APPLICATION_JAVA_NATIVE_WITH_OBJECT_REF)
@Produces(MediaTypeConst.APPLICATION_JAVA_NATIVE_WITH_OBJECT_REF)
public class InvokeMethodService extends AbstractService {

	private static final Logger logger = LoggerFactory.getLogger(InvokeMethodService.class);

	private static final Executor executor = Executors.newCachedThreadPool();
	private static final Map<Uid, InvocationRunnable> delayedResponseId2InvocationRunnable = Collections.synchronizedMap(new HashMap<Uid, InvocationRunnable>());
	private static final SortedSet<DelayedResponseIdScheduledEviction> delayedResponseIdScheduledEvictions = Collections.synchronizedSortedSet(new TreeSet<DelayedResponseIdScheduledEviction>());

	private static final Timer evictOldDataTimer = new Timer("InvokeMethodService.evictOldDataTimer", true);
	private static final TimerTask evictOldDataTimerTask = new TimerTask() {
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
	static {
		evictOldDataTimer.schedule(evictOldDataTimerTask, 60000L, 60000L);
	}

	@POST
	public MethodInvocationResponse performMethodInvocation(final MethodInvocationRequest methodInvocationRequest) throws Throwable {
		assertNotNull("methodInvocationRequest", methodInvocationRequest);

		// *always* acquiring to make sure the lastUseDate is updated - and to make things easy: we have what we need.
		final InverseInvoker inverseInvoker = getInverseInvoker();
		final ObjectManager objectManager = inverseInvoker.getObjectManager();
		final ClassManager classManager = objectManager.getClassManager();

		final String className = methodInvocationRequest.getClassName();
		final Class<?> clazz = className == null ? null : classManager.getClassOrFail(className);

		final String methodName = methodInvocationRequest.getMethodName();

		if (ObjectRef.VIRTUAL_METHOD_NAME_INC_REF_COUNT.equals(methodName)) {
			final ObjectRefWithRefId[] objectRefWithRefIds = cast(methodInvocationRequest.getArguments()[0]);
			for (final ObjectRefWithRefId objectRefWithRefId : objectRefWithRefIds)
				objectManager.incRefCount(objectRefWithRefId.object, objectRefWithRefId.refId);

			return MethodInvocationResponse.forInvocation(null);
		}
		else if (ObjectRef.VIRTUAL_METHOD_NAME_DEC_REF_COUNT.equals(methodName)) {
			final ObjectRefWithRefId[] objectRefWithRefIds = cast(methodInvocationRequest.getArguments()[0]);
			for (final ObjectRefWithRefId objectRefWithRefId : objectRefWithRefIds)
				objectManager.decRefCount(objectRefWithRefId.object, objectRefWithRefId.refId);

			return MethodInvocationResponse.forInvocation(null);
		}

		final ExtMethodInvocationRequest extMethodInvocationRequest = new ExtMethodInvocationRequest(objectManager, methodInvocationRequest, clazz);
		InvocationFilterRegistry.getInstance().assertCanInvoke(extMethodInvocationRequest);

		final InvocationRunnable invocationRunnable = new InvocationRunnable(extMethodInvocationRequest);
		executor.execute(invocationRunnable);

		synchronized (invocationRunnable) {
			MethodInvocationResponse methodInvocationResponse = invocationRunnable.getMethodInvocationResponse();
			if (methodInvocationResponse != null)
				return methodInvocationResponse;

			Throwable error = invocationRunnable.getError();
			if (error != null)
				throw error;

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
				throw error;

			final Uid delayedResponseId = invocationRunnable.getDelayedResponseId();
			assertNotNull("delayedResponseId", delayedResponseId);
			delayedResponseId2InvocationRunnable.put(delayedResponseId, invocationRunnable);

			return new DelayedMethodInvocationResponse(delayedResponseId);
		}
	}

	@GET
	@Path("{delayedResponseId}")
	public MethodInvocationResponse getDelayedMethodInvocationResponse(@PathParam("delayedResponseId") final Uid delayedResponseId) throws Throwable {
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
				throw error;
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
				throw error;
			}

			return new DelayedMethodInvocationResponse(delayedResponseId);
		}
	}

	private static class InvocationRunnable implements Runnable {
		private static final Logger logger = LoggerFactory.getLogger(InvokeMethodService.InvocationRunnable.class);

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
					methodInvocationResponse = MethodInvocationResponse.forInvocation(resultObject);
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
}
