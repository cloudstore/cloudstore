package co.codewizards.cloudstore.local.db;

import java.util.UUID;

import co.codewizards.cloudstore.core.oio.File;

public interface DatabaseAdapter extends AutoCloseable {
	UUID getRepositoryId();
	void setRepositoryId(UUID repositoryId);

	File getLocalRoot();
	void setLocalRoot(File localRoot);

	void createPersistencePropertiesFileAndDatabase() throws Exception;
}
