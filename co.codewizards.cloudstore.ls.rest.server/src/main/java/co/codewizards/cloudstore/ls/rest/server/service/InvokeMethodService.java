package co.codewizards.cloudstore.ls.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import co.codewizards.cloudstore.ls.core.invoke.ClassManager;
import co.codewizards.cloudstore.ls.core.invoke.InvocationType;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;
import co.codewizards.cloudstore.ls.core.provider.MediaTypeConst;
import co.codewizards.cloudstore.ls.rest.server.InverseInvoker;

@Path("InvokeMethod")
@Consumes(MediaTypeConst.APPLICATION_JAVA_NATIVE_WITH_OBJECT_REF)
@Produces(MediaTypeConst.APPLICATION_JAVA_NATIVE_WITH_OBJECT_REF)
public class InvokeMethodService extends AbstractService {

	private InverseInvoker inverseInvoker;
	private ObjectManager objectManager;
	private ClassManager classManager;

	@POST
	public MethodInvocationResponse performMethodInvocation(final MethodInvocationRequest methodInvocationRequest) {
		assertNotNull("methodInvocationRequest", methodInvocationRequest);

		// *always* acquiring to make sure the lastUseDate is updated - and to make things easy: we have what we need.
		inverseInvoker = getInverseInvoker();
		objectManager = inverseInvoker.getObjectManager();
		classManager = objectManager.getClassManager();

		final String className = methodInvocationRequest.getClassName();
		final Class<?> clazz = className == null ? null : classManager.getClassOrFail(className);

		final String methodName = methodInvocationRequest.getMethodName();

		final Object object = methodInvocationRequest.getObject();
		if (ObjectRef.VIRTUAL_METHOD_NAME_REMOVE_OBJECT_REF.equals(methodName)) {
			objectManager.remove(object);
			return MethodInvocationResponse.forInvocation(null);
		}

//		final Object object = objectRef == null ? null : objectManager.getObjectOrFail(objectRef);

		final String[] argumentTypeNames = methodInvocationRequest.getArgumentTypeNames();
		final Class<?>[] argumentTypes = argumentTypeNames == null ? null : classManager.getClassesOrFail(argumentTypeNames);

//		final Object[] arguments = fromObjectRefsToObjects(methodInvocationRequest.getArguments());
		final Object[] arguments = methodInvocationRequest.getArguments();

		final Object resultObject;

		final InvocationType invocationType = methodInvocationRequest.getInvocationType();
		switch (invocationType) {
			case CONSTRUCTOR:
				resultObject = invokeConstructor(clazz, arguments);
				break;
			case OBJECT:
				resultObject = invoke(object, methodName, argumentTypes, arguments);
				break;
			case STATIC:
				resultObject = invokeStatic(clazz, methodName, arguments);
				break;
			default:
				throw new IllegalStateException("Unknown InvocationType: " + invocationType);
		}

//		final Object resultObjectOrObjectRef = objectManager.getObjectRefOrObject(resultObject);
//		final MethodInvocationResponse result = MethodInvocationResponse.forInvocation(resultObjectOrObjectRef);
//		return result;

		return MethodInvocationResponse.forInvocation(resultObject);
	}

//	private Object[] fromObjectRefsToObjects(final Object[] objects) {
//		if (objects == null)
//			return objects;
//
//		final Object[] result = new Object[objects.length];
//		for (int i = 0; i < objects.length; i++) {
//			final Object object = objects[i];
//			if (object instanceof ObjectRef) {
//				final ObjectRef objectRef = (ObjectRef) object;
//				if (objectManager.getClientId().equals(objectRef.getClientId()))
//					result[i] = objectManager.getObjectOrFail(objectRef);
//				else // the reference is a remote object from the client-side => lookup or create proxy
//					result[i] = inverseInvoker.getRemoteObjectProxyOrCreate(objectRef);
//			} else
//				result[i] = object;
//		}
//		return result;
//	}
}
