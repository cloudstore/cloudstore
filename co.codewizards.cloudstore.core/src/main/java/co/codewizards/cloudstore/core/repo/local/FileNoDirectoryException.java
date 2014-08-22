package co.codewizards.cloudstore.core.repo.local;

import co.codewizards.cloudstore.oio.api.File;


/**
 * Thrown if a {@link LocalRepoManager} could not be created for a given {@link File}, because the file
 * is not a {@link File#isDirectory() directory}.
 * <p>
 * Note, that the path exists in the file system, though. If it does not exist, a {@link FileNotFoundException}
 * is thrown instead.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class FileNoDirectoryException extends LocalRepoManagerException {
	private static final long serialVersionUID = 1L;

	private final File file;

	public FileNoDirectoryException(final File file) {
		super(createMessage(file));
		this.file = file;
	}

	public FileNoDirectoryException(final File file, final Throwable cause) {
		super(createMessage(file), cause);
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	private static String createMessage(final File file) {
		return String.format("File exists, but is not a directory: %s", file == null ? null : file.getAbsolutePath());
	}
}
