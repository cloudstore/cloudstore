package co.codewizards.cloudstore.ls.core.remoteobject;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;

public class ObjectManager {
	private static final long EVICT_AGE_MS = 15 * 60 * 1000L; // 15 minutes

	private static final Logger logger = LoggerFactory.getLogger(ObjectManager.class);

	private final Uid clientId;

	private long nextObjectId = 0;

	private volatile Date lastUseDate;

	private static final Map<Uid, ObjectManager> clientId2ObjectManager = new HashMap<>();

	private static final Timer timer = new Timer(true);
	private static final TimerTask timerTask = new TimerTask() {
		@Override
		public void run() {
			try {
				evictOldObjectManagers();
			} catch (Exception x) {
				logger.error("run: " + x, x);
			}
		}
	};
	static {
		timer.schedule(timerTask, 60000L, 60000L);
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

	private static synchronized void evictOldObjectManagers() {
		for (Iterator<ObjectManager> it = clientId2ObjectManager.values().iterator(); it.hasNext();) {
			final ObjectManager objectManager = it.next();
			if (objectManager.getLastUseDate().getTime() < System.currentTimeMillis() - EVICT_AGE_MS)
				it.remove();
		}
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

	/**
	 * Gets the id of the client using this {@code ObjectManager}. This is either the remote client talking to a server
	 * or it is the server (when the remote client holds references e.g. to listeners or other callbacks for the server).
	 * @return the id of the client.
	 */
	public Uid getClientId() {
		return clientId;
	}

	protected synchronized ObjectRef createObjectRef() {
		return new ObjectRef(clientId, nextObjectId++);
	}

	private final Map<ObjectRef, Object> objectRef2Object = new HashMap<>();
	private final Map<Object, ObjectRef> object2ObjectRef = new IdentityHashMap<>();

	public synchronized Object getObjectRefOrObject(final Object object) {
		if (isObjectRefMappingEnabled(object))
			return getObjectRefOrCreate(object);
		else
			return object;
	}

	public synchronized ObjectRef getObjectRefOrCreate(final Object object) {
		ObjectRef objectRef = getObjectRef(object);
		if (objectRef == null) {
			objectRef = createObjectRef();
			objectRef2Object.put(objectRef, object);
			object2ObjectRef.put(object, objectRef);
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

	public synchronized void remove(final ObjectRef objectRef) {
		assertNotNull("objectRef", objectRef);
		final Object object = objectRef2Object.remove(objectRef);
		object2ObjectRef.remove(object);
		updateLastUseDate();
	}

	public synchronized void remove(final Object object) {
		assertNotNull("object", object);
		assertNotInstanceOfObjectRef(object);
		final ObjectRef objectRef = object2ObjectRef.remove(object);
		objectRef2Object.remove(objectRef);
		updateLastUseDate();
	}

	private static void assertNotInstanceOfObjectRef(final Object object) {
		if (object instanceof ObjectRef)
			throw new IllegalArgumentException("object is an instance of ObjectRef! " + object);
	}

	public boolean isObjectRefMappingEnabled(final Object object) { // TODO maybe use annotations or a meta-data-service?! or both?!
		if (object == null)
			return false;

		if (Proxy.isProxyClass(object.getClass()))
			return true;

		if (object instanceof Serializable)
			return false;

		return true;
	}
}
