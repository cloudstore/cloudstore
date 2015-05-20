package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;

public class RemoteObjectProxyInvocationHandler implements InvocationHandler {

	private static final Logger logger = LoggerFactory.getLogger(RemoteObjectProxyInvocationHandler.class);

	protected final Uid refId = new Uid();
	protected final Invoker invoker;
	protected final ObjectRef objectRef;

	public RemoteObjectProxyInvocationHandler(final Invoker invoker, final ObjectRef objectRef) {
		this.invoker = assertNotNull("invoker", invoker);
		this.objectRef = assertNotNull("objectRef", objectRef);

		if (logger.isDebugEnabled())
			logger.debug("[{}]<init>: {} refId={}", getThisId(), objectRef, refId);

		// TODO make bulk operation (collect multiple refIds) or at least use a ThreadPool in Invoker.
//		new Thread() {
//			@Override
//			public void run() {
//			}
//		}.start();
//		invoker.invoke(object, Object.VIRTUAL_METHOD_NAME_INC_REF_COUNT, (Class<?>[])null, new Object[] { refId });
		invoker.incRefCount(objectRef, refId);
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

		return invoker.invoke(objectRef, method.getName(), method.getParameterTypes(), args);
	}

	@Override
	protected void finalize() throws Throwable {
		if (logger.isDebugEnabled())
			logger.debug("[{}]finalize: {}", getThisId(), objectRef);

//		try {
//			invoker.invoke(object, Object.VIRTUAL_METHOD_NAME_DEC_REF_COUNT, (Class<?>[])null, new Object[] { refId });
//		} catch (Exception x) {
//			logger.warn("[" + getThisId() + "]finalize: " + x, x);
//		}
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
