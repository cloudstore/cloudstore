package co.codewizards.cloudstore.shared.repo.local;

import java.io.File;

/**
 * Thrown if a {@link LocalRepoManager} could not be created for a given {@link File}, because the file does not exist.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class FileNotFoundException extends LocalRepoManagerException {
	private static final long serialVersionUID = 1L;

	private File file;

	public FileNotFoundException(File file) {
		super(createMessage(file));
		this.file = file;
	}

	public FileNotFoundException(File file, Throwable cause) {
		super(createMessage(file), cause);
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	protected static String createMessage(File file) {
		return String.format("File does not exist: %s", file == null ? null : file.getAbsolutePath());
	}
}
