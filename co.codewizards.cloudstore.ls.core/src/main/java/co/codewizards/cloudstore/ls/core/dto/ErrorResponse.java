package co.codewizards.cloudstore.ls.core.dto;

import static java.util.Objects.*;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.dto.Error;

public class ErrorResponse extends AbstractInverseServiceResponse {
	private static final long serialVersionUID = 1L;

	private final Error error;

	public ErrorResponse(final Uid requestId, final Error error) {
		super(requestId);
		this.error = requireNonNull(error, "error");
	}

	public Error getError() {
		return error;
	}
}
