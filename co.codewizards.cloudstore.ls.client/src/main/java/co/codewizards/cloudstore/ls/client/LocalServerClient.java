package co.codewizards.cloudstore.ls.client;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import co.codewizards.cloudstore.ls.core.remoteobject.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.remoteobject.MethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.remoteobject.ObjectRef;
import co.codewizards.cloudstore.ls.core.remoteobject.RemoteObject;
import co.codewizards.cloudstore.ls.rest.client.LocalServerRestClient;
import co.codewizards.cloudstore.ls.rest.client.request.InvokeMethod;

public class LocalServerClient {

	private final LocalServerRestClient localServerRestClient = LocalServerRestClient.getInstance();
//	private final ObjectManager objectManager = ObjectManager.getInstance(); // needed for inverse references as used by listeners! later...

	private static final class Holder {
		public static final LocalServerClient instance = new LocalServerClient();
	}

	public static LocalServerClient getInstance() {
		return Holder.instance;
	}

	protected LocalServerRestClient getLocalServerRestClient() {
		return localServerRestClient;
	}

	private final Map<ObjectRef, RemoteObject> objectRef2RemoteObject = new HashMap<>();

	protected LocalServerClient() {
	}

	public <T> T invokeStatic(final Class<?> clazz, final String methodName, final Object ... arguments) {
		assertNotNull("clazz", clazz);
		assertNotNull("methodName", methodName);
		return invokeStatic(clazz.getName(), methodName, arguments);
	}

	public <T> T invokeStatic(final String className, final String methodName, final Object ... arguments) {
		assertNotNull("className", className);
		assertNotNull("methodName", methodName);
		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forStaticInvocation(
				className, methodName, fromObjectsToObjectRefs(arguments));

		return invoke(methodInvocationRequest);
	}

	public <T> T invokeConstructor(final Class<?> clazz, final Object ... arguments) {
		assertNotNull("clazz", clazz);
		return invokeConstructor(clazz.getName(), arguments);
	}

	public <T> T invokeConstructor(final String className, final Object ... arguments) {
		assertNotNull("className", className);
		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forConstructorInvocation(
				className, fromObjectsToObjectRefs(arguments));

		return invoke(methodInvocationRequest);
	}

	public <T> T invoke(final Object object, final String methodName, final Object ... arguments) {
		assertNotNull("object", object);
		assertNotNull("methodName", methodName);

		if (!(object instanceof RemoteObject))
			throw new IllegalArgumentException("object is not an instance of RemoteObject!");

		final ObjectRef objectRef = assertNotNull("object.getObjectRef()", ((RemoteObject)object).getObjectRef());
		return invoke(objectRef, methodName, (Class<?>[]) null, arguments);
	}

	private <T> T invoke(final ObjectRef objectRef, final String methodName, final Class<?>[] argumentTypes, final Object[] arguments) {
		assertNotNull("objectRef", objectRef);
		assertNotNull("methodName", methodName);

		final String[] argumentTypeNames;
		if (argumentTypes == null)
			argumentTypeNames = null;
		else {
			argumentTypeNames = new String[argumentTypes.length];
			for (int i = 0; i < argumentTypes.length; i++)
				argumentTypeNames[i] = argumentTypes[i].getName();
		}

		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forObjectInvocation(
				objectRef, methodName, argumentTypeNames, fromObjectsToObjectRefs(arguments));

		return invoke(methodInvocationRequest);
	}

	private Object[] fromObjectsToObjectRefs(final Object[] objects) {
		if (objects == null)
			return objects;

		final Object[] result = new Object[objects.length];
		for (int i = 0; i < objects.length; i++) {
			final Object object = objects[i];
			if (object instanceof RemoteObject) {
				result[i] = assertNotNull("object.getObjectRef()", ((RemoteObject)object).getObjectRef());
			} else
				result[i] = object;
		}
		return result;
	}

	private <T> T invoke(final MethodInvocationRequest methodInvocationRequest) {
		assertNotNull("methodInvocationRequest", methodInvocationRequest);

		final MethodInvocationResponse methodInvocationResponse = localServerRestClient.execute(
				new InvokeMethod(methodInvocationRequest));

		final Object result = methodInvocationResponse.getResult();
		if (result == null)
			return null;

		final Class<?>[] interfaces = getInterfaces(methodInvocationResponse);
		if (result instanceof ObjectRef) {
			final ObjectRef resultObjectRef = (ObjectRef) result;
			return cast(getRemoteObjectProxy(resultObjectRef, interfaces));
		}

		return cast(result);
	}

	private synchronized RemoteObject getRemoteObjectProxy(final ObjectRef objectRef, final Class<?>[] interfaces) {
		assertNotNull("objectRef", objectRef);
		assertNotNull("interfaces", interfaces);

		RemoteObject remoteObject = objectRef2RemoteObject.get(objectRef);
		if (remoteObject == null) {
			remoteObject = createRemoteObjectProxy(objectRef, interfaces);
			objectRef2RemoteObject.put(objectRef, remoteObject);
		}
		return remoteObject;
	}

	private RemoteObject createRemoteObjectProxy(final ObjectRef objectRef, final Class<?>[] interfaces) {
		final ClassLoader classLoader = this.getClass().getClassLoader();
		return (RemoteObject) Proxy.newProxyInstance(classLoader, interfaces, new InvocationHandler() {
			@Override
			public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
				// BEGIN implement RemoteObject
				if ("getObjectRef".equals(method.getName()) && method.getParameterTypes().length == 0)
					return objectRef;
				// END implement RemoteObject

//				if ("equals".equals(method.getName()) && method.getParameterTypes().length == 1) {
//					return
//				}

				return LocalServerClient.this.invoke(objectRef, method.getName(), method.getParameterTypes(), args);
			}
		});
	}

	private Class<?>[] getInterfaces(final MethodInvocationResponse methodInvocationResponse) {
		assertNotNull("methodInvocationResponse", methodInvocationResponse);

		// TODO we should *not* expect to know the class in the client! we should query the interfaces from the server
		// and implement only all those interfaces that are available on the client side!
		final Class<?> resultClass;
		try {
			resultClass = Class.forName(methodInvocationResponse.getResultClassName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		final Set<Class<?>> interfaces = getAllInterfaces(resultClass);
		interfaces.add(RemoteObject.class);
		return interfaces.toArray(new Class<?>[interfaces.size()]);
	}

}
