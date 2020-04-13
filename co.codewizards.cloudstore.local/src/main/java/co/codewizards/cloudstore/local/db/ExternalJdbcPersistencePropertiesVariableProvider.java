package co.codewizards.cloudstore.local.db;

import static co.codewizards.cloudstore.local.db.ExternalJdbcDatabaseAdapter.*;
import static java.util.Objects.*;

import java.util.Map;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.local.PersistencePropertiesVariableProvider;

public class ExternalJdbcPersistencePropertiesVariableProvider implements PersistencePropertiesVariableProvider {
	
	private Config config;
	private Map<String, Object> variableMap;

	@Override
	public int getPriority() {
		return 10;
	}

	@Override
	public void populatePersistencePropertiesVariableMap(Map<String, Object> variableMap) {
		this.variableMap = requireNonNull(variableMap, "variableMap");
		this.config = ConfigImpl.getInstance();

		copyConfigValueToVariable(CONFIG_KEY_JDBC_HOST_NAME);
		copyConfigValueToVariable(CONFIG_KEY_JDBC_USER_NAME);
		copyConfigValueToVariable(CONFIG_KEY_JDBC_PASSWORD);

		copyConfigValueToVariable(CONFIG_KEY_JDBC_DB_NAME_PREFIX);
		copyConfigValueToVariable(CONFIG_KEY_JDBC_DB_NAME_SUFFIX);

		copyConfigValueToVariable(CONFIG_KEY_JDBC_SYSDB_NAME);
	}

	private void copyConfigValueToVariable(String key) {
		requireNonNull(key, "key");
		String value = requireNonNull(config, "config").getPropertyAsNonEmptyTrimmedString(key, "");
		requireNonNull(variableMap, "variableMap").put(key, value);
	}
}
