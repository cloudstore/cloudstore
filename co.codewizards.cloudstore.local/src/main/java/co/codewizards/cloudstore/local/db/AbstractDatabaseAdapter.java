package co.codewizards.cloudstore.local.db;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.repo.local.LocalRepoManager.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;

public abstract class AbstractDatabaseAdapter implements DatabaseAdapter {
	private static final String PERSISTENCE_PROPERTIES_TEMPLATE_FILE_NAME = "cloudstore-persistence.${databaseAdapter.name}.properties";

	private AbstractDatabaseAdapterFactory factory;

	private UUID repositoryId;
	private File localRoot;

	public AbstractDatabaseAdapterFactory getFactory() {
		return factory;
	}
	public AbstractDatabaseAdapterFactory getFactoryOrFail() {
		return assertNotNull("factory", factory);
	}
	protected void setFactory(AbstractDatabaseAdapterFactory factory) {
		this.factory = factory;
	}

	@Override
	public UUID getRepositoryId() {
		return repositoryId;
	}
	public UUID getRepositoryIdOrFail() {
		return assertNotNull("repositoryId", repositoryId);
	}
	@Override
	public void setRepositoryId(UUID repositoryId) {
		this.repositoryId = repositoryId;
	}

	@Override
	public File getLocalRoot() {
		return localRoot;
	}
	public File getLocalRootOrFail() {
		return assertNotNull("localRoot", localRoot);
	}
	@Override
	public void setLocalRoot(File localRoot) {
		this.localRoot = localRoot;
	}

	protected File getMetaDir() {
		return createFile(localRoot, META_DIR_NAME);
	}

	protected File getPersistencePropertiesFile() {
		return createFile(getMetaDir(), PERSISTENCE_PROPERTIES_FILE_NAME);
	}

	protected void createPersistencePropertiesFile() {
		final Map<String, Object> variables = new HashMap<>();
		variables.put("databaseAdapter.name", getFactoryOrFail().getName());
		final String resolvedPersistencePropertiesTemplateFileName =
				IOUtil.replaceTemplateVariables(PERSISTENCE_PROPERTIES_TEMPLATE_FILE_NAME, variables);

		try {
			IOUtil.copyResource(this.getClass(), "/" + resolvedPersistencePropertiesTemplateFileName,
					getPersistencePropertiesFile());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void createPersistencePropertiesFileAndDatabase() throws Exception {
		createPersistencePropertiesFile();
		createDatabase();
	}

	protected abstract void createDatabase() throws Exception;

	@Override
	public void close() throws Exception {
		// nothing to do - sub-classes may extend
	}
}
