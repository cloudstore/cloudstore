package co.codewizards.cloudstore.core.io;

import java.io.File;

public interface LockFile {

	File getFile();

	void release();

}
