package co.codewizards.cloudstore.core.auth;

public class SignatureException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SignatureException() {
	}

	public SignatureException(String message) {
		super(message);
	}

	public SignatureException(Throwable cause) {
		super(cause);
	}

	public SignatureException(String message, Throwable cause) {
		super(message, cause);
	}

	public SignatureException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
