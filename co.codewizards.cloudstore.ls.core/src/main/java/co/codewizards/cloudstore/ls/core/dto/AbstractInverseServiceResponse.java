package co.codewizards.cloudstore.ls.core.dto;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import co.codewizards.cloudstore.core.Uid;

public abstract class AbstractInverseServiceResponse implements InverseServiceResponse {
	private static final long serialVersionUID = 1L;

	private final Uid requestId;

	public AbstractInverseServiceResponse(InverseServiceRequest request) {
		this(assertNotNull(assertNotNull(request, "request").getRequestId(), "request.requestId"));
	}

	public AbstractInverseServiceResponse(Uid requestId) {
		this.requestId = assertNotNull(requestId, "requestId");
	}

	@Override
	public Uid getRequestId() {
		return requestId;
	}
}
