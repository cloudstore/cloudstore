package co.codewizards.cloudstore.ls.core.invoke;

import java.io.Serializable;

public class MethodInvocationResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	private final Object result;

	private final Object[] writableArguments;

	protected MethodInvocationResponse(final Object result, Object[] writableArguments) {
		this.result = result;
		this.writableArguments = writableArguments;
	}

	public static MethodInvocationResponse forInvocation(Object object, Object[] writableArguments) {
		return new MethodInvocationResponse(object, writableArguments);
	}

	public Object getResult() {
		return result;
	}

	public Object[] getWritableArguments() {
		return writableArguments;
	}
}
