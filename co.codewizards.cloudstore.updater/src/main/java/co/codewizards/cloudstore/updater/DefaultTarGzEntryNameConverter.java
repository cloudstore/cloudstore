package co.codewizards.cloudstore.updater;

import co.codewizards.cloudstore.core.oio.file.File;
import java.io.IOException;

import co.codewizards.cloudstore.core.util.IOUtil;

public class DefaultTarGzEntryNameConverter implements TarGzEntryNameConverter {

	@Override
	public String getEntryName(final File rootDir, final File file) {
		try {
			return IOUtil.getRelativePath(rootDir, file).replace(File.separatorChar, '/');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public File getFile(final File rootDir, final String entryName) {
		return newFile(rootDir, entryName);
	}

}
