package co.codewizards.cloudstore.core.repo.local;

import co.codewizards.cloudstore.oio.api.File;


/**
 * Thrown if a {@link LocalRepoManager} could not be created for a given {@link File}, because the file
 * is already a repository and the {@code LocalRepoManager} was instructed to create a new repository
 * from a simple file.
 * <p>
 * Note, that this exception is thrown for simple files or directories inside a repository, too.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class FileAlreadyRepositoryException extends LocalRepoManagerException {
	private static final long serialVersionUID = 1L;

	private final File file;

	public FileAlreadyRepositoryException(final File file) {
		super(createMessage(file));
		this.file = file;
	}

	public FileAlreadyRepositoryException(final File file, final Throwable cause) {
		super(createMessage(file), cause);
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	private static String createMessage(final File file) {
		return String.format("File is already (in) a repository (cannot be converted into one): %s", file == null ? null : file.getAbsolutePath());
	}
}
