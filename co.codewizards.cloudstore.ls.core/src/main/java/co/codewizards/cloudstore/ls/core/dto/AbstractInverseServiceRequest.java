package co.codewizards.cloudstore.ls.core.dto;

import co.codewizards.cloudstore.core.dto.Uid;

public abstract class AbstractInverseServiceRequest implements InverseServiceRequest {
	private static final long serialVersionUID = 1L;

	private final Uid requestId = new Uid();

	@Override
	public Uid getRequestId() {
		return requestId;
	}

	@Override
	public boolean isTimeoutDeadly() {
		return false;
	}
}
