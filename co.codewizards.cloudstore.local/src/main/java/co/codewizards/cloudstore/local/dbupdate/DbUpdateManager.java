package co.codewizards.cloudstore.local.dbupdate;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.repo.local.LocalRepoManager.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.RepositoryCorruptException;
import co.codewizards.cloudstore.core.util.PropertiesUtil;
import co.codewizards.cloudstore.local.db.DatabaseAdapter;

public class DbUpdateManager {

	protected final File localRoot;
	protected final DbUpdateStepRegistry dbUpdateStepRegistry;
	protected final DatabaseAdapter databaseAdapter;
	
	public DbUpdateManager(File localRoot, DbUpdateStepRegistry dbUpdateStepRegistry, DatabaseAdapter databaseAdapter) {
		this.localRoot = requireNonNull(localRoot, "localRoot");
		this.dbUpdateStepRegistry = requireNonNull(dbUpdateStepRegistry, "dbUpdateStepRegistry");
		this.databaseAdapter = requireNonNull(databaseAdapter, "databaseAdapter");
	}
	
	protected File getMetaDir() {
		return localRoot.createFile(META_DIR_NAME);
	}
	
	public Properties readRepositoryProperties() {
		final File repositoryPropertiesFile = createFile(getMetaDir(), REPOSITORY_PROPERTIES_FILE_NAME);
		if (!repositoryPropertiesFile.exists())
			throw new RepositoryCorruptException(localRoot,
					String.format("Meta-directory does not contain '%s'!", REPOSITORY_PROPERTIES_FILE_NAME));

		Properties repositoryProperties;
		try {
			repositoryProperties = PropertiesUtil.load(repositoryPropertiesFile);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return repositoryProperties;
	}

	public int readRepositoryVersion() {
		Properties repositoryProperties = readRepositoryProperties();
		String version = repositoryProperties.getProperty(PROP_VERSION);
		if (version == null || version.isEmpty())
			throw new RepositoryCorruptException(localRoot,
					String.format("Meta-file '%s' does not contain property '%s'!", REPOSITORY_PROPERTIES_FILE_NAME, PROP_VERSION));

		version = version.trim();
		int ver;
		try {
			ver = Integer.parseInt(version);
		} catch (final NumberFormatException x) {
			throw new RepositoryCorruptException(localRoot,
					String.format("Meta-file '%s' contains an illegal value (not a number) for property '%s'!", REPOSITORY_PROPERTIES_FILE_NAME, PROP_VERSION));
		}
		return ver;
	}
	
	public void performUpdate() {
		int lastVersion = readRepositoryVersion();
		SortedMap<Integer, List<DbUpdateStep>> version2dbUpdateSteps = dbUpdateStepRegistry.getDbUpdateStepsAfter(lastVersion);
		for (Map.Entry<Integer, List<DbUpdateStep>> me : version2dbUpdateSteps.entrySet()) {
			int version = me.getKey();
			for (DbUpdateStep dbUpdateStep : me.getValue()) {
				try {
					dbUpdateStep.setDatabaseAdapter(databaseAdapter);
					dbUpdateStep.performUpdate();
				} catch (Exception x) {
					throw new RepositoryCorruptException(localRoot,
							String.format("Updating repository via '%s' failed!",
									dbUpdateStep.getClass().getName()), x);
				}
			}
			writeRepositoryVersion(version);
			lastVersion = version;
		}
		int version = readRepositoryVersion();
		if (lastVersion != version) {
			throw new RepositoryCorruptException(localRoot,
					String.format("Writing lastVersion seems to have failed! Newly read version=%d, but expected version=%d!",
							version, lastVersion));
		}
	}
	
	protected void writeRepositoryVersion(int version) {
		final File repositoryPropertiesFile = createFile(getMetaDir(), REPOSITORY_PROPERTIES_FILE_NAME);
		if (!repositoryPropertiesFile.exists())
			throw new RepositoryCorruptException(localRoot,
					String.format("Meta-directory does not contain '%s'!", REPOSITORY_PROPERTIES_FILE_NAME));

		Properties repositoryProperties;
		try {
			repositoryProperties = PropertiesUtil.load(repositoryPropertiesFile);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		repositoryProperties.setProperty(PROP_VERSION, Integer.toString(version));

		try {
			PropertiesUtil.store(repositoryPropertiesFile, repositoryProperties, null);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
}
