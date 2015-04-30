package co.codewizards.cloudstore.ls.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import co.codewizards.cloudstore.ls.core.provider.MediaTypeConst;
import co.codewizards.cloudstore.ls.core.remoteobject.InvocationType;
import co.codewizards.cloudstore.ls.core.remoteobject.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.remoteobject.MethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.remoteobject.ObjectManager;
import co.codewizards.cloudstore.ls.core.remoteobject.ObjectRef;

@Path("InvokeMethod")
@Consumes(MediaTypeConst.APPLICATION_JAVA_NATIVE)
@Produces(MediaTypeConst.APPLICATION_JAVA_NATIVE)
public class InvokeMethodService extends AbstractService {

	private ObjectManager objectManager;

	@POST
	public MethodInvocationResponse performMethodInvocation(final MethodInvocationRequest methodInvocationRequest) {
		assertNotNull("methodInvocationRequest", methodInvocationRequest);

		objectManager = getObjectManager(); // *always* acquiring to make sure the lastUseDate is updated.

		final String className = methodInvocationRequest.getClassName();
		final Class<?> clazz = className == null ? null : getClassOrFail(className);

		final ObjectRef objectRef = methodInvocationRequest.getObjectRef();
		final Object object = objectRef == null ? null : objectManager.getObjectOrFail(objectRef);

		final String[] argumentTypeNames = methodInvocationRequest.getArgumentTypeNames();
		final Class<?>[] argumentTypes = argumentTypeNames == null ? null : getClassesOrFail(argumentTypeNames);

		final Object[] arguments = fromObjectRefsToObjects(methodInvocationRequest.getArguments());

		final Object resultObject;

		final InvocationType invocationType = methodInvocationRequest.getInvocationType();
		switch (invocationType) {
			case CONSTRUCTOR:
				resultObject = invokeConstructor(clazz, arguments);
				break;
			case OBJECT:
				resultObject = invoke(object, methodInvocationRequest.getMethodName(), argumentTypes, arguments);
				break;
			case STATIC:
				resultObject = invokeStatic(clazz, methodInvocationRequest.getMethodName(), arguments);
				break;
			default:
				throw new IllegalStateException("Unknown InvocationType: " + invocationType);
		}

		final Object resultObjectOrObjectRef = objectManager.getObjectRefOrObject(resultObject);
		final Class<?> resultClass = resultObject == null ? null : resultObject.getClass();
		final String resultClassName = resultClass == null ? null : resultClass.getName();

		final MethodInvocationResponse result = MethodInvocationResponse.forInvocation(resultClassName, resultObjectOrObjectRef);
		return result;
	}

	private Object[] fromObjectRefsToObjects(final Object[] objects) {
		if (objects == null)
			return objects;

		final Object[] result = new Object[objects.length];
		for (int i = 0; i < objects.length; i++) {
			final Object object = objects[i];
			if (object instanceof ObjectRef) {
				result[i] = objectManager.getObjectOrFail((ObjectRef) object);
			} else
				result[i] = object;
		}
		return result;
	}

	private Class<?>[] getClassesOrFail(final String[] classNames) {
		assertNotNull("classNames", classNames);
		final Class<?>[] classes = new Class<?>[classNames.length];

		for (int i = 0; i < classNames.length; i++)
			classes[i] = getClassOrFail(classNames[i]);

		return classes;
	}

	private Class<?> getClassOrFail(final String className) {
		assertNotNull("className", className);
		// TODO maybe use context-class-loader, too and other loaders (which?)?

		final Class<?> clazz;
		try {
			clazz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
		return clazz;
	}
}
