package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.repo.local.LocalRepoManager.*;

import java.io.File;
import java.io.FilenameFilter;

public class FilenameFilterSkipMetaDir implements FilenameFilter {

	@Override
	public boolean accept(File dir, String name) {
		return !META_DIR_NAME.equals(name) && !TEMP_DIR_NAME.equals(name);
	}

}
