package co.codewizards.cloudstore.core.util;

import static java.util.Objects.*;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Pattern;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.oio.File;

public class DerbyUtil {

	/**
	 * The Derby database was shut down successfully.
	 */
	private static final int DERBY_ERROR_CODE_SHUTDOWN_DATABASE_SUCCESSFULLY = 45000;
	/**
	 * The Derby database which was shut down was not running (the shut down had no effect).
	 */
	private static final int DERBY_ERROR_CODE_SHUTDOWN_DATABASE_WAS_NOT_RUNNING = 40000;

	public static final String DERBY_PROPERTIES_PREFIX = "derby.";

	public static final String CONFIG_KEY_DERBY_LANGUAGE_STATEMENT_CACHE_SIZE = "derby.language.statementCacheSize";

	public static final String DEFAULT_DERBY_LANGUAGE_STATEMENT_CACHE_SIZE = "500";

	private DerbyUtil() { }

	public static void shutdownDerbyDatabase(String connectionURL) {
		String shutdownConnectionURL = requireNonNull(connectionURL, "connectionURL") + ";shutdown=true";
		try {
			DriverManager.getConnection(shutdownConnectionURL);
		} catch (SQLException e) {
			int errorCode = e.getErrorCode();
			if (DERBY_ERROR_CODE_SHUTDOWN_DATABASE_SUCCESSFULLY != errorCode &&
					DERBY_ERROR_CODE_SHUTDOWN_DATABASE_WAS_NOT_RUNNING != errorCode) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void setLogFile(File file) {
		// First pass all config-properties whose key starts with "derby." as system-property.
		setDerbyPropertiesAsSystemProperties();

		// Then set the actual derby-log-file.
		System.setProperty("derby.stream.error.file", requireNonNull(file, "file").getAbsolutePath());
	}

	protected static void setDerbyPropertiesAsSystemProperties() {
		Config config = ConfigImpl.getInstance();
		Pattern regex = Pattern.compile(Pattern.quote(DERBY_PROPERTIES_PREFIX) + ".*");
		for (String key : config.getKey2GroupsMatching(regex).keySet()) {
			String value = config.getProperty(key, null);
			System.setProperty(key, value);
		}

		String derbyLanguageStatementCacheSize = config.getPropertyAsNonEmptyTrimmedString(
				CONFIG_KEY_DERBY_LANGUAGE_STATEMENT_CACHE_SIZE,
				DEFAULT_DERBY_LANGUAGE_STATEMENT_CACHE_SIZE);

		System.setProperty(CONFIG_KEY_DERBY_LANGUAGE_STATEMENT_CACHE_SIZE, derbyLanguageStatementCacheSize);
	}
}
