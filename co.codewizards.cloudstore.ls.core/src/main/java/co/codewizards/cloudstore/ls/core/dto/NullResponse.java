package co.codewizards.cloudstore.ls.core.dto;

import co.codewizards.cloudstore.core.Uid;

public class NullResponse extends AbstractInverseServiceResponse {
	private static final long serialVersionUID = 1L;

	public NullResponse(final Uid requestId) {
		super(requestId);
	}
}
