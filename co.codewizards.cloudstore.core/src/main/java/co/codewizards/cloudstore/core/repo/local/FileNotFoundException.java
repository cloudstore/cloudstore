package co.codewizards.cloudstore.core.repo.local;

import co.codewizards.cloudstore.oio.api.File;


/**
 * Thrown if a {@link LocalRepoManager} could not be created for a given {@link File}, because the file does not exist.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class FileNotFoundException extends LocalRepoManagerException {
	private static final long serialVersionUID = 1L;

	private final File file;

	public FileNotFoundException(final File file) {
		super(createMessage(file));
		this.file = file;
	}

	public FileNotFoundException(final File file, final Throwable cause) {
		super(createMessage(file), cause);
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	protected static String createMessage(final File file) {
		return String.format("File does not exist: %s", file == null ? null : file.getAbsolutePath());
	}
}
