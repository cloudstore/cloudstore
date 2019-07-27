package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.repo.local.LocalRepoManager.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static java.util.Objects.*;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.jdo.PersistenceManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;

/**
 * Factory creating JDBC connections to the repository's derby database.
 * <p>
 * <b>Important: This is for maintenance only!</b> Ordinary operations work with a {@link PersistenceManagerFactory}
 * which is managed by {@link LocalRepoManagerImpl}.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class JdbcConnectionFactory {
	private static final Logger logger = LoggerFactory.getLogger(JdbcConnectionFactory.class);

	private final File localRoot;

	private String connectionURL;

	private String connectionDriverName;

	private String connectionUserName;

	private String connectionPassword;

	public JdbcConnectionFactory(final File localRoot) {
		this.localRoot = requireNonNull(localRoot, "localRoot");
		if (!localRoot.isDirectory())
			throw new IllegalArgumentException("The given localRoot is not an existing directory: " + localRoot.getAbsolutePath());

		initProperties();
		initDriverClass();
	}

	private UUID readRepositoryIdFromRepositoryPropertiesFile() {
		final File repositoryPropertiesFile = createFile(getMetaDir(), REPOSITORY_PROPERTIES_FILE_NAME);
		try {
			final Properties repositoryProperties = new Properties();
			try (InputStream in = castStream(repositoryPropertiesFile.createInputStream())) {
				repositoryProperties.load(in);
			}
			final String repositoryIdStr = repositoryProperties.getProperty(PROP_REPOSITORY_ID);
			if (isEmpty(repositoryIdStr))
				throw new IllegalStateException("repositoryProperties.getProperty(PROP_REPOSITORY_ID) is empty!");

			final UUID repositoryId = UUID.fromString(repositoryIdStr);
			return repositoryId;
		} catch (Exception x) {
			throw new RuntimeException("Reading readRepositoryId from '" + repositoryPropertiesFile.getAbsolutePath() + "' failed: " + x, x);
		}
	}

	private void initProperties() {
		UUID repositoryId = readRepositoryIdFromRepositoryPropertiesFile();
		Map<String, String> persistenceProperties = new PersistencePropertiesProvider(repositoryId, localRoot).getPersistenceProperties();
		connectionDriverName = persistenceProperties.get(PersistencePropertiesEnum.CONNECTION_DRIVER_NAME.key);
		connectionURL = persistenceProperties.get(PersistencePropertiesEnum.CONNECTION_URL.key);
		connectionUserName = persistenceProperties.get(PersistencePropertiesEnum.CONNECTION_USER_NAME.key);
		connectionPassword = persistenceProperties.get(PersistencePropertiesEnum.CONNECTION_PASSWORD.key);
	};

	protected File getMetaDir() {
		return createFile(localRoot, META_DIR_NAME);
	}

	private void initDriverClass() {
		if (isEmpty(connectionDriverName))
			return;

		try {
			Class.forName(connectionDriverName);
		} catch (Throwable e) { // Might theoretically be a link error (i.e. a sub-class of Error instead of Exception) => catch Throwable
			logger.warn("initDriverClass" + e, e);
		}
	}

	public Connection createConnection() throws SQLException {
		if (isEmpty(connectionUserName) && isEmpty(connectionPassword))
			return DriverManager.getConnection(connectionURL);
		else
			return DriverManager.getConnection(connectionURL, connectionUserName, connectionPassword);
	}
}
