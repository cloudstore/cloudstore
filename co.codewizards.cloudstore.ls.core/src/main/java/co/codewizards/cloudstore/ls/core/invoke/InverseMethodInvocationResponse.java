package co.codewizards.cloudstore.ls.core.invoke;

import static java.util.Objects.*;

import co.codewizards.cloudstore.ls.core.dto.AbstractInverseServiceResponse;

public class InverseMethodInvocationResponse extends AbstractInverseServiceResponse {
	private static final long serialVersionUID = 1L;

	private final MethodInvocationResponse methodInvocationResponse;

	public InverseMethodInvocationResponse(final InverseMethodInvocationRequest request, final MethodInvocationResponse methodInvocationResponse) {
		super(request);
		this.methodInvocationResponse = requireNonNull(methodInvocationResponse, "methodInvocationResponse");
	}

	public MethodInvocationResponse getMethodInvocationResponse() {
		return methodInvocationResponse;
	}
}
