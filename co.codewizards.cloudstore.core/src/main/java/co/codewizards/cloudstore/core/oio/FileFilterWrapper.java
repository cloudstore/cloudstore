package co.codewizards.cloudstore.core.oio;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static java.util.Objects.*;

public class FileFilterWrapper implements java.io.FileFilter {

	private final FileFilter fileFilter;

	public FileFilterWrapper(final FileFilter fileFilter) {
		this.fileFilter = requireNonNull(fileFilter, "fileFilter");
	}

	@Override
	public boolean accept(final java.io.File pathname) {
		final File file = createFile(pathname);
		return fileFilter.accept(file);
	}
}
