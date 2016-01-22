package co.codewizards.cloudstore.core.repo.transport;

public class CollisionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	protected CollisionException() { }

	protected CollisionException(String message) {
		super(message);
	}

	protected CollisionException(Throwable cause) {
		super(cause);
	}

	protected CollisionException(String message, Throwable cause) {
		super(message, cause);
	}

	protected CollisionException(String message,
			Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
