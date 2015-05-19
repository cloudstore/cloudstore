package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;

public abstract class AbstractRemoteObjectProxyInvocationHandler implements InvocationHandler {

	private static final Logger logger = LoggerFactory.getLogger(AbstractRemoteObjectProxyInvocationHandler.class);

	protected final Uid refId = new Uid();
	protected final ObjectRef objectRef;

	public AbstractRemoteObjectProxyInvocationHandler(final ObjectRef objectRef) {
		this.objectRef = assertNotNull("objectRef", objectRef);

		if (logger.isDebugEnabled())
			logger.debug("[{}]<init>: {}", getThisId(), objectRef);
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		// BEGIN implement RemoteObjectProxy
		if ("getObjectRef".equals(method.getName()) && method.getParameterTypes().length == 0)
			return objectRef;

		// TODO we must somehow support delegating equals and hashCode to the real object - but this should
		// only happen, if either the method is overridden (can we find this out?) or if there is a special annotation.

		if ("equals".equals(method.getName()) && method.getParameterTypes().length == 1)
			return _equals(proxy, method, args[0]);

		if ("hashCode".equals(method.getName()) && method.getParameterTypes().length == 0)
			return _hashCode(proxy, method);
		// END implement RemoteObjectProxy

		if (logger.isDebugEnabled())
			logger.debug("[{}]invoke: method='{}'", getThisId(), method);

		return doInvoke(proxy, method, args);
	}

	protected abstract Object doInvoke(final Object proxy, final Method method, final Object[] args) throws Throwable;

	private Object _equals(final Object proxy, final Method method, final Object other) {
		if (proxy == other)
			return true;

		if (null == other)
			return false;

		if (proxy.getClass() != other.getClass())
			return false;

		final AbstractRemoteObjectProxyInvocationHandler otherHandler = (AbstractRemoteObjectProxyInvocationHandler) Proxy.getInvocationHandler(other);
		return this.objectRef.equals(otherHandler.objectRef);
	}

	private int _hashCode(final Object proxy, final Method method) {
		return 31 * objectRef.hashCode();
	}

	protected String getThisId() {
		return Integer.toHexString(System.identityHashCode(this));
	}
}
