package co.codewizards.cloudstore.ls.core.invoke;

import static java.util.Objects.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.Uid;

public class RemoteObjectProxyInvocationHandler implements InvocationHandler {

	private static final Logger logger = LoggerFactory.getLogger(RemoteObjectProxyInvocationHandler.class);

	protected final Uid refId = new Uid();
	protected final Invoker invoker;
	protected final ObjectRef objectRef;
	protected final boolean equalsOverridden;

	public RemoteObjectProxyInvocationHandler(final Invoker invoker, final ObjectRef objectRef) {
		this.invoker = requireNonNull(invoker, "invoker");
		this.objectRef = requireNonNull(objectRef, "objectRef");

		if (logger.isDebugEnabled())
			logger.debug("[{}]<init>: {} refId={}", getThisId(), objectRef, refId);

		equalsOverridden = invoker.getClassInfoMap().getClassInfoOrFail(objectRef.getClassId()).isEqualsOverridden();
		invoker.incRefCount(objectRef, refId);
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		// BEGIN implement RemoteObjectProxy
		if ("getObjectRef".equals(method.getName()) && method.getParameterTypes().length == 0)
			return objectRef;
		// END implement RemoteObjectProxy

		// BEGIN equals(...) + hashCode()
		if (! equalsOverridden) {
			if ("equals".equals(method.getName()) && method.getParameterTypes().length == 1)
				return _equals(proxy, method, args[0]);

			if ("hashCode".equals(method.getName()) && method.getParameterTypes().length == 0)
				return _hashCode(proxy, method);
		}
		// END equals(...) + hashCode()

		if (logger.isDebugEnabled())
			logger.debug("[{}]invoke: method='{}'", getThisId(), method);

		return invoker.invoke(objectRef, method.getName(), method.getParameterTypes(), args);
	}

	@Override
	protected void finalize() throws Throwable {
		if (logger.isDebugEnabled())
			logger.debug("[{}]finalize: {}", getThisId(), objectRef);

		invoker.decRefCount(objectRef, refId);
		super.finalize();
	}

	private Object _equals(final Object proxy, final Method method, final Object other) {
		if (proxy == other)
			return true;

		if (null == other)
			return false;

		if (proxy.getClass() != other.getClass())
			return false;

		final RemoteObjectProxyInvocationHandler otherHandler = (RemoteObjectProxyInvocationHandler) Proxy.getInvocationHandler(other);
		return this.objectRef.equals(otherHandler.objectRef);
	}

	private int _hashCode(final Object proxy, final Method method) {
		return 31 * objectRef.hashCode();
	}

	protected String getThisId() {
		return Integer.toHexString(System.identityHashCode(this));
	}
}
