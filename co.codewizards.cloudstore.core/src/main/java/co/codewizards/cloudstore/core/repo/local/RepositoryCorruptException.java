package co.codewizards.cloudstore.core.repo.local;

import co.codewizards.cloudstore.core.oio.file.File;

public class RepositoryCorruptException extends LocalRepoManagerException {

	private static final long serialVersionUID = 1L;

	private File localRoot;

	public RepositoryCorruptException(File localRoot, String message) {
		super(String.format("%s Repository: %s", message, localRoot));
		this.localRoot = localRoot;
	}

	public RepositoryCorruptException(File localRoot, Throwable cause) {
		super(String.format("%s Repository: %s", cause.getMessage(), localRoot), cause);
		this.localRoot = localRoot;
	}

	public RepositoryCorruptException(File localRoot, String message, Throwable cause) {
		super(String.format("%s Repository: %s", message, localRoot), cause);
		this.localRoot = localRoot;
	}

	public File getLocalRoot() {
		return localRoot;
	}
}
