package co.codewizards.cloudstore.shared.repo.local;

import java.io.File;
import java.io.FilenameFilter;

public class FilenameFilterSkipMetaDir implements FilenameFilter {

	@Override
	public boolean accept(File dir, String name) {
		return !RepositoryManager.META_DIR_NAME.equals(name);
	}

}
