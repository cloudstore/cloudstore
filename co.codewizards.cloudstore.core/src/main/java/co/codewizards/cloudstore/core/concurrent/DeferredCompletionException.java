package co.codewizards.cloudstore.core.concurrent;

import co.codewizards.cloudstore.core.exception.ApplicationException;

@ApplicationException
public class DeferredCompletionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public DeferredCompletionException() { }

	public DeferredCompletionException(String message) {
		super(message);
	}

	public DeferredCompletionException(Throwable cause) {
		super(cause);
	}

	public DeferredCompletionException(String message, Throwable cause) {
		super(message, cause);
	}

	public DeferredCompletionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
