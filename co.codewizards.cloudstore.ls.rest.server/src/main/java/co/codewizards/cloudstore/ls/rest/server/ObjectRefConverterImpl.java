package co.codewizards.cloudstore.ls.rest.server;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRefConverter;
import co.codewizards.cloudstore.ls.core.invoke.RemoteObjectProxy;

class ObjectRefConverterImpl implements ObjectRefConverter {

	private final ObjectManager objectManager;
	private InverseInvoker inverseInvoker;

	public ObjectRefConverterImpl(final ObjectManager objectManager) {
		this.objectManager = assertNotNull("objectManager", objectManager);
	}

	@Override
	public Object convertToObjectRefIfNeeded(Object object) {
		if (object instanceof RemoteObjectProxy)
			return assertNotNull("object.getObjectRef()", ((RemoteObjectProxy)object).getObjectRef());
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
				return getInverseInvoker().getRemoteObjectProxyOrCreate(objectRef);
		} else
			return object;
	}

	protected InverseInvoker getInverseInvoker() {
		if (inverseInvoker == null)
			inverseInvoker = InverseInvoker.getInverseInvoker(objectManager);

		return inverseInvoker;
	}
}
