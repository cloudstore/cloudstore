package co.codewizards.cloudstore.updater;

public class PGPVerifyException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PGPVerifyException() { }

	public PGPVerifyException(String message) {
		super(message);
	}

	public PGPVerifyException(Throwable cause) {
		super(cause);
	}

	public PGPVerifyException(String message, Throwable cause) {
		super(message, cause);
	}

	public PGPVerifyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
