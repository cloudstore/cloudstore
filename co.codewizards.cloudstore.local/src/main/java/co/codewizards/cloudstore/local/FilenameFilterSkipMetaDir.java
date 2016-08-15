package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.repo.local.LocalRepoManager.*;

import java.io.File;
import java.io.FilenameFilter;

import co.codewizards.cloudstore.core.config.Config;

public class FilenameFilterSkipMetaDir implements FilenameFilter {

	@Override
	public boolean accept(final File dir, final String name) {
		boolean ignored = !META_DIR_NAME.equals(name)
				&& !TEMP_DIR_NAME.equals(name)
				&& !name.startsWith(TEMP_NEW_FILE_PREFIX)
				&& !name.equalsIgnoreCase(Config.PROPERTIES_FILE_NAME_FOR_DIRECTORY_LOCAL);


//		if (! ignored) {
//			co.codewizards.cloudstore.core.oio.File dirF = createFile(dir);
//			co.codewizards.cloudstore.core.oio.File fileF = dirF.createFile(name);
//			ignored = IgnoreRuleManagerImpl.getInstanceForDirectory(dirF).isIgnored(fileF);
//		}
		return ignored;
	}

}
