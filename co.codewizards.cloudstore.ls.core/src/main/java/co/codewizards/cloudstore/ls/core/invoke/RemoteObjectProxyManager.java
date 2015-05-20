package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteObjectProxyManager {

	private static final Logger logger = LoggerFactory.getLogger(RemoteObjectProxyManager.class);

	// TODO need to purge orphaned keys! Use ReferenceQueue in WeakReference!
	private final Map<ObjectRef, WeakReference<RemoteObjectProxy>> objectRef2RemoteObjectProxyRef = new HashMap<>();

	protected RemoteObjectProxyManager() {
		if (logger.isDebugEnabled())
			logger.debug("[{}]<init>", getThisId());
	}

	public synchronized RemoteObjectProxy getRemoteObjectProxyOrCreate(final ObjectRef objectRef) {
		assertNotNull("objectRef", objectRef);
		final WeakReference<RemoteObjectProxy> remoteObjectProxyRef = objectRef2RemoteObjectProxyRef.get(objectRef);
		final RemoteObjectProxy remoteObjectProxy = remoteObjectProxyRef == null ? null : remoteObjectProxyRef.get();
		return remoteObjectProxy;
	}

	public synchronized RemoteObjectProxy getRemoteObjectProxyOrCreate(final ObjectRef objectRef, final RemoteObjectProxyFactory remoteObjectProxyFactory) {
		assertNotNull("objectRef", objectRef);
		assertNotNull("remoteObjectProxyFactory", remoteObjectProxyFactory);

		final WeakReference<RemoteObjectProxy> remoteObjectProxyRef = objectRef2RemoteObjectProxyRef.get(objectRef);
		RemoteObjectProxy remoteObjectProxy = remoteObjectProxyRef == null ? null : remoteObjectProxyRef.get();

		if (remoteObjectProxy == null) {
			if (logger.isDebugEnabled())
				logger.debug("[{}]getRemoteObjectProxy: Creating proxy for {}. remoteObjectProxyRef={}", getThisId(), objectRef, remoteObjectProxyRef);

			// We do not need to create the proxy outside of the synchronized block, anymore, because the proxy creation now works without
			// immediate inverse-invocation and thus there's no more risk of a deadlock, here. => stay inside single big synchronized-block.
			remoteObjectProxy = remoteObjectProxyFactory.createRemoteObjectProxy(objectRef);
			assertNotNull("remoteObjectProxyFactory.createRemoteObjectProxy(objectRef)", remoteObjectProxy);

			objectRef2RemoteObjectProxyRef.put(objectRef, new WeakReference<>(remoteObjectProxy));
		}
		return remoteObjectProxy;
	}

	private String getThisId() {
		return Integer.toHexString(System.identityHashCode(this));
	}
}
