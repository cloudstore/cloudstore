package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;

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
	private static final long EVICT_UNUSED_OBJECT_MANAGER_TIMEOUT_MS = 5 * 60 * 1000L; // 5 minutes
	private static final long EVICT_UNUSED_OBJECT_MANAGER_PERIOD_MS = 60 * 1000L;

	private static final long EVICT_ZERO_REFERENCE_OBJECT_REFS_TIMEOUT_MS = 30 * 1000L; // 30 seconds
	private static final long EVICT_ZERO_REFERENCE_OBJECT_REFS_PERIOD_MS = 5 * 1000L;

	private static final Logger logger = LoggerFactory.getLogger(ObjectManager.class);

	private final Uid clientId;

	private long nextObjectId;

	private volatile Date lastUseDate;
	private volatile boolean neverEvict;

	private final Map<ObjectRef, Object> objectRef2Object = new HashMap<>();
	private final Map<Object, ObjectRef> object2ObjectRef = new IdentityHashMap<>();
	private final Map<String, Object> contextObjectMap = new HashMap<>();

	private final Map<ObjectRef, Long> zeroReferenceObjectRef2Timestamp = new HashMap<>();
	private final Map<ObjectRef, Set<Uid>> objectRef2RefIds = new HashMap<>();

	private final RemoteObjectProxyManager remoteObjectProxyManager = new RemoteObjectProxyManager();
	private final ClassManager classManager = new ClassManager();

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
		timer.schedule(timerTask, 1000L, 1000L);
	}

	public static synchronized ObjectManager getInstance(final Uid clientId) {
		assertNotNull("clientId", clientId);
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

	private static synchronized void evictOldObjectManagers() {
		final long now = System.currentTimeMillis();

		if (evictOldObjectManagersLastInvocation >= now - EVICT_UNUSED_OBJECT_MANAGER_PERIOD_MS)
			return;

		evictOldObjectManagersLastInvocation = now;

		for (Iterator<ObjectManager> it = clientId2ObjectManager.values().iterator(); it.hasNext();) {
			final ObjectManager objectManager = it.next();

			if (objectManager.isNeverEvict())
				continue;

			if (objectManager.getLastUseDate().getTime() < now - EVICT_UNUSED_OBJECT_MANAGER_TIMEOUT_MS)
				it.remove();
		}
	}

	private static synchronized List<ObjectManager> getObjectManagers() {
		final List<ObjectManager> objectManagers = new ArrayList<ObjectManager>(clientId2ObjectManager.values());
		return objectManagers;
	}

	private static void allObjectManagers_evictZeroReferenceObjectRefs() {
		final long now = System.currentTimeMillis();

		if (evictZeroReferenceObjectRefsLastInvocation >= now - EVICT_ZERO_REFERENCE_OBJECT_REFS_PERIOD_MS)
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
		assertNotNull("clientId", clientId);
		this.clientId = clientId;
	}

	protected Date getLastUseDate() {
		return lastUseDate;
	}
	private void updateLastUseDate() {
		this.lastUseDate = new Date();
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
			objectRef2Object.put(objectRef, object);
			object2ObjectRef.put(object, objectRef);
			zeroReferenceObjectRef2Timestamp.put(objectRef, System.currentTimeMillis());
		}
		return objectRef;
	}

	public synchronized ObjectRef getObjectRefOrFail(final Object object) {
		final ObjectRef objectRef = getObjectRef(object);
		if (objectRef == null)
			throw new IllegalArgumentException("There is no ObjectRef known for this object: " + object);

		return objectRef;
	}

	public synchronized ObjectRef getObjectRef(final Object object) {
		assertNotNull("object", object);
		assertNotInstanceOfObjectRef(object);
		final ObjectRef objectRef = object2ObjectRef.get(object);
		updateLastUseDate();
		return objectRef;
	}

	public synchronized Object getObjectOrFail(final ObjectRef objectRef) {
		final Object object = getObject(objectRef);
		if (object == null)
			throw new IllegalArgumentException("There is no object known for this ObjectRef: " + objectRef);

		return object;
	}

	public synchronized Object getObject(final ObjectRef objectRef) {
		assertNotNull("objectRef", objectRef);
		final Object object = objectRef2Object.get(objectRef);
		updateLastUseDate();
		return object;
	}

	private synchronized void remove(final ObjectRef objectRef) {
		assertNotNull("objectRef", objectRef);

		if (!objectRef2Object.containsKey(objectRef))
			throw new IllegalStateException("!objectRef2Object.containsKey(objectRef): " + objectRef);

		zeroReferenceObjectRef2Timestamp.remove(objectRef);
		final Object object = objectRef2Object.remove(objectRef);
		object2ObjectRef.remove(object);
		updateLastUseDate();
	}

	public synchronized void incRefCount(final Object object, final Uid refId) {
		assertNotNull("object", object);
		assertNotNull("refId", refId);
		final ObjectRef objectRef = getObjectRefOrFail(object);
		if (zeroReferenceObjectRef2Timestamp.remove(objectRef) != null) {
			if (objectRef2RefIds.put(objectRef, new HashSet<Uid>(Collections.singleton(refId))) != null)
				throw new IllegalStateException("Collision! WTF?!");
		}
		else {
			final Set<Uid> refIds = objectRef2RefIds.get(objectRef);
			assertNotNull("objectRef2RefIds.get(" + objectRef + ")", refIds);
			refIds.add(refId);
		}
		classManager.setClassIdKnownByRemoteSide(objectRef.getClassId());
	}

	public synchronized void decRefCount(final Object object, final Uid refId) {
		assertNotNull("object", object);
		assertNotNull("refId", refId);
		final ObjectRef objectRef = getObjectRefOrFail(object);
		final Set<Uid> refIds = objectRef2RefIds.get(objectRef);
		if (refIds == null)
			return;

		refIds.remove(refId);

		if (refIds.isEmpty()) {
			objectRef2RefIds.remove(objectRef);
			zeroReferenceObjectRef2Timestamp.put(objectRef, System.currentTimeMillis());
		}
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

	public boolean isObjectRefMappingEnabled(final Object object) {
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

	private Class<?> getClassOrArrayComponentType(final Object object) {
		final Class<?> clazz = object.getClass();
		if (clazz.isArray())
			return clazz.getComponentType();
		else
			return clazz;
	}
}
