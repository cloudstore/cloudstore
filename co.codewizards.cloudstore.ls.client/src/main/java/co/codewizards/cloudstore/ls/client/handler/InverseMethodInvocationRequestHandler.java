package co.codewizards.cloudstore.ls.client.handler;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;
import co.codewizards.cloudstore.ls.core.invoke.ClassManager;
import co.codewizards.cloudstore.ls.core.invoke.InverseMethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.InverseMethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.InvocationType;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;

public class InverseMethodInvocationRequestHandler extends AbstractInverseServiceRequestHandler<InverseMethodInvocationRequest, InverseMethodInvocationResponse> {

	private ObjectManager objectManager;

	@Override
	public InverseMethodInvocationResponse handle(final InverseMethodInvocationRequest request) {
		assertNotNull("request", request);
		MethodInvocationRequest methodInvocationRequest = request.getMethodInvocationRequest();

		objectManager = getLocalServerClient().getObjectManager();
		final ClassManager classManager = objectManager.getClassManager();

		final String className = methodInvocationRequest.getClassName();
		final Class<?> clazz = className == null ? null : classManager.getClassOrFail(className);

		final ObjectRef objectRef = methodInvocationRequest.getObjectRef();
		final Object object = objectRef == null ? null : objectManager.getObjectOrFail(objectRef);

		final String[] argumentTypeNames = methodInvocationRequest.getArgumentTypeNames();
		final Class<?>[] argumentTypes = argumentTypeNames == null ? null : classManager.getClassesOrFail(argumentTypeNames);

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

		return new InverseMethodInvocationResponse(request, MethodInvocationResponse.forInvocation(resultObjectOrObjectRef));
	}

	private Object[] fromObjectRefsToObjects(final Object[] objects) {
		if (objects == null)
			return objects;

		final Object[] result = new Object[objects.length];
		for (int i = 0; i < objects.length; i++) {
			final Object object = objects[i];
			if (object instanceof ObjectRef) {
				final ObjectRef objectRef = (ObjectRef) object;
				if (objectManager.getClientId().equals(objectRef.getClientId()))
					result[i] = objectManager.getObjectOrFail(objectRef);
				else // the reference is a remote object from the client-side => lookup or create proxy
					result[i] = getLocalServerClient().getRemoteObjectProxyOrCreate(objectRef);
			} else
				result[i] = object;
		}
		return result;
	}
}
