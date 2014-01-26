package co.codewizards.cloudstore.core.concurrent;

public class ExecutionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ExecutionException() { }

	public ExecutionException(String message) {
		super(message);
	}

	public ExecutionException(Throwable cause) {
		super(cause);
	}

	public ExecutionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExecutionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
