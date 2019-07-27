package co.codewizards.cloudstore.ls.core.invoke.filter;

import static java.util.Objects.*;

import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;

public class ExtMethodInvocationRequest {

	private final ObjectManager objectManager;
	private final MethodInvocationRequest methodInvocationRequest;
	private final Class<?> targetClass;

	public ExtMethodInvocationRequest(final ObjectManager objectManager, final MethodInvocationRequest methodInvocationRequest, final Class<?> targetClass) {
		this.objectManager = requireNonNull(objectManager, "objectManager");
		this.methodInvocationRequest = requireNonNull(methodInvocationRequest, "methodInvocationRequest");
		this.targetClass = targetClass == null ? methodInvocationRequest.getObject().getClass() : targetClass;
		requireNonNull(this.targetClass, "this.targetClass");
	}

	public ObjectManager getObjectManager() {
		return objectManager;
	}

	public MethodInvocationRequest getMethodInvocationRequest() {
		return methodInvocationRequest;
	}

	public Class<?> getTargetClass() {
		return targetClass;
	}
}
