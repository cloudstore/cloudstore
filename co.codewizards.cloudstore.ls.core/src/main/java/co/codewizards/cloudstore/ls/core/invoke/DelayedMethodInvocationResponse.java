package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import co.codewizards.cloudstore.core.Uid;

public class DelayedMethodInvocationResponse extends MethodInvocationResponse {
	private static final long serialVersionUID = 1L;

	public DelayedMethodInvocationResponse(Uid delayedResponseId) {
		super(assertNotNull("delayedResponseId", delayedResponseId));
	}

	@Override
	public Uid getResult() {
		return (Uid) super.getResult();
	}

	public Uid getDelayedResponseId() {
		return getResult();
	}
}
