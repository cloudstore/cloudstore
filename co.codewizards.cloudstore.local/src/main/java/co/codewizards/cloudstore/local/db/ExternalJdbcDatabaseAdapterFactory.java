package co.codewizards.cloudstore.local.db;

import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static co.codewizards.cloudstore.local.db.AbstractDatabaseAdapter.*;
import static co.codewizards.cloudstore.local.db.ExternalJdbcDatabaseAdapter.*;
import static java.util.Objects.*;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.local.PersistencePropertiesEnum;

/**
 * Abstract common base for JDBC-based external (non-embedded) RDBMS.
 * @author mangu
 */
public abstract class ExternalJdbcDatabaseAdapterFactory extends AbstractDatabaseAdapterFactory {
	private static final Logger logger = LoggerFactory.getLogger(ExternalJdbcDatabaseAdapterFactory.class);

	@Override
	public int getPriority() {
		return 50;
	}

	@Override
	public String getDisableReason() {
		Config config = ConfigImpl.getInstance();
		String hostName = config.getPropertyAsNonEmptyTrimmedString(CONFIG_KEY_JDBC_HOST_NAME, null);
		if (isEmpty(hostName)) {
			return String.format("Configuration lacks '%s'!", CONFIG_KEY_JDBC_HOST_NAME);
		}
		
		String driverName = getDriverName();
		if (! isEmpty(driverName)) {
			try {
				Class.forName(driverName);
			} catch (Throwable error) {
				String result = String.format("Loading driver '%s' failed: %s", driverName, error);
				logger.debug(result, error);
				return result;
			}
		}

		String userName = config.getPropertyAsNonEmptyTrimmedString(CONFIG_KEY_JDBC_USER_NAME, null);
		String password = config.getPropertyAsNonEmptyTrimmedString(CONFIG_KEY_JDBC_PASSWORD, null);

		String url = getJdbcSysdbUrl();
		if (! isEmpty(url)) {
			try {
				Connection connection = DriverManager.getConnection(url, userName, password);
				connection.close();
			} catch (Throwable error) {
				String result = String.format("Connecting to '%s' failed: %s", url, error);
				logger.debug(result, error);
				return result;
			}
		}
		return null;
	}

	/**
	 * Gets the JDBC-protocol. The base-implementation in {@code ExternalJdbcDatabaseAdapterFactory}
	 * returns the same as {@link #getName()}.
	 * @return the JDBC-protocol. Never <code>null</code>.
	 */
	protected String getJdbcProtocol() {
		return getName();
	}

	public String getJdbcSysdbUrl() {
		Config config = ConfigImpl.getInstance();
		
		String jdbcProtocol = getJdbcProtocol();
		requireNonNull(jdbcProtocol, "jdbcProtocol");
		
		String hostName = config.getPropertyAsNonEmptyTrimmedString(CONFIG_KEY_JDBC_HOST_NAME, null);
		requireNonNull(hostName, "hostName");

		String url = "jdbc:" + getJdbcProtocol() + "://" + hostName + "/";
		
		String sysdbName = config.getPropertyAsNonEmptyTrimmedString(CONFIG_KEY_JDBC_SYSDB_NAME, null);
		if (! isEmpty(sysdbName))
			url = url + sysdbName;
		
		return url;
	}

	/**
	 * @return the JDBC-driver-name.
	 */
	protected String getDriverName() {
		final Map<String, Object> variables = new HashMap<>();
		variables.put("databaseAdapter.name", requireNonNull(getName(), "name"));
		final String resolvedPersistencePropertiesTemplateFileName =
				IOUtil.replaceTemplateVariables(PERSISTENCE_PROPERTIES_TEMPLATE_FILE_NAME, variables);
		
		InputStream in = this.getClass().getResourceAsStream("/" + resolvedPersistencePropertiesTemplateFileName);
		if (in == null) {
			logger.warn("Did not find resource: {}", resolvedPersistencePropertiesTemplateFileName);
			return null;
		}		
		try {
			Properties p = new Properties();
			p.load(in);
			String driverName = p.getProperty(PersistencePropertiesEnum.CONNECTION_DRIVER_NAME.key);
			return driverName;
		} catch (Exception x) {
			logger.warn("Reading resource '" + resolvedPersistencePropertiesTemplateFileName + "' failed: " + x, x);
			return null;
		} finally {
			try {
				in.close();
			} catch (Exception e) {
				logger.warn("Closing resource '" + resolvedPersistencePropertiesTemplateFileName + "' failed: " + e, e);
			}
		}
	}

}
