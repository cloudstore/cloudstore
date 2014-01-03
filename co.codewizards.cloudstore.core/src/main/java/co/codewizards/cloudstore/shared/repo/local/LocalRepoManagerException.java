package co.codewizards.cloudstore.shared.repo.local;

public class LocalRepoManagerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public LocalRepoManagerException() { }

	public LocalRepoManagerException(String message) {
		super(message);
	}

	public LocalRepoManagerException(Throwable cause) {
		super(cause);
	}

	public LocalRepoManagerException(String message, Throwable cause) {
		super(message, cause);
	}

	public LocalRepoManagerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
