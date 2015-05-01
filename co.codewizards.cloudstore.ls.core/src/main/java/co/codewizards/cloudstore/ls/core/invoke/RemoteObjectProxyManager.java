package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.HashMap;
import java.util.Map;

public class RemoteObjectProxyManager {

	private final Map<ObjectRef, RemoteObject> objectRef2RemoteObject = new HashMap<>();

	protected RemoteObjectProxyManager() { }

	public synchronized RemoteObject getRemoteObjectProxy(final ObjectRef objectRef, final RemoteObjectProxyFactory remoteObjectProxyFactory) {
		assertNotNull("objectRef", objectRef);
		assertNotNull("remoteObjectProxyFactory", remoteObjectProxyFactory);

		RemoteObject remoteObject = objectRef2RemoteObject.get(objectRef);
		if (remoteObject == null) {
			remoteObject = remoteObjectProxyFactory.createRemoteObject(objectRef);
			assertNotNull("remoteObjectProxyFactory.createRemoteObject(objectRef)", remoteObject);
			objectRef2RemoteObject.put(objectRef, remoteObject);
		}
		return remoteObject;
	}
}
