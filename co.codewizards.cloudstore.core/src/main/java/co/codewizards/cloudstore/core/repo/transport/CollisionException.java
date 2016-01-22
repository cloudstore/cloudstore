package co.codewizards.cloudstore.core.repo.transport;

public class CollisionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CollisionException() { }

	public CollisionException(String message) {
		super(message);
	}

	public CollisionException(Throwable cause) {
		super(cause);
	}

	public CollisionException(String message, Throwable cause) {
		super(message, cause);
	}

	public CollisionException(String message,
			Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
