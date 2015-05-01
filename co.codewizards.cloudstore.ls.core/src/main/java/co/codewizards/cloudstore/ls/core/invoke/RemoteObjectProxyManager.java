package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class RemoteObjectProxyManager {

	private final Map<ObjectRef, WeakReference<RemoteObjectProxy>> objectRef2RemoteObjectProxyRef = new HashMap<>();

	protected RemoteObjectProxyManager() { }

	public synchronized RemoteObjectProxy getRemoteObjectProxy(final ObjectRef objectRef, final RemoteObjectProxyFactory remoteObjectProxyFactory) {
		assertNotNull("objectRef", objectRef);
		assertNotNull("remoteObjectProxyFactory", remoteObjectProxyFactory);

		final WeakReference<RemoteObjectProxy> remoteObjectProxyRef = objectRef2RemoteObjectProxyRef.get(objectRef);
		RemoteObjectProxy remoteObjectProxy = remoteObjectProxyRef == null ? null : remoteObjectProxyRef.get();
		if (remoteObjectProxy == null) {
			remoteObjectProxy = remoteObjectProxyFactory.createRemoteObject(objectRef);
			assertNotNull("remoteObjectProxyFactory.createRemoteObject(objectRef)", remoteObjectProxy);
			objectRef2RemoteObjectProxyRef.put(objectRef, new WeakReference<>(remoteObjectProxy));
		}
		return remoteObjectProxy;
	}
}
