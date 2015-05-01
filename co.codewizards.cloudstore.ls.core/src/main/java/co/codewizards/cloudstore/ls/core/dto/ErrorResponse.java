package co.codewizards.cloudstore.ls.core.dto;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.Uid;

public class ErrorResponse extends AbstractInverseServiceResponse {
	private static final long serialVersionUID = 1L;

	private final Error error;

	public ErrorResponse(final Uid requestId, final Error error) {
		super(requestId);
		this.error = assertNotNull("error", error);
	}

	public Error getError() {
		return error;
	}
}
