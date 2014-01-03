package co.codewizards.cloudstore.core.repo.local;

import java.io.File;

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

	private File file;

	public FileAlreadyRepositoryException(File file) {
		super(createMessage(file));
		this.file = file;
	}

	public FileAlreadyRepositoryException(File file, Throwable cause) {
		super(createMessage(file), cause);
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	private static String createMessage(File file) {
		return String.format("File is an existing directory, but it is not a repository: %s", file == null ? null : file.getAbsolutePath());
	}
}
