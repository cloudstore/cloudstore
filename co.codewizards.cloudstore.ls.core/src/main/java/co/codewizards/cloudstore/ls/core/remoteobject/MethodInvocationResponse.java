package co.codewizards.cloudstore.ls.core.remoteobject;

import java.io.Serializable;

public class MethodInvocationResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String resultClassName;

	private final Object result;

	protected MethodInvocationResponse(final String resultClassName, final Object result) {
		this.resultClassName = resultClassName;
		this.result = result;
	}

	public static MethodInvocationResponse forInvocation(final String resultClassName, Object object) {
		return new MethodInvocationResponse(resultClassName, object);
	}

	public String getResultClassName() {
		return resultClassName;
	}

	public Object getResult() {
		return result;
	}
}
