package co.codewizards.cloudstore.ls.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.ls.core.invoke.ClassManager;
import co.codewizards.cloudstore.ls.core.invoke.InvokeMethodExecutor;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRefWithRefId;
import co.codewizards.cloudstore.ls.core.invoke.filter.ExtMethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.provider.MediaTypeConst;
import co.codewizards.cloudstore.ls.rest.server.InverseInvoker;

@Path("InvokeMethod")
@Consumes(MediaTypeConst.APPLICATION_JAVA_NATIVE_WITH_OBJECT_REF)
@Produces(MediaTypeConst.APPLICATION_JAVA_NATIVE_WITH_OBJECT_REF)
public class InvokeMethodService extends AbstractService {

	private static final InvokeMethodExecutor invokeMethodExecutor = new InvokeMethodExecutor();

	@POST
	public MethodInvocationResponse performMethodInvocation(final MethodInvocationRequest methodInvocationRequest) throws Throwable {
		assertNotNull("methodInvocationRequest", methodInvocationRequest);

		// *always* acquiring to make sure the lastUseDate is updated - and to make things easy: we have what we need.
		final InverseInvoker inverseInvoker = getInverseInvoker();
		final ObjectManager objectManager = inverseInvoker.getObjectManager();
		final ClassManager classManager = objectManager.getClassManager();

		final String className = methodInvocationRequest.getClassName();
		final Class<?> clazz = className == null ? null : classManager.getClassOrFail(className);

		final String methodName = methodInvocationRequest.getMethodName();

		if (ObjectRef.VIRTUAL_METHOD_NAME_INC_REF_COUNT.equals(methodName)) {
			final ObjectRefWithRefId[] objectRefWithRefIds = cast(methodInvocationRequest.getArguments()[0]);
			for (final ObjectRefWithRefId objectRefWithRefId : objectRefWithRefIds)
				objectManager.incRefCount(objectRefWithRefId.object, objectRefWithRefId.refId);

			return MethodInvocationResponse.forInvocation(null);
		}
		else if (ObjectRef.VIRTUAL_METHOD_NAME_DEC_REF_COUNT.equals(methodName)) {
			final ObjectRefWithRefId[] objectRefWithRefIds = cast(methodInvocationRequest.getArguments()[0]);
			for (final ObjectRefWithRefId objectRefWithRefId : objectRefWithRefIds)
				objectManager.decRefCount(objectRefWithRefId.object, objectRefWithRefId.refId);

			return MethodInvocationResponse.forInvocation(null);
		}

		final ExtMethodInvocationRequest extMethodInvocationRequest = new ExtMethodInvocationRequest(objectManager, methodInvocationRequest, clazz);
		return invokeMethodExecutor.execute(extMethodInvocationRequest);
	}

	@GET
	@Path("{delayedResponseId}")
	public MethodInvocationResponse getDelayedMethodInvocationResponse(@PathParam("delayedResponseId") final Uid delayedResponseId) throws Throwable {
		assertNotNull("delayedResponseId", delayedResponseId);
		// *always* acquiring to make sure the lastUseDate is updated - and to make things easy: we have what we need.
		getInverseInvoker().getObjectManager();

		return invokeMethodExecutor.getDelayedResponse(delayedResponseId);
	}
}
