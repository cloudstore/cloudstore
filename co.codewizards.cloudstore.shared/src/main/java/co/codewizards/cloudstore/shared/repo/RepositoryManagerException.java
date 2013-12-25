package co.codewizards.cloudstore.shared.repo;

public class RepositoryManagerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RepositoryManagerException() { }

	public RepositoryManagerException(String message) {
		super(message);
	}

	public RepositoryManagerException(Throwable cause) {
		super(cause);
	}

	public RepositoryManagerException(String message, Throwable cause) {
		super(message, cause);
	}

	public RepositoryManagerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
