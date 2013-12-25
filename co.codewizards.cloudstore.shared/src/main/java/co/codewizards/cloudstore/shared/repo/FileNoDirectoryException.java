package co.codewizards.cloudstore.shared.repo;

import java.io.File;

/**
 * Thrown if a {@link RepositoryManager} could not be created for a given {@link File}, because the file
 * is not a {@link File#isDirectory() directory}.
 * <p>
 * Note, that the path exists in the file system, though. If it does not exist, a {@link FileNotFoundException}
 * is thrown instead.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class FileNoDirectoryException extends RepositoryManagerException {
	private static final long serialVersionUID = 1L;

	private File file;

	public FileNoDirectoryException(File file) {
		super(createMessage(file));
		this.file = file;
	}

	public FileNoDirectoryException(File file, Throwable cause) {
		super(createMessage(file), cause);
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	private static String createMessage(File file) {
		return String.format("File exists, but is not a directory: %s", file == null ? null : file.getAbsolutePath());
	}
}
