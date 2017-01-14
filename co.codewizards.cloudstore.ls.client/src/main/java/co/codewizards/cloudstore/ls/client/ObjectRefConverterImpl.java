package co.codewizards.cloudstore.ls.client;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRefConverter;
import co.codewizards.cloudstore.ls.core.invoke.RemoteObjectProxy;

class ObjectRefConverterImpl implements ObjectRefConverter {

	private final LocalServerClient localServerClient;
	private final ObjectManager objectManager;

	public ObjectRefConverterImpl(final LocalServerClient localServerClient) {
		this.localServerClient = assertNotNull(localServerClient, "localServerClient");
		this.objectManager = assertNotNull(localServerClient.getObjectManager(), "localServerClient.objectManager");
	}

	@Override
	public Object convertToObjectRefIfNeeded(Object object) {
		if (object instanceof RemoteObjectProxy)
			return assertNotNull(((RemoteObjectProxy)object).getObjectRef(), "object.getObjectRef()");
		else
			return objectManager.getObjectRefOrObject(object);
	}

	@Override
	public Object convertFromObjectRefIfNeeded(Object object) {
		if (object instanceof ObjectRef) {
			final ObjectRef objectRef = (ObjectRef) object;
			if (objectManager.getClientId().equals(objectRef.getClientId()))
				return objectManager.getObjectOrFail(objectRef);
			else // the reference is a remote object from the client-side => lookup or create proxy
				return localServerClient.getRemoteObjectProxyOrCreate(objectRef);
		} else
			return object;
	}

}
