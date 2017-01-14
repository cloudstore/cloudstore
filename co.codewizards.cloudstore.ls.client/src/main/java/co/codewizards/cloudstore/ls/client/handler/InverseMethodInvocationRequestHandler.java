package co.codewizards.cloudstore.ls.client.handler;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.ls.core.invoke.ClassManager;
import co.codewizards.cloudstore.ls.core.invoke.InverseMethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.InverseMethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.InvokeMethodExecutor;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRefWithRefId;
import co.codewizards.cloudstore.ls.core.invoke.filter.ExtMethodInvocationRequest;

public class InverseMethodInvocationRequestHandler extends AbstractInverseServiceRequestHandler<InverseMethodInvocationRequest, InverseMethodInvocationResponse> {

	private static final InvokeMethodExecutor invokeMethodExecutor = new InvokeMethodExecutor();

	@Override
	public InverseMethodInvocationResponse handle(final InverseMethodInvocationRequest request) throws Exception {
		assertNotNull(request, "request");

		final MethodInvocationRequest methodInvocationRequest = request.getMethodInvocationRequest();
		if (methodInvocationRequest != null) {
			final MethodInvocationResponse response = performMethodInvocation(methodInvocationRequest);
			return new InverseMethodInvocationResponse(request, response);
		}

		final Uid delayedResponseId = request.getDelayedResponseId();
		if (delayedResponseId != null) {
			final MethodInvocationResponse response = getDelayedMethodInvocationResponse(delayedResponseId);
			return new InverseMethodInvocationResponse(request, response);
		}

		throw new IllegalArgumentException("request.methodInvocationRequest and request.delayedResponseId are both null!");
	}

	private MethodInvocationResponse performMethodInvocation(final MethodInvocationRequest methodInvocationRequest) throws Exception {
		assertNotNull(methodInvocationRequest, "methodInvocationRequest");

		final ObjectManager objectManager = getLocalServerClient().getObjectManager();
		final ClassManager classManager = objectManager.getClassManager();

		final String className = methodInvocationRequest.getClassName();
		final Class<?> clazz = className == null ? null : classManager.getClassOrFail(className);

		final String methodName = methodInvocationRequest.getMethodName();

		if (ObjectRef.VIRTUAL_METHOD_NAME_INC_REF_COUNT.equals(methodName)) {
			final ObjectRefWithRefId[] objectRefWithRefIds = cast(methodInvocationRequest.getArguments()[0]);
			for (final ObjectRefWithRefId objectRefWithRefId : objectRefWithRefIds)
				objectManager.incRefCount(objectRefWithRefId.object, objectRefWithRefId.refId);

			return MethodInvocationResponse.forInvocation(null, null);
		}
		else if (ObjectRef.VIRTUAL_METHOD_NAME_DEC_REF_COUNT.equals(methodName)) {
			final ObjectRefWithRefId[] objectRefWithRefIds = cast(methodInvocationRequest.getArguments()[0]);
			for (final ObjectRefWithRefId objectRefWithRefId : objectRefWithRefIds)
				objectManager.decRefCount(objectRefWithRefId.object, objectRefWithRefId.refId);

			return MethodInvocationResponse.forInvocation(null, null);
		}

		final ExtMethodInvocationRequest extMethodInvocationRequest = new ExtMethodInvocationRequest(objectManager, methodInvocationRequest, clazz);
		final MethodInvocationResponse response = invokeMethodExecutor.execute(extMethodInvocationRequest);
		return response;

//		InvocationFilterRegistry.getInstance().assertCanInvoke(extMethodInvocationRequest);
//
//		final String[] argumentTypeNames = methodInvocationRequest.getArgumentTypeNames();
//		final Class<?>[] argumentTypes = argumentTypeNames == null ? null : classManager.getClassesOrFail(argumentTypeNames);
//
//		final Object[] arguments = methodInvocationRequest.getArguments();
//
//		Object resultObject = null;
//
//		final InvocationType invocationType = methodInvocationRequest.getInvocationType();
//
//		objectManager.getReferenceCleanerRegistry().preInvoke(extMethodInvocationRequest);
//
//		Throwable error = null;
//		try {
//			switch (invocationType) {
//				case CONSTRUCTOR:
//					resultObject = invokeConstructor(clazz, arguments);
//					break;
//				case OBJECT:
//					resultObject = invoke(object, methodName, argumentTypes, arguments);
//					break;
//				case STATIC:
//					resultObject = invokeStatic(clazz, methodName, arguments);
//					break;
//				default:
//					throw new IllegalStateException("Unknown InvocationType: " + invocationType);
//			}
//		} catch (Throwable x) {
//			error = x;
//		} finally {
//			objectManager.getReferenceCleanerRegistry().postInvoke(extMethodInvocationRequest, resultObject, error);
//		}
//
//		if (error != null) {
//			if (error instanceof RuntimeException)
//				throw (RuntimeException) error;
//			else if (error instanceof Error)
//				throw (Error) error;
//			else
//				throw new RuntimeException(error);
//		}
//
//		return new InverseMethodInvocationResponse(request, MethodInvocationResponse.forInvocation(resultObject));
	}

	private MethodInvocationResponse getDelayedMethodInvocationResponse(final Uid delayedResponseId) throws Exception {
		assertNotNull(delayedResponseId, "delayedResponseId");
		return invokeMethodExecutor.getDelayedResponse(delayedResponseId);
	}
}
