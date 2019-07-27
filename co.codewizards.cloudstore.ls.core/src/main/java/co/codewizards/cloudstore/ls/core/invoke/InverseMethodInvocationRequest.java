package co.codewizards.cloudstore.ls.core.invoke;

import static java.util.Objects.*;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.ls.core.dto.AbstractInverseServiceRequest;

public class InverseMethodInvocationRequest extends AbstractInverseServiceRequest {
	private static final long serialVersionUID = 1L;

	private final MethodInvocationRequest methodInvocationRequest;
	private final Uid delayedResponseId;

	public InverseMethodInvocationRequest(final MethodInvocationRequest methodInvocationRequest) {
		this.methodInvocationRequest = requireNonNull(methodInvocationRequest, "methodInvocationRequest");
		this.delayedResponseId = null;
	}

	public InverseMethodInvocationRequest(final Uid delayedResponseId) {
		this.methodInvocationRequest = null;
		this.delayedResponseId = requireNonNull(delayedResponseId, "delayedResponseId");
	}

	public MethodInvocationRequest getMethodInvocationRequest() {
		return methodInvocationRequest;
	}

	public Uid getDelayedResponseId() {
		return delayedResponseId;
	}

	@Override
	public boolean isTimeoutDeadly() {
		return true;
	}
}
