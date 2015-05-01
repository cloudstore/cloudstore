package co.codewizards.cloudstore.ls.core.invoke;

import java.io.Serializable;

public class MethodInvocationResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	private final Object result;

	protected MethodInvocationResponse(final Object result) {
		this.result = result;
	}

	public static MethodInvocationResponse forInvocation(Object object) {
		return new MethodInvocationResponse(object);
	}

	public Object getResult() {
		return result;
	}
}
