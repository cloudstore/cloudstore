package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;
import java.util.Arrays;

public class MethodInvocationRequest implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String className;

	private final Object object;

	private final String methodName;

	private final String[] argumentTypeNames;

	private final Object[] arguments;

	protected MethodInvocationRequest(final String className, final Object object, final String methodName, final String[] argumentTypeNames, final Object[] arguments) {
		this.className = className;
		this.object = object;
		this.methodName = methodName;
		this.argumentTypeNames = argumentTypeNames;
		this.arguments = arguments;
	}

	public static MethodInvocationRequest forConstructorInvocation(final String className, final String[] argumentTypeNames, final Object ... arguments) {
		return new MethodInvocationRequest(assertNotNull("className", className), null, null, argumentTypeNames, arguments);
	}

	public static MethodInvocationRequest forStaticInvocation(final String className, final String methodName, final String[] argumentTypeNames, final Object ... arguments) {
		return new MethodInvocationRequest(
				assertNotNull("className", className), null, assertNotNull("methodName", methodName), argumentTypeNames, arguments);
	}

	public static MethodInvocationRequest forObjectInvocation(final Object object, final String methodName, final String[] argumentTypeNames, final Object ... arguments) {
		if (argumentTypeNames != null) {
			if (argumentTypeNames.length > 0 && arguments == null)
				throw new IllegalArgumentException("argumentTypeNames != null && argumentTypeNames.length > 0 && arguments == null");

			int argumentsLength = arguments == null ? 0 : arguments.length;
			if (argumentTypeNames.length != argumentsLength)
				throw new IllegalArgumentException(String.format("argumentTypeNames.length != arguments.length :: %d != %d", argumentTypeNames.length, argumentsLength));
		}
		return new MethodInvocationRequest(
				null, assertNotNull("object", object), assertNotNull("methodName", methodName), argumentTypeNames, arguments);
	}

	public String getClassName() {
		return className;
	}

	public Object getObject() {
		return object;
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
		else if (object != null)
			return InvocationType.OBJECT;

		throw new IllegalStateException("Cannot determine InvocationType!");
	}

	@Override
	public String toString() {
		final InvocationType invocationType = getInvocationType();
		final String argumentsString = arguments == null ? "[]" : Arrays.toString(arguments);
		switch (invocationType) {
			case CONSTRUCTOR:
				return String.format("%s[%s, %s, %s]", getClass().getSimpleName(), invocationType, className, argumentsString);
			case STATIC:
				return String.format("%s[%s, %s, %s, %s]", getClass().getSimpleName(), invocationType, className, methodName, argumentsString);
			case OBJECT:
				return String.format("%s[%s, %s, %s, %s, %s]", getClass().getSimpleName(), invocationType, object.getClass().getName(), object, methodName, argumentsString);
		}
		throw new IllegalStateException("Unexpected InvocationType!");
	}
}
