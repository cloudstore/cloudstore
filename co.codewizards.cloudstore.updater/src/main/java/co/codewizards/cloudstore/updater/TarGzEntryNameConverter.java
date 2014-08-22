package co.codewizards.cloudstore.updater;

import co.codewizards.cloudstore.oio.api.File;

public interface TarGzEntryNameConverter {
	String getEntryName(File rootDir, File file);
	File getFile(File rootDir, String entryName);
}
