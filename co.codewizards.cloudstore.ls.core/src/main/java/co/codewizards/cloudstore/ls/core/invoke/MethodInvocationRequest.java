package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;

public class MethodInvocationRequest implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String className;

	private final ObjectRef objectRef;

	private final String methodName;

	private final String[] argumentTypeNames;

	private final Object[] arguments;

	protected MethodInvocationRequest(final String className, final ObjectRef objectRef, final String methodName, final String[] argumentTypeNames, final Object ... arguments) {
		this.className = className;
		this.objectRef = objectRef;
		this.methodName = methodName;
		this.argumentTypeNames = argumentTypeNames;
		this.arguments = arguments;
	}

	public static MethodInvocationRequest forConstructorInvocation(final String className, final Object ... arguments) {
		return new MethodInvocationRequest(assertNotNull("className", className), null, null, null, arguments);
	}

	public static MethodInvocationRequest forStaticInvocation(final String className, final String methodName, final Object ... arguments) {
		return new MethodInvocationRequest(
				assertNotNull("className", className), null, assertNotNull("methodName", methodName), null, arguments);
	}

	public static MethodInvocationRequest forObjectInvocation(final ObjectRef objectRef, final String methodName, final Object ... arguments) {
		return new MethodInvocationRequest(
				null, assertNotNull("objectRef", objectRef), assertNotNull("methodName", methodName), null, arguments);
	}

	public static MethodInvocationRequest forObjectInvocation(final ObjectRef objectRef, final String methodName, final String[] argumentTypeNames, final Object ... arguments) {
		if (argumentTypeNames != null) {
			if (argumentTypeNames.length > 0 && arguments == null)
				throw new IllegalArgumentException("argumentTypeNames != null && argumentTypeNames.length > 0 && arguments == null");

			int argumentsLength = arguments == null ? 0 : arguments.length;
			if (argumentTypeNames.length != argumentsLength)
				throw new IllegalArgumentException(String.format("argumentTypeNames.length != arguments.length :: %d != %d", argumentTypeNames.length, argumentsLength));
		}
		return new MethodInvocationRequest(
				null, assertNotNull("objectRef", objectRef), assertNotNull("methodName", methodName), argumentTypeNames, arguments);
	}

	public String getClassName() {
		return className;
	}

	public ObjectRef getObjectRef() {
		return objectRef;
	}

	public String getMethodName() {
		return methodName;
	}

	public String[] getArgumentTypeNames() {
		return argumentTypeNames;
	}

	public Object[] getArguments() {
		return arguments;
	}

	public InvocationType getInvocationType() {
		if (className != null) {
			if (methodName == null)
				return InvocationType.CONSTRUCTOR;
			else
				return InvocationType.STATIC;
		}
		else if (objectRef != null)
			return InvocationType.OBJECT;

		throw new IllegalStateException("Cannot determine InvocationType!");
	}
}
