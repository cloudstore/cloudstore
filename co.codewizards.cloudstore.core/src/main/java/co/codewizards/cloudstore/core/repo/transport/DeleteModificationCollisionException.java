package co.codewizards.cloudstore.core.repo.transport;

public class DeleteModificationCollisionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public DeleteModificationCollisionException() { }

	public DeleteModificationCollisionException(String message) {
		super(message);
	}

	public DeleteModificationCollisionException(Throwable cause) {
		super(cause);
	}

	public DeleteModificationCollisionException(String message, Throwable cause) {
		super(message, cause);
	}

	public DeleteModificationCollisionException(String message,
			Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
