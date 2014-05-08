package co.codewizards.cloudstore.updater;

import java.io.File;

public interface TarGzEntryNameConverter {
	String getEntryName(File rootDir, File file);
	File getFile(File rootDir, String entryName);
}
