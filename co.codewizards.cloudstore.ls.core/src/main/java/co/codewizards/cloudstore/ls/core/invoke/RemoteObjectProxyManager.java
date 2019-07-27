package co.codewizards.cloudstore.ls.core.invoke;

import static java.util.Objects.*;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteObjectProxyManager {

	private static final Logger logger = LoggerFactory.getLogger(RemoteObjectProxyManager.class);

	private final Map<ObjectRef, WeakReference<RemoteObjectProxy>> objectRef2RemoteObjectProxyRef = new HashMap<>();
	private final Map<WeakReference<RemoteObjectProxy>, ObjectRef> remoteObjectProxyRef2ObjectRef = new IdentityHashMap<>();
	private final ReferenceQueue<RemoteObjectProxy> referenceQueue = new ReferenceQueue<RemoteObjectProxy>();

	protected RemoteObjectProxyManager() {
		if (logger.isDebugEnabled())
			logger.debug("[{}]<init>", getThisId());
	}

	public synchronized RemoteObjectProxy getRemoteObjectProxy(final ObjectRef objectRef) {
		requireNonNull(objectRef, "objectRef");
		final WeakReference<RemoteObjectProxy> remoteObjectProxyRef = objectRef2RemoteObjectProxyRef.get(objectRef);
		final RemoteObjectProxy remoteObjectProxy = remoteObjectProxyRef == null ? null : remoteObjectProxyRef.get();
		evictOrphanedObjectRefs();
		return remoteObjectProxy;
	}

	public synchronized RemoteObjectProxy getRemoteObjectProxyOrCreate(final ObjectRef objectRef, final RemoteObjectProxyFactory remoteObjectProxyFactory) {
		requireNonNull(objectRef, "objectRef");
		requireNonNull(remoteObjectProxyFactory, "remoteObjectProxyFactory");

		final WeakReference<RemoteObjectProxy> remoteObjectProxyRef = objectRef2RemoteObjectProxyRef.get(objectRef);
		RemoteObjectProxy remoteObjectProxy = remoteObjectProxyRef == null ? null : remoteObjectProxyRef.get();

		if (remoteObjectProxy == null) {
			if (logger.isDebugEnabled())
				logger.debug("[{}]getRemoteObjectProxy: Creating proxy for {}. remoteObjectProxyRef={}", getThisId(), objectRef, remoteObjectProxyRef);

			// We do not need to create the proxy outside of the synchronized block, anymore, because the proxy creation now works without
			// immediate inverse-invocation and thus there's no more risk of a deadlock, here. => stay inside single big synchronized-block.
			remoteObjectProxy = remoteObjectProxyFactory.createRemoteObjectProxy(objectRef);
			requireNonNull(remoteObjectProxy, "remoteObjectProxyFactory.createRemoteObjectProxy(objectRef)");

			final WeakReference<RemoteObjectProxy> reference = new WeakReference<>(remoteObjectProxy, referenceQueue);
			objectRef2RemoteObjectProxyRef.put(objectRef, reference);
			remoteObjectProxyRef2ObjectRef.put(reference, objectRef);
		}
		evictOrphanedObjectRefs();
		return remoteObjectProxy;
	}

	private String getThisId() {
		return Integer.toHexString(System.identityHashCode(this));
	}

	private synchronized void evictOrphanedObjectRefs() {
		Reference<? extends RemoteObjectProxy> reference;
		while (null != (reference = referenceQueue.poll())) {
			final ObjectRef objectRef = remoteObjectProxyRef2ObjectRef.remove(reference);
			objectRef2RemoteObjectProxyRef.remove(objectRef);
		}
	}
}
