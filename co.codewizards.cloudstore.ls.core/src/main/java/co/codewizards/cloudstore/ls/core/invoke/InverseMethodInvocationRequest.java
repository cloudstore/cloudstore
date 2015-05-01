package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import co.codewizards.cloudstore.ls.core.dto.AbstractInverseServiceRequest;

public class InverseMethodInvocationRequest extends AbstractInverseServiceRequest {
	private static final long serialVersionUID = 1L;

	private final MethodInvocationRequest methodInvocationRequest;

	public InverseMethodInvocationRequest(final MethodInvocationRequest methodInvocationRequest) {
		this.methodInvocationRequest = assertNotNull("methodInvocationRequest", methodInvocationRequest);
	}

	public MethodInvocationRequest getMethodInvocationRequest() {
		return methodInvocationRequest;
	}
}
