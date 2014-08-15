package co.codewizards.cloudstore.core.repo.local;

import co.codewizards.cloudstore.core.oio.file.File;


/**
 * Thrown if a {@link LocalRepoManager} could not be created for a given {@link File}, because the file
 * is not yet a repository.
 * <p>
 * Note, that the path denotes an existing directory in the file system, though. However, it was expected
 * to be a repository and not only a simple directory. A repository contains appropriate meta-data, while
 * a simple directory does not.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class FileNoRepositoryException extends LocalRepoManagerException {
	private static final long serialVersionUID = 1L;

	private final File file;

	public FileNoRepositoryException(final File file) {
		super(createMessage(file));
		this.file = file;
	}

	public FileNoRepositoryException(final File file, final Throwable cause) {
		super(createMessage(file), cause);
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	private static String createMessage(final File file) {
		return String.format("File is an existing directory, but it is not a repository: %s", file == null ? null : file.getAbsolutePath());
	}
}
