package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.DateUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.ls.core.invoke.refjanitor.ReferenceJanitorRegistry;

public class ObjectManager {
	/**
	 * Timeout after which an unused {@code ObjectManager} is evicted.
	 * <p>
	 * If a client is closed normally (or crashes) we must make sure that the object-references held
	 * by this {@code ObjectManager} in the server's JVM are released and can be garbage-collected.
	 * Therefore, we track the {@linkplain #getLastUseDate() last use timestamp} (e.g.
	 * {@linkplain #updateLastUseDate() update it} when {@link #getInstance(Uid)} is called).
	 * <p>
	 * Periodically, all {@code ObjectManager}s not being used for a time period longer than this timeout
	 * are "forgotten" and thus both the {@code ObjectManager}s and all the objects they hold can be
	 * garbage-collected.
	 * <p>
	 * This timeout must be (significantly) longer than {@code InverseInvoker.POLL_INVERSE_SERVICE_REQUEST_TIMEOUT_MS}
	 * to make sure, the long-polling of inverse-service-invocation-requests serves additionally as a keep-alive for
	 * the server-side {@code ObjectManager}.
	 */
	protected static final long EVICT_UNUSED_OBJECT_MANAGER_TIMEOUT_MS = 2 * 60 * 1000L; // 2 minutes
	protected static final long EVICT_UNUSED_OBJECT_MANAGER_PERIOD_MS = 60 * 1000L;

	/**
	 * The other side must notify us that an object is actually used (by invoking {@link #incRefCount(Object, Uid)})
	 * within this timeout.
	 * <p>
	 * Thus, this timeout must be longer than the maximum time it ever takes to
	 * (1) transmit the entire object graph from here to the other side and (2) notify in the inverse direction
	 * (increment reference count).
	 * <p>
	 * Note, that the inverse notification is deferred for performance reasons!
	 * {@link IncDecRefCountQueue#INC_DEC_REF_COUNT_PERIOD_MS} thus must be significantly shorter than this timeout
	 * here.
	 */
	protected static final long EVICT_ZERO_REFERENCE_OBJECT_REFS_TIMEOUT_MS = 30 * 1000L; // 30 seconds
	protected static final long EVICT_ZERO_REFERENCE_OBJECT_REFS_PERIOD_MS = 5 * 1000L;

	private static final Logger logger = LoggerFactory.getLogger(ObjectManager.class);

	private final Uid clientId;

	private long nextObjectId;

	private volatile Date lastUseDate;
	private volatile boolean neverEvict;

	private boolean closed;

	private final Map<ObjectRef, Object> objectRef2Object = new HashMap<>();
	private final Map<Object, ObjectRef> object2ObjectRef = new IdentityHashMap<>();
	private final Map<String, Object> contextObjectMap = new HashMap<>();

	private final Map<ObjectRef, Long> zeroReferenceObjectRef2Timestamp = new HashMap<>();
	private final Map<ObjectRef, Set<Uid>> objectRef2RefIds = new HashMap<>();

	private final RemoteObjectProxyManager remoteObjectProxyManager = new RemoteObjectProxyManager();
	private final ClassManager classManager;
	private final ReferenceJanitorRegistry referenceJanitorRegistry;

	private static final Map<Uid, ObjectManager> clientId2ObjectManager = new HashMap<>();

	private static long evictOldObjectManagersLastInvocation = 0;
	private static long evictZeroReferenceObjectRefsLastInvocation = 0;

	private static final Timer timer = new Timer(true);
	private static final TimerTask timerTask = new TimerTask() {
		@Override
		public void run() {
			try {
				evictOldObjectManagers();
			} catch (Exception x) {
				logger.error("run: " + x, x);
			}
			try {
				allObjectManagers_evictZeroReferenceObjectRefs();
			} catch (Exception x) {
				logger.error("run: " + x, x);
			}
		}
	};
	static {
		final long period = BigInteger.valueOf(EVICT_UNUSED_OBJECT_MANAGER_PERIOD_MS)
				.gcd(BigInteger.valueOf(EVICT_ZERO_REFERENCE_OBJECT_REFS_PERIOD_MS)).longValue();

		timer.schedule(timerTask, period, period);
	}

	public static synchronized ObjectManager getInstance(final Uid clientId) {
		requireNonNull(clientId, "clientId");
		ObjectManager objectManager = clientId2ObjectManager.get(clientId);
		if (objectManager == null) {
			objectManager = new ObjectManager(clientId);
			clientId2ObjectManager.put(clientId, objectManager);
		}
		objectManager.updateLastUseDate();
		return objectManager;
	}

	/**
	 * @deprecated Only used for tests! Don't use this method productively!
	 */
	@Deprecated
	public static synchronized void clearObjectManagers() {
		clientId2ObjectManager.clear();
	}

	private static void evictOldObjectManagers() {
		int objectManagerCountTotal = 0;
		int objectManagerCountNeverEvict = 0;

		final List<ObjectManager> objectManagersToClose = new LinkedList<>();
		synchronized (ObjectManager.class) {
			final long now = System.currentTimeMillis();

			if (evictOldObjectManagersLastInvocation > now - EVICT_UNUSED_OBJECT_MANAGER_PERIOD_MS)
				return;

			evictOldObjectManagersLastInvocation = now;

			for (final ObjectManager objectManager : clientId2ObjectManager.values()) {
				++objectManagerCountTotal;

				if (objectManager.isNeverEvict()) {
					++objectManagerCountNeverEvict;
					continue;
				}

				if (objectManager.getLastUseDate().getTime() < now - EVICT_UNUSED_OBJECT_MANAGER_TIMEOUT_MS) {
					objectManagersToClose.add(objectManager);
					logger.debug("evictOldObjectManagers: evicting ObjectManager with clientId={}", objectManager.getClientId());
				}
			}
		}

		for (final ObjectManager objectManager : objectManagersToClose)
			objectManager.close();

		logger.debug("evictOldObjectManagers: objectManagerCountTotal={} objectManagerCountNeverEvict={} objectManagerCountEvicted={}",
				objectManagerCountTotal, objectManagerCountNeverEvict, objectManagersToClose.size());
	}

	private static synchronized List<ObjectManager> getObjectManagers() {
		final List<ObjectManager> objectManagers = new ArrayList<ObjectManager>(clientId2ObjectManager.values());
		return objectManagers;
	}

	private static void allObjectManagers_evictZeroReferenceObjectRefs() {
		final long now = System.currentTimeMillis();

		if (evictZeroReferenceObjectRefsLastInvocation > now - EVICT_ZERO_REFERENCE_OBJECT_REFS_PERIOD_MS)
			return;

		evictZeroReferenceObjectRefsLastInvocation = now;

		final List<ObjectManager> objectManagers = getObjectManagers();
		for (final ObjectManager objectManager : objectManagers)
			objectManager.evictZeroReferenceObjectRefs();
	}

	private synchronized void evictZeroReferenceObjectRefs() {
		final long now = System.currentTimeMillis();

		final LinkedList<ObjectRef> objectRefsToRemove = new LinkedList<>();
		for (final Map.Entry<ObjectRef, Long> me : zeroReferenceObjectRef2Timestamp.entrySet()) {
			final ObjectRef objectRef = me.getKey();
			final long timestamp = me.getValue();

			if (timestamp < now - EVICT_ZERO_REFERENCE_OBJECT_REFS_TIMEOUT_MS)
				objectRefsToRemove.add(objectRef);
		}

		for (final ObjectRef objectRef : objectRefsToRemove)
			remove(objectRef);
	}

	protected ObjectManager(final Uid clientId) {
		this.clientId = requireNonNull(clientId, "clientId");
		classManager = new ClassManager(clientId);
		referenceJanitorRegistry = new ReferenceJanitorRegistry(this);
		logger.debug("[{}].<init>: Created ObjectManager.", clientId);
	}

	protected Date getLastUseDate() {
		return lastUseDate;
	}
	private void updateLastUseDate() {
		this.lastUseDate = now();
	}

	public boolean isNeverEvict() {
		return neverEvict;
	}

	public void setNeverEvict(boolean neverEvict) {
		this.neverEvict = neverEvict;
	}

	/**
	 * Gets the id of the client using this {@code ObjectManager}. This is either the remote client talking to a server
	 * or it is the server (when the remote client holds references e.g. to listeners or other callbacks for the server).
	 * @return the id of the client.
	 */
	public Uid getClientId() {
		return clientId;
	}

	public synchronized Object getContextObject(String key) {
		return contextObjectMap.get(key);
	}

	public synchronized void putContextObject(String key, Object object) {
		contextObjectMap.put(key, object);
	}

	protected synchronized ObjectRef createObjectRef(Class<?> clazz) {
		assertNotClosed();

		final int classId = classManager.getClassIdOrCreate(clazz);
		final ObjectRef objectRef = new ObjectRef(clientId, classId, nextObjectId++);

		if (! classManager.isClassIdKnownByRemoteSide(classId))
			objectRef.setClassInfo(classManager.getClassInfo(classId));

		return objectRef;
	}

	public synchronized Object getObjectRefOrObject(final Object object) {
		if (isObjectRefMappingEnabled(object))
			return getObjectRefOrCreate(object);
		else
			return object;
	}

	public synchronized ObjectRef getObjectRefOrCreate(final Object object) {
		ObjectRef objectRef = getObjectRef(object);
		if (objectRef == null) {
			objectRef = createObjectRef(object.getClass());

			if (logger.isDebugEnabled())
				logger.debug("[{}].getObjectRefOrCreate: Created {} for {} ({}).", clientId, objectRef, toIdentityString(object), object);

			objectRef2Object.put(objectRef, object);
			object2ObjectRef.put(object, objectRef);
			zeroReferenceObjectRef2Timestamp.put(objectRef, System.currentTimeMillis());
		}
		else {
			// Must refresh timestamp to guarantee enough time for reference handling.
			// Otherwise it might be released after maybe only a few millis!
			if (zeroReferenceObjectRef2Timestamp.containsKey(objectRef))
				zeroReferenceObjectRef2Timestamp.put(objectRef, System.currentTimeMillis());
		}
		return objectRef;
	}

	public synchronized ObjectRef getObjectRefOrFail(final Object object) {
		final ObjectRef objectRef = getObjectRef(object);
		if (objectRef == null)
			throw new IllegalArgumentException(String.format("ObjectManager[%s] does not have ObjectRef for this object: %s (%s)",
					clientId, toIdentityString(object), object));

		return objectRef;
	}

	public synchronized ObjectRef getObjectRef(final Object object) {
		requireNonNull(object, "object");
		assertNotInstanceOfObjectRef(object);
		final ObjectRef objectRef = object2ObjectRef.get(object);
		updateLastUseDate();
		return objectRef;
	}

	public synchronized Object getObjectOrFail(final ObjectRef objectRef) {
		final Object object = getObject(objectRef);
		if (object == null)
			throw new IllegalArgumentException(String.format("ObjectManager[%s] does not have object for this ObjectRef: %s",
					clientId, objectRef));

		return object;
	}

	public synchronized Object getObject(final ObjectRef objectRef) {
		requireNonNull(objectRef, "objectRef");
		final Object object = objectRef2Object.get(objectRef);
		updateLastUseDate();
		return object;
	}

	private synchronized void remove(final ObjectRef objectRef) {
		requireNonNull(objectRef, "objectRef");

		if (!objectRef2Object.containsKey(objectRef))
			throw new IllegalStateException("!objectRef2Object.containsKey(objectRef): " + objectRef);

		zeroReferenceObjectRef2Timestamp.remove(objectRef);
		final Object object = objectRef2Object.remove(objectRef);
		object2ObjectRef.remove(object);
		updateLastUseDate();

		logger.debug("remove: {}", objectRef);
	}

	public synchronized void incRefCount(final Object object, final Uid refId) {
		requireNonNull(object, "object");
		requireNonNull(refId, "refId");

		int refCountBefore;
		int refCountAfter;

		final ObjectRef objectRef = getObjectRefOrFail(object);
		if (zeroReferenceObjectRef2Timestamp.remove(objectRef) != null) {
			if (objectRef2RefIds.put(objectRef, new HashSet<Uid>(Collections.singleton(refId))) != null)
				throw new IllegalStateException("Collision! WTF?!");

			refCountBefore = 0;
			refCountAfter = 1;
		}
		else {
			final Set<Uid> refIds = objectRef2RefIds.get(objectRef);
			refCountBefore = refIds.size();
			requireNonNull(refIds, "objectRef2RefIds.get(" + objectRef + ")");
			refIds.add(refId);
			refCountAfter = refIds.size();
		}
		classManager.setClassIdKnownByRemoteSide(objectRef.getClassId());

		logger.trace("[{}].incRefCount: {} refCountAfter={} refCountBefore={} refId={}",
				clientId, objectRef, refCountAfter, refCountBefore, refId);
	}

	public synchronized void decRefCount(final Object object, final Uid refId) {
		requireNonNull(object, "object");
		requireNonNull(refId, "refId");

		int refCountBefore = 0;
		int refCountAfter = 0;

		final ObjectRef objectRef = getObjectRefOrFail(object);
		final Set<Uid> refIds = objectRef2RefIds.get(objectRef);
		if (refIds != null) {
			refCountBefore = refIds.size();
			refIds.remove(refId);
			refCountAfter = refIds.size();

			if (refIds.isEmpty()) {
				objectRef2RefIds.remove(objectRef);
				zeroReferenceObjectRef2Timestamp.put(objectRef, System.currentTimeMillis());
			}
		}
		logger.trace("[{}].decRefCount: {} refCountAfter={} refCountBefore={} refId={}",
				clientId, objectRef, refCountAfter, refCountBefore, refId);
	}

	private static void assertNotInstanceOfObjectRef(final Object object) {
		if (object instanceof ObjectRef)
			throw new IllegalArgumentException("object is an instance of ObjectRef! " + object);
	}

	public RemoteObjectProxyManager getRemoteObjectProxyManager() {
		return remoteObjectProxyManager;
	}

	public ClassManager getClassManager() {
		return classManager;
	}

	public static boolean isObjectRefMappingEnabled(final Object object) {
		if (object == null)
			return false;

		if (object instanceof ObjectRef)
			return false;

		final Class<?> clazz = getClassOrArrayComponentType(object);

		if (Proxy.isProxyClass(clazz))
			return true;

		if (Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz)) // a collection can be modified on the server-side - we want this to be reflected on the client-side, hence we proxy it
			return true;

		if (object instanceof Serializable)
			return false;

		return true;
	}

	private static Class<?> getClassOrArrayComponentType(final Object object) {
		final Class<?> clazz = object.getClass();
		if (clazz.isArray())
			return clazz.getComponentType();
		else
			return clazz;
	}

	public ReferenceJanitorRegistry getReferenceCleanerRegistry() {
		return referenceJanitorRegistry;
	}

	public synchronized boolean isClosed() {
		return closed;
	}

	protected synchronized void assertNotClosed() {
		if (closed)
			throw new IllegalStateException(String.format("ObjectManager[%s] is closed!", clientId));
	}

	public void close() {
		synchronized (this) {
			if (closed)
				return;

			closed = true;
		}
		synchronized (ObjectManager.class) {
			clientId2ObjectManager.remove(clientId);
		}
		referenceJanitorRegistry.cleanUp();
	}
}
