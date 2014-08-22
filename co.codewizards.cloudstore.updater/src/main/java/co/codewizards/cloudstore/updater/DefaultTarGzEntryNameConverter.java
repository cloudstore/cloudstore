package co.codewizards.cloudstore.updater;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;

import java.io.IOException;

import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.oio.api.File;

public class DefaultTarGzEntryNameConverter implements TarGzEntryNameConverter {

	@Override
	public String getEntryName(final File rootDir, final File file) {
		try {
			return IOUtil.getRelativePath(rootDir, file).replace(FILE_SEPARATOR_CHAR, '/');
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public File getFile(final File rootDir, final String entryName) {
		return newFile(rootDir, entryName);
	}

}
