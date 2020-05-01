package co.codewizards.cloudstore.local.db;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.repo.local.LocalRepoManager.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.jdo.FetchGroup;
import javax.jdo.FetchPlan;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.io.IInputStream;
import co.codewizards.cloudstore.core.io.IOutputStream;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.DaoProvider;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.util.DebugUtil;
import co.codewizards.cloudstore.core.util.PropertiesUtil;
import co.codewizards.cloudstore.local.PersistencePropertiesProvider;
import co.codewizards.cloudstore.local.persistence.CloudStorePersistenceCapableClassesProvider;
import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.Entity;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.NormalFileDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class DatabaseMigrater implements DaoProvider {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrater.class);

	public static final String DBMIGRATE_TRIGGER_FILE_NAME = "dbmigrate.deleteToRun";
	public static final String DBMIGRATE_STATUS_FILE_NAME = "dbmigrate.status.properties";
	public static final String DBMIGRATE_TARGET_DIR_NAME = "dbmigrate.target.tmp";

	public static final String STATUS_SOURCE_DB_ADAPTER_NAME = "sourceDatabaseAdapterName";
	public static final String STATUS_TARGET_DB_ADAPTER_NAME = "targetDatabaseAdapterName";
	public static final String STATUS_TARGET_DB_CREATED = "targetDatabaseCreated";
	public static final String STATUS_MIGRATION_COMPLETE = "MIGRATION_COMPLETE";

	public static final String STATUS_TARGET_TABLE_IDENTITY_COLUMN_PASSIVATED_FORMAT = "targetTable[%s].identityColumnPassivated";
	public static final String STATUS_TARGET_TABLE_IDENTITY_COLUMN_ACTIVATED_FORMAT = "targetTable[%s].identityColumnActivated";
	public static final String STATUS_TABLE_DATA_COPIED_FORMAT = "table[%s].dataCopied";

	protected final File localRoot;
	protected final File metaDir;
	protected final File lockFile;
	protected final File targetLocalRoot;
	protected final File targetMetaDir;
	protected final UUID repositoryId;
	protected Properties status = new Properties();

	protected DatabaseAdapterFactory sourceDbAdapterFactory;
	protected DatabaseAdapterFactory targetDbAdapterFactory;
	protected DatabaseAdapter sourceDbAdapter;
	protected DatabaseAdapter targetDbAdapter;

	protected PersistenceManagerFactory sourcePmf;
	protected PersistenceManagerFactory targetPmf;
	protected PersistenceManager sourcePm;
	protected PersistenceManager targetPm;
	protected Connection sourceConnection;
	protected Connection targetConnection;
	private final Map<Class<?>, Object> daoClass2Dao = new HashMap<>();

	protected DatabaseMigrater(File localRoot) {
		this.localRoot = requireNonNull(localRoot, "localRoot");
		this.metaDir = this.localRoot.createFile(META_DIR_NAME);
		this.lockFile = this.metaDir.createFile(REPOSITORY_LOCK_FILE_NAME);

		this.targetLocalRoot = this.metaDir.createFile(DBMIGRATE_TARGET_DIR_NAME);
		this.targetMetaDir = this.targetLocalRoot.createFile(META_DIR_NAME);

		this.repositoryId = readRepositoryIdFromRepositoryPropertiesFile();
	}

	public static DatabaseMigrater create(File localRoot) {
		requireNonNull(localRoot, "localRoot");
		return createObject(DatabaseMigrater.class, localRoot);
	}

	private UUID readRepositoryIdFromRepositoryPropertiesFile() {
		final File repositoryPropertiesFile = createFile(metaDir, REPOSITORY_PROPERTIES_FILE_NAME);
		if (! repositoryPropertiesFile.isFile())
			return null;

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

	public void deleteTriggerFile() {
		File triggerFile = getTriggerFile();
		if (! triggerFile.exists()) {
			logger.info("deleteTriggerFile: Trigger-file '{}' does not exist => cannot delete.", triggerFile.getAbsolutePath());
			return;
		}

		triggerFile.delete();
		if (triggerFile.exists())
			throw new IllegalStateException(String.format("Trigger-file '%s' could not be deleted (it still exists)!", triggerFile.getAbsolutePath()));

		logger.info("deleteTriggerFile: Trigger-file '{}' successfully deleted.", triggerFile.getAbsolutePath());
	}

	public void migrateIfNeeded() {
		try (LockFile lf = LockFileFactory.getInstance().acquire(lockFile, 3000)) {
			Lock lock = lf.getLock();
			try {
				if (! lock.tryLock(3000, TimeUnit.MILLISECONDS))
					throw new IllegalStateException("Cannot lock within timeout!");
			} catch (InterruptedException x) {
				logger.warn("migrateIfNeeded: localRoot='" + localRoot + "': " + x, x);
				return;
			}
			try {
				if (! isMigrationInProcess()) {
					logger.info("migrateIfNeeded: localRoot='{}': No migration => return immediately.", localRoot);
					return;
				}
				try {
					if (LocalRepoManagerFactory.Helper.getInstance().getLocalRoots().contains(localRoot)) {
						throw new IllegalStateException("There is currently a LocalRepoManager open for this localRoot: " +localRoot.getPath());
					}
					DatabaseAdapterFactoryRegistry databaseAdapterFactoryRegistry = DatabaseAdapterFactoryRegistry.getInstance();
					sourceDbAdapterFactory = databaseAdapterFactoryRegistry.getDatabaseAdapterFactoryOrFail(localRoot);
					targetDbAdapterFactory = databaseAdapterFactoryRegistry.getDatabaseAdapterFactoryOrFail();
					String sourceDbafName = sourceDbAdapterFactory.getName();
					String targetDbafName = targetDbAdapterFactory.getName();

					if (sourceDbafName.equals(targetDbafName)) {
						logger.info("migrateIfNeeded: localRoot='{}': sourceDatabaseAdapterName == targetDatabaseAdapterName == '{}' :: Nothing to do!",
								localRoot, sourceDbafName);
					} else {
						logger.info("migrateIfNeeded: localRoot='{}': sourceDatabaseAdapterName == '{}' != targetDatabaseAdapterName == '{}' :: Starting migration, now!",
								localRoot, sourceDbafName, targetDbafName);

						readStatus();

						if (status.getProperty(STATUS_SOURCE_DB_ADAPTER_NAME) != null
								&& ! sourceDbafName.equals(status.getProperty(STATUS_SOURCE_DB_ADAPTER_NAME))) {
							throw new IllegalStateException(String.format("Previously started migration with sourceDatabaseAdapterName = '%s', but current sourceDatabaseAdapterName = '%s'!",
									status.getProperty(STATUS_SOURCE_DB_ADAPTER_NAME), sourceDbafName));
						}
						status.setProperty(STATUS_SOURCE_DB_ADAPTER_NAME, sourceDbafName);

						if (status.getProperty(STATUS_TARGET_DB_ADAPTER_NAME) != null
								&& ! targetDbafName.equals(status.getProperty(STATUS_TARGET_DB_ADAPTER_NAME))) {
							throw new IllegalStateException(String.format("Previously started migration with targetDatabaseAdapterName = '%s', but current targetDatabaseAdapterName = '%s'!",
									status.getProperty(STATUS_TARGET_DB_ADAPTER_NAME), targetDbafName));
						}
						status.setProperty(STATUS_TARGET_DB_ADAPTER_NAME, targetDbafName);

						writeStatus();

						if (! Boolean.parseBoolean(status.getProperty(STATUS_TARGET_DB_CREATED))) {
							createTargetPersistencePropertiesAndDatabase();

							status.setProperty(STATUS_TARGET_DB_CREATED, Boolean.toString(true));
							writeStatus();
						}

						createPersistenceManagerFactories();
						closePersistenceManagerFactories();

						try {
							createJdbcConnections();

							dropTargetForeignKeys();
							copyTableData();
						} finally {
							closeJdbcConnections();
						}

						// Re-create the PMFs in order to re-create the target's foreign-keys.
						createPersistenceManagerFactories();
						testTargetPersistence();
						closePersistenceManagerFactories();

						moveTargetMetaDirContents();

						status.setProperty(STATUS_MIGRATION_COMPLETE, STATUS_MIGRATION_COMPLETE + " *** *** *** Please delete this file now. *** *** ***");
						writeStatus();
						logger.info("migrateIfNeeded: localRoot='{}': Migration complete!", localRoot);
					}
					// Finally, when we're completely done, we create the trigger-file, again.
					createTriggerFile();
				} catch (RuntimeException x) {
					throw x;
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					closePersistenceManagerFactories();
				}
			} finally {
				lock.unlock();
			}
		}
	}

	private void moveTargetMetaDirContents() {
		File[] newFiles = targetMetaDir.listFiles();
		requireNonNull(newFiles, "targetMetaDir.listFiles() :: targetMetaDir=" + targetMetaDir.getAbsolutePath());

		final String backupFileNameSuffix = ".dbmigrate_" + Long.toString(System.currentTimeMillis(), 36) + ".bak";

		for (File newFile : newFiles) {
			File oldFile = metaDir.createFile(newFile.getName());
			if (oldFile.exists()) {
				File backupOldFile = oldFile.getParentFile().createFile(oldFile.getName() + backupFileNameSuffix);
				if (backupOldFile.exists())
					throw new IllegalStateException("backupOldFile already exists: " + backupOldFile.getAbsolutePath());

				oldFile.renameTo(backupOldFile);

				if (! backupOldFile.exists())
					throw new IllegalStateException(String.format("Renaming '%s' to '%s' failed! Target-file still does not exist!", oldFile.getAbsolutePath(), backupOldFile.getAbsolutePath()));

				if (oldFile.exists())
					throw new IllegalStateException(String.format("Renaming '%s' to '%s' failed! Source-file still exists!", oldFile.getAbsolutePath(), backupOldFile.getAbsolutePath()));
			}

			newFile.renameTo(oldFile);

			if (newFile.exists())
				throw new IllegalStateException(String.format("Renaming '%s' to '%s' failed! Source-file still exists!", newFile.getAbsolutePath(), oldFile.getAbsolutePath()));

			if (! oldFile.exists())
				throw new IllegalStateException(String.format("Renaming '%s' to '%s' failed! Target-file still does not exist!", newFile.getAbsolutePath(), oldFile.getAbsolutePath()));
		}

		targetMetaDir.delete();
		targetLocalRoot.delete();
	}

	protected SortedSet<Table> getTables(Connection connection) throws Exception {
		DatabaseMetaData metaData = requireNonNull(connection, "connection").getMetaData();
		SortedSet<Table> result = new TreeSet<Table>();
		try (ResultSet rs = metaData.getTables(null, null, null, new String[] { "TABLE" })) {
			while (rs.next()) {
				String catalogue = rs.getString("TABLE_CAT");
				String schema = rs.getString("TABLE_SCHEM");
				String name = rs.getString("TABLE_NAME");
				logger.debug("getTables: catalogue='{}' schema='{}' name='{}'", catalogue, schema, name);
				result.add(new Table(catalogue, schema, name));
			}
		}
		return result;
	}

	protected void dropTargetForeignKeys() throws Exception {
		DatabaseMetaData metaData = requireNonNull(targetConnection, "targetConnection").getMetaData();
		SortedSet<Table> tables = getTables(targetConnection);
		for (Table table : tables) {
			try (ResultSet rs = metaData.getImportedKeys(table.catalogue, table.schema, table.name)) {
				while (rs.next()) {
					String fkName = rs.getString("FK_NAME");
					if (fkName != null && ! fkName.isEmpty()) {
						dropForeignKey(targetConnection, table, fkName);
					}
				}
			}
		}
	}

	protected void dropForeignKey(Connection connection, Table table, String fkName) throws Exception {
		requireNonNull(connection, "connection");
		requireNonNull(table, "table");
		requireNonNull(fkName, "fkName");

		try (Statement statement = connection.createStatement()) {
			String sql = String.format("alter table \"%s\" drop constraint \"%s\"",
					table.name, fkName);
			logger.info("dropForeignKey: Executing: {}", sql);
			statement.executeUpdate(sql);
		}
	}

	protected void copyTableData() throws Exception {
		SortedSet<Table> sourceTables = getTables(sourceConnection);
		SortedSet<Table> targetTables = getTables(targetConnection);

		Set<String> targetTableNames = new TreeSet<String>();
		for (Table table : targetTables) {
			targetTableNames.add(table.name.toUpperCase(Locale.UK));
		}

		Set<String> sourceTableNames = new TreeSet<String>();
		for (Table table : sourceTables) {
			sourceTableNames.add(table.name.toUpperCase(Locale.UK));
		}

		Set<String> sourceTablesNamesMissingInTarget = new TreeSet<String>(sourceTableNames);
		sourceTablesNamesMissingInTarget.removeAll(targetTableNames);

		Set<String> targetTableNamesMissingInSource = new TreeSet<String>(targetTableNames);
		targetTableNamesMissingInSource.removeAll(sourceTableNames);

		if (sourceTablesNamesMissingInTarget.isEmpty())
			logger.info("copyTableData: All source-tables exist in the target-DB!");
		else
			logger.warn("copyTableData: The following source-tables are missing in the target-DB: {}", sourceTablesNamesMissingInTarget);

		if (targetTableNamesMissingInSource.isEmpty())
			logger.info("copyTableData: All target-tables exist in the source-DB!");
		else
			logger.warn("copyTableData: The following target-tables are missing in the source-DB: {}", targetTableNamesMissingInSource);

		List<String> tableNamesToCopy = new ArrayList<String>(sourceTables.size());

		for (Table table : sourceTables) {
			if (targetTableNames.contains(table.name.toUpperCase(Locale.UK))) {
				tableNamesToCopy.add(table.name.toUpperCase(Locale.UK));
			}
		}
		Map<String, Table> targetTableName2Table = new HashMap<String, Table>();
		for (Table table : targetTables) {
			targetTableName2Table.put(table.name.toUpperCase(Locale.UK), table);
		}

		Map<Table, List<Column>> sourceTable2Columns = getTable2Columns(sourceConnection, sourceTables);
		Map<Table, List<Column>> targetTable2Columns = getTable2Columns(targetConnection, targetTables);

		int tableIndex = 0; // 1-based!!!
		for (Table sourceTable : sourceTables) {
			if (! tableNamesToCopy.contains(sourceTable.name.toUpperCase(Locale.UK))) {
				continue;
			}
			++tableIndex;
			logger.info("copyTableData: Copying table '{}' ({} of {})...",
					sourceTable.name.toUpperCase(Locale.UK), tableIndex, tableNamesToCopy.size());

			Table targetTable = requireNonNull(targetTableName2Table.get(sourceTable.name.toUpperCase(Locale.UK)),
					"targetTables[" + sourceTable.name.toUpperCase(Locale.UK) + "]");

			SortedMap<String, Column> sourceColumnName2Column = getColumnName2ColumnMap(
					requireNonNull(sourceTable2Columns.get(sourceTable), "sourceTable2Columns.get(" + sourceTable + ")"));

			SortedMap<String, Column> targetColumnName2Column = getColumnName2ColumnMap(
					requireNonNull(targetTable2Columns.get(targetTable), "targetTable2Columns.get(" + targetTable + ")"));

			Set<String> sourceColumnNamesMissingInTarget = new TreeSet<String>(sourceColumnName2Column.keySet());
			sourceColumnNamesMissingInTarget.removeAll(targetColumnName2Column.keySet());

			Set<String> targetColumnNamesMissingInSource = new TreeSet<String>(targetColumnName2Column.keySet());
			targetColumnNamesMissingInSource.removeAll(sourceColumnName2Column.keySet());

			if (sourceColumnNamesMissingInTarget.isEmpty())
				logger.info("copyTableData: Table '{}': All source-columns exist in the target-DB!", sourceTable.name.toUpperCase(Locale.UK));
			else
				logger.warn("copyTableData: Table '{}': The following source-columns are missing in the target-DB: {}",
						sourceTable.name.toUpperCase(Locale.UK), sourceColumnNamesMissingInTarget);

			if (targetColumnNamesMissingInSource.isEmpty())
				logger.info("copyTableData: Table '{}': All target-columns exist in the source-DB!", sourceTable.name.toUpperCase(Locale.UK));
			else
				logger.warn("copyTableData: Table '{}': The following target-columns are missing in the source-DB: {}",
						sourceTable.name.toUpperCase(Locale.UK), targetColumnNamesMissingInSource);

			Boolean passivatedIdentityColumn = getStatusTargetTableIdentityColumnPassivated(targetTable);
			if (passivatedIdentityColumn == null) {
				passivatedIdentityColumn = targetDbAdapter.passivateIdentityColumn(targetConnection, targetTable, targetColumnName2Column);
				setStatusTargetTableIdentityColumnPassivated(targetTable, passivatedIdentityColumn);
			}

			Boolean dataCopied = getStatusTableDataCopied(targetTable);
			if (! Boolean.TRUE.equals(dataCopied)) {
				copyTableData(sourceTable, targetTable, sourceColumnName2Column, targetColumnName2Column);
				setStatusTableDataCopied(targetTable, true);
			}

			if (passivatedIdentityColumn) {
				Boolean activatedIdentityColumn = getStatusTargetTableIdentityColumnActivated(targetTable);
				if (! Boolean.TRUE.equals(activatedIdentityColumn)) {
					targetDbAdapter.activateIdentityColumn(targetConnection, targetTable, targetColumnName2Column);
					setStatusTargetTableIdentityColumnActivated(targetTable, true);
				}
			}

			logger.info("copyTableData: Copied table '{}' ({} of {})."
					, sourceTable.name.toUpperCase(Locale.UK), tableIndex, tableNamesToCopy.size());
		}
	}

	protected Boolean getStatusTableDataCopied(Table targetTable) {
		requireNonNull(targetTable, "targetTable");
		String statusKey = String.format(STATUS_TABLE_DATA_COPIED_FORMAT, targetTable.name.toUpperCase(Locale.UK));
		String statusValue = status.getProperty(statusKey);
		if (statusValue == null || statusValue.trim().isEmpty())
			return null;

		return Boolean.parseBoolean(statusValue);
	}

	protected void setStatusTableDataCopied(Table targetTable, boolean value) throws Exception {
		requireNonNull(targetTable, "targetTable");
		String statusKey = String.format(STATUS_TABLE_DATA_COPIED_FORMAT, targetTable.name.toUpperCase(Locale.UK));
		status.setProperty(statusKey, Boolean.toString(value));
		writeStatus();
	}

	protected Boolean getStatusTargetTableIdentityColumnPassivated(Table targetTable) {
		requireNonNull(targetTable, "targetTable");
		String statusKey = String.format(STATUS_TARGET_TABLE_IDENTITY_COLUMN_PASSIVATED_FORMAT, targetTable.name.toUpperCase(Locale.UK));
		String statusValue = status.getProperty(statusKey);
		if (statusValue == null || statusValue.trim().isEmpty())
			return null;

		return Boolean.parseBoolean(statusValue);
	}

	protected void setStatusTargetTableIdentityColumnPassivated(Table targetTable, boolean value) throws Exception {
		requireNonNull(targetTable, "targetTable");
		String statusKey = String.format(STATUS_TARGET_TABLE_IDENTITY_COLUMN_PASSIVATED_FORMAT, targetTable.name.toUpperCase(Locale.UK));
		status.setProperty(statusKey, Boolean.toString(value));
		writeStatus();
	}

	protected Boolean getStatusTargetTableIdentityColumnActivated(Table targetTable) {
		requireNonNull(targetTable, "targetTable");
		String statusKey = String.format(STATUS_TARGET_TABLE_IDENTITY_COLUMN_ACTIVATED_FORMAT, targetTable.name.toUpperCase(Locale.UK));
		String statusValue = status.getProperty(statusKey);
		if (statusValue == null || statusValue.trim().isEmpty())
			return null;

		return Boolean.parseBoolean(statusValue);
	}

	protected void setStatusTargetTableIdentityColumnActivated(Table targetTable, boolean value) throws Exception {
		requireNonNull(targetTable, "targetTable");
		String statusKey = String.format(STATUS_TARGET_TABLE_IDENTITY_COLUMN_ACTIVATED_FORMAT, targetTable.name.toUpperCase(Locale.UK));
		status.setProperty(statusKey, Boolean.toString(value));
		writeStatus();
	}

	protected void copyTableData(Table sourceTable, Table targetTable,
			SortedMap<String, Column> sourceColumnName2Column, SortedMap<String, Column> targetColumnName2Column) throws Exception {
		requireNonNull(sourceTable, "sourceTable");
		requireNonNull(targetTable, "targetTable");
		requireNonNull(sourceColumnName2Column, "sourceColumnName2Column");
		requireNonNull(targetColumnName2Column, "targetColumnName2Column");

		// Without explicitly declaring the time-zone via this calendar, the timestamps are not correctly copied!
		final Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

		boolean sourceAutoCommit = sourceConnection.getAutoCommit();
		boolean targetAutoCommit = targetConnection.getAutoCommit();
		try {
			sourceConnection.setAutoCommit(false);
			targetConnection.setAutoCommit(false);

			SortedMap<Column, Column> sourceColumn2TargetColumn = new TreeMap<Column, Column>();
			for (Column sourceColumn : sourceColumnName2Column.values()) {
				Column targetColumn = targetColumnName2Column.get(sourceColumn.name.toUpperCase(Locale.UK));
				if (targetColumn == null) {
					continue;
				}
				sourceColumn2TargetColumn.put(sourceColumn, targetColumn);
			}

			try (Statement deleteStatement = targetConnection.createStatement()) {
				String sql = String.format("delete from \"%s\"", targetTable.name);
				logger.debug("copyTableData: Executing: {}", sql);
				int rowsAffected = deleteStatement.executeUpdate(sql);
				logger.debug("copyTableData: Deleted {} rows from '{}'.", rowsAffected, targetTable.name);
			}
			targetConnection.commit();

			final long sourceTableRowCount;
			try (Statement sourceStatement = sourceConnection.createStatement()) {
				String sql = String.format("select count(*) from \"%s\"", sourceTable.name);

				logger.debug("copyTableData: Executing: {}", sql);
				try (ResultSet rs = sourceStatement.executeQuery(sql)) {
					if (! rs.next())
						throw new IllegalStateException("'SELECT count(*)' failed to return a row!");

					sourceTableRowCount = rs.getLong(1);

					if (rs.next())
						throw new IllegalStateException("'SELECT count(*)' returned multiple rows!");
				}

				logger.info("copyTableData: Table '{}' contains {} rows to be copied.",
						sourceTable.name.toUpperCase(Locale.UK), sourceTableRowCount);

				sql = "select ";
				int idx = 0;
				for (Map.Entry<Column, Column> me : sourceColumn2TargetColumn.entrySet()) {
					Column sourceColumn = me.getKey();
					if (++idx > 1)
						sql += ", ";

					sql += "\"" + sourceColumn.name + "\"";
				}
				sql += " from \"" + sourceTable.name + "\"";

				logger.debug("copyTableData: Executing: {}", sql);
				try (ResultSet rs = sourceStatement.executeQuery(sql)) {

					sql = "insert into \"" + targetTable.name + "\" (";
					idx = 0;
					for (Map.Entry<Column, Column> me : sourceColumn2TargetColumn.entrySet()) {
						Column targetColumn = me.getValue();
						if (++idx > 1)
							sql += ", ";

						sql += "\"" + targetColumn.name + "\"";
					}

					sql += ") values (";
					idx = 0;
					for (Map.Entry<Column, Column> me : sourceColumn2TargetColumn.entrySet()) {
						if (++idx > 1)
							sql += ", ";

						sql += "?";
					}
					sql += ")";

					logger.debug("copyTableData: Preparing: {}", sql);
					try (PreparedStatement insertStatement = targetConnection.prepareStatement(sql)) {
						long rowCountProcessed = 0;
						while (rs.next()) {
							idx = 0;
							for (Map.Entry<Column, Column> me : sourceColumn2TargetColumn.entrySet()) {
								Column sourceColumn = me.getKey();
								Column targetColumn = me.getValue();
								Object sourceValue = rs.getObject(++idx);

								if (sourceValue instanceof Timestamp)
									sourceValue = rs.getTimestamp(idx, utc);
								else if (sourceValue instanceof java.sql.Date)
									sourceValue = rs.getDate(idx, utc);
								else if (sourceValue instanceof java.sql.Time)
									sourceValue = rs.getTime(idx, utc);

								Object targetValue = convertValue(sourceColumn, targetColumn, sourceValue);
								if (logger.isTraceEnabled()) {
									logger.trace("copyTableData: tableName={} columnName={} columnIndex={} sourceJdbcType={} sourceValue.class={} sourceValue={} targetJdbcType={} targetValue.class={} targetValue={}",
											targetTable.name, targetColumn.name, idx,
											getJdbcTypeAsString(sourceColumn.dataType), (sourceValue == null ? null : sourceValue.getClass().getName()), sourceValue,
											getJdbcTypeAsString(targetColumn.dataType), (targetValue == null ? null : targetValue.getClass().getName()), targetValue);
								}
								if (targetValue == null) {
									int dataType = targetColumn.dataType;
									insertStatement.setNull(idx, dataType);
								}
								else {
									if (targetValue instanceof Timestamp)
										insertStatement.setTimestamp(idx, (Timestamp) targetValue, utc);
									else if (targetValue instanceof java.sql.Date)
										insertStatement.setDate(idx, (java.sql.Date) targetValue, utc);
									else if (targetValue instanceof java.sql.Time)
										insertStatement.setTime(idx, (java.sql.Time) targetValue, utc);
									else
										insertStatement.setObject(idx, targetValue);
								}
							}
							int rowsAffected = insertStatement.executeUpdate();
							if (rowsAffected != 1)
								throw new IllegalStateException("INSERT caused rowsAffected=" + rowsAffected);


							++rowCountProcessed;
							if ((rowCountProcessed % 1000) == 0 || rowCountProcessed == sourceTableRowCount) {
								targetConnection.commit();
								logger.info("copyTableData: Table '{}': {} of {} rows have been copied.",
										sourceTable.name.toUpperCase(Locale.UK), rowCountProcessed, sourceTableRowCount);
							}
						}
					}
				}
			}
			sourceConnection.commit();
			targetConnection.commit();
		} finally {
			sourceConnection.rollback();
			targetConnection.rollback();
			sourceConnection.setAutoCommit(sourceAutoCommit);
			targetConnection.setAutoCommit(targetAutoCommit);
		}
	}

	private static Map<Integer, String> jdbcTypeIntToString;

	protected String getJdbcTypeAsString(int dataType) {
		if (jdbcTypeIntToString == null) {
			try {
				Map<Integer, String> m = new HashMap<Integer, String>();
				for (Field field : Types.class.getFields()) {
					if (field.isSynthetic())
						continue;

					if ((field.getModifiers() & Modifier.STATIC) == 0)
						continue;

					if (field.getType() != Integer.class && field.getType() != int.class)
						continue;

					Integer value = (Integer) field.get(null);
					m.put(value, field.getName() + "(" + value + ")");
				}
				jdbcTypeIntToString = Collections.unmodifiableMap(m);
			} catch (Exception e) {
				logger.warn("getJdbcTypeAsString: " + e, e);
				jdbcTypeIntToString = Collections.emptyMap();
			}
		}

		String string = jdbcTypeIntToString.get(dataType);
		if (string != null)
			return string;

		return Integer.toString(dataType);
	}

	protected Object convertValue(Column sourceColumn, Column targetColumn, Object sourceValue) throws Exception {
		if (sourceValue == null)
			return sourceValue;

		if (sourceValue instanceof Clob) {
			Clob sourceClob = (Clob) sourceValue;
			long length = sourceClob.length();
			if (length > Integer.MAX_VALUE)
				throw new IllegalStateException("sourceClob.length > Integer.MAX_VALUE!!!");

			String string = sourceClob.getSubString(1, (int) length);
			return string;
		}

		if (sourceValue instanceof Blob) {
			Blob sourceBlob = (Blob) sourceValue;
			long length = sourceBlob.length();
			if (length > Integer.MAX_VALUE)
				throw new IllegalStateException("sourceBlob.length > Integer.MAX_VALUE!!!");

			byte[] bytes = sourceBlob.getBytes(1L, (int) length);
			return bytes;
		}

		if (sourceColumn.dataType == targetColumn.dataType)
			return sourceValue;

		switch (targetColumn.dataType) {
			case Types.BOOLEAN:
			case Types.BIT: // PostgreSQL returns BIT even though it shows 'boolean' as data-type in the pgAdmin3 -- strange, but true.
				return toBoolean(sourceValue.toString());
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
				if (sourceValue instanceof Boolean) {
					return ((Boolean)sourceValue).booleanValue() ? "Y" : "N";
				}
				return sourceValue.toString();

			default:
				return sourceValue;
		}
	}

	protected boolean toBoolean(String string) {
		requireNonNull(string, "string");
		if (string.startsWith("y") || string.startsWith("Y"))
			return true;

		if (string.startsWith("n") || string.startsWith("N"))
			return false;

		if (string.startsWith("t") || string.startsWith("T"))
			return true;

		if (string.startsWith("f") || string.startsWith("F"))
			return false;

		if (string.startsWith("1"))
			return true;

		if (string.startsWith("0"))
			return false;

		throw new IllegalArgumentException("string cannot be interpreted as boolean: " + string);
	}

	protected SortedMap<String, Column> getColumnName2ColumnMap(Collection<Column> columns) {
		requireNonNull(columns, "columns");
		SortedMap<String, Column> columnName2Colum = new TreeMap<String, Column>();
		for (Column column : columns) {
			columnName2Colum.put(column.name.toUpperCase(Locale.UK), column);
		}
		return columnName2Colum;
	}

	protected Map<Table, List<Column>> getTable2Columns(Connection connection, Set<Table> tables) throws Exception {
		requireNonNull(connection, "connection");
		requireNonNull(tables, "tables");

		Map<Table, Table> table2table = new HashMap<Table, Table>();
		Map<Table, List<Column>> table2Columns = new HashMap<Table, List<Column>>();
		for (Table table : tables) {
			table2table.put(table, table);
			table2Columns.put(table, new ArrayList<Column>());
		}

		try (ResultSet rs = connection.getMetaData().getColumns(null, null, null, null)) {
			while (rs.next()) {
				String catalogue = rs.getString("TABLE_CAT");
				String schema = rs.getString("TABLE_SCHEM");
				String tableName = rs.getString("TABLE_NAME");

				String columnName = rs.getString("COLUMN_NAME");
				int dataType = rs.getInt("DATA_TYPE");
				int size = rs.getInt("COLUMN_SIZE");
				String defaultValue = rs.getString("COLUMN_DEF");
				String autoInc = rs.getString("IS_AUTOINCREMENT");
				Boolean autoIncrement;
				if (autoInc == null || autoInc.trim().isEmpty())
					// autoIncrement = null;
					throw new IllegalStateException("Unknown 'IS_AUTOINCREMENT' not supported by us!");
				else if ("yes".equalsIgnoreCase(autoInc))
					autoIncrement = true;
				else if ("no".equalsIgnoreCase(autoInc))
					autoIncrement = false;
				else
					throw new IllegalStateException("Illegal value for 'IS_AUTOINCREMENT': " + autoInc);

				Table table = new Table(catalogue, schema, tableName);
				List<Column> columns = table2Columns.get(table);
				if (columns == null) {
					logger.trace("getTable2Columns: Ignoring column '{}' for ignored table '{}'!", columnName, tableName);
				} else {
					table = requireNonNull(table2table.get(table), "table2table.get(" + table + ")");
					Column column = new Column(table, columnName, dataType, size, autoIncrement);
					columns.add(column);
				}
			}
		}
		return table2Columns;
	}

	protected void createPersistenceManagerFactories() {
		closePersistenceManagerFactories(); // just in case...

		PersistencePropertiesProvider ppp = new PersistencePropertiesProvider(repositoryId, localRoot);
		Map<String, String> sourcePersistenceProperties = ppp.getPersistenceProperties();

		ppp = new PersistencePropertiesProvider(repositoryId, targetLocalRoot);
		ppp.setOverridePersistencePropertiesFile(getTargetPersistencePropertiesFile());
		Map<String, String> targetPersistenceProperties = ppp.getPersistenceProperties();

		sourcePmf = JDOHelper.getPersistenceManagerFactory(sourcePersistenceProperties);
		sourcePm = sourcePmf.getPersistenceManager();

		targetPmf = JDOHelper.getPersistenceManagerFactory(targetPersistenceProperties);
		targetPm = targetPmf.getPersistenceManager();

		CloudStorePersistenceCapableClassesProvider.Helper.initPersistenceCapableClasses(sourcePm);
		CloudStorePersistenceCapableClassesProvider.Helper.initPersistenceCapableClasses(targetPm);
	}

	protected void createJdbcConnections() {
		closeJdbcConnections(); // just in case...
		try {
			sourceDbAdapter = sourceDbAdapterFactory.createDatabaseAdapter();
			sourceDbAdapter.setRepositoryId(repositoryId);
			sourceDbAdapter.setLocalRoot(localRoot);

			targetDbAdapter = targetDbAdapterFactory.createDatabaseAdapter();
			targetDbAdapter.setRepositoryId(repositoryId);
			targetDbAdapter.setLocalRoot(targetLocalRoot);

			sourceConnection = sourceDbAdapter.createConnection();
			targetConnection = targetDbAdapter.createConnection();
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RuntimeException(x);
		}
	}

	protected void closeJdbcConnections() {
		try {
			if (sourceConnection != null) {
				sourceConnection.close(); sourceConnection = null;
			}
			if (targetConnection != null) {
				targetConnection.close(); targetConnection = null;
			}
			if (sourceDbAdapter != null) {
				sourceDbAdapter.close(); sourceDbAdapter = null;
			}
			if (targetDbAdapter != null) {
				targetDbAdapter.close(); targetDbAdapter = null;
			}
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RuntimeException(x);
		}
	}

	protected void closePersistenceManagerFactories() {
		if (sourcePm != null) {
			if (sourcePm.currentTransaction().isActive())
				sourcePm.currentTransaction().rollback();

			sourcePm.close(); sourcePm = null;
		}
		if (targetPm != null) {
			if (targetPm.currentTransaction().isActive())
				targetPm.currentTransaction().rollback();

			targetPm.close(); targetPm = null;
		}
		if (sourcePmf != null) {
			sourcePmf.close(); sourcePmf = null;
		}
		if (targetPmf != null) {
			targetPmf.close(); targetPmf = null;
		}
	}

	protected void createTargetPersistencePropertiesAndDatabase() throws Exception {
		requireNonNull(sourceDbAdapterFactory, "sourceDbAdapterFactory");
		requireNonNull(targetDbAdapterFactory, "targetDbAdapterFactory");

		targetLocalRoot.mkdir();
		targetMetaDir.mkdir();
		if (! targetMetaDir.isDirectory())
			throw new IllegalStateException("Creating directory failed: " + targetMetaDir.getAbsolutePath());

		if (getTargetPersistencePropertiesFile().exists()) {
			File directory = getTargetPersistencePropertiesFile().getParentFile();
			File backupFile = directory.createFile(
					getTargetPersistencePropertiesFile().getName() + ".bak_" + Long.toHexString(System.currentTimeMillis()));
			getTargetPersistencePropertiesFile().renameTo(backupFile);

			if (getTargetPersistencePropertiesFile().exists())
				throw new IOException(String.format("Renaming file '%s' to '%s' (in directory '%s') failed!",
						getTargetPersistencePropertiesFile().getName(), backupFile.getName(), directory.getAbsolutePath()));
		}

		try (DatabaseAdapter targetDatabaseAdapter = targetDbAdapterFactory.createDatabaseAdapter()) {
			targetDatabaseAdapter.setRepositoryId(repositoryId);
			targetDatabaseAdapter.setLocalRoot(targetLocalRoot);
			targetDatabaseAdapter.createPersistencePropertiesFileAndDatabase();
		}

		if (! getTargetPersistencePropertiesFile().exists())
			throw new IOException(String.format("Creating persistence-properties '%s' failed!",
					getTargetPersistencePropertiesFile().getAbsolutePath()));
	}

	protected File getSourcePersistencePropertiesFile() {
		return metaDir.createFile(PERSISTENCE_PROPERTIES_FILE_NAME);
	}

	protected File getTargetPersistencePropertiesFile() {
		return targetMetaDir.createFile(PERSISTENCE_PROPERTIES_FILE_NAME);
	}

	protected Properties readRawPersistenceProperties() throws IOException {
		final File persistencePropertiesFile = createFile(metaDir, LocalRepoManager.PERSISTENCE_PROPERTIES_FILE_NAME);
		if (!persistencePropertiesFile.isFile())
			throw new IllegalStateException("The persistencePropertiesFile does not exist or is not a file: " + persistencePropertiesFile.getAbsolutePath());

		Properties rawProperties = PropertiesUtil.load(persistencePropertiesFile);
		return rawProperties;
	}

	protected void readStatus() throws IOException {
		File statusFile = getStatusFile();
		if (statusFile.exists()) {
			try (IInputStream in = getStatusFile().createInputStream()) {
				status.load(castStream(in));
			}
		}
	}

	protected void writeStatus() throws IOException {
		try (IOutputStream out = getStatusFile().createOutputStream()) {
			status.store(castStream(out), null);
		}
	}

	protected File getTriggerFile() {
		return metaDir.createFile(DBMIGRATE_TRIGGER_FILE_NAME);
	}

	protected File getStatusFile() {
		return metaDir.createFile(DBMIGRATE_STATUS_FILE_NAME);
	}

	public boolean isMigrationInProcess() {
		if (repositoryId == null)
			return false;

		final File persistencePropertiesFile = createFile(metaDir, LocalRepoManager.PERSISTENCE_PROPERTIES_FILE_NAME);
		if (! persistencePropertiesFile.isFile())
			return false;

		File statusFile = getStatusFile();
		File triggerFile = getTriggerFile();
		return statusFile.exists() || ! triggerFile.exists();
	}

	public void createTriggerFile() {
		try {
			File triggerFile = getTriggerFile();
			triggerFile.createNewFile();

			if (! triggerFile.isFile())
				throw new IOException("Creating file failed: " + triggerFile.getAbsolutePath());

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Perform a few smoke-tests to see whether the target-database is actually usable.
	 */
	protected void testTargetPersistence() throws Exception {
		requireNonNull(targetPm, "targetPm");
		String nf0Name = "test_" + Long.toString(System.currentTimeMillis(), 36) + "_" + new Uid();
		long nf0Id;

		targetPm.currentTransaction().begin();
		try {
			NormalFileDao nfDao = getDao(NormalFileDao.class);
			RepoFileDao rfDao = getDao(RepoFileDao.class);

			RepoFile rootDir = rfDao.getChildRepoFile(null, "");
			requireNonNull(rootDir, "rootDir");

			if (! (rootDir instanceof Directory))
				throw new IllegalStateException("rootDir is an instance of " + rootDir.getClass().getName() + ", but it must be an instance of Directory: " + rootDir);

			NormalFile nf = createObject(NormalFile.class);
			nf.setLastModified(new Date());
			nf.setParent(rootDir);
			nf.setName(nf0Name);
			nf.setSha1("xyz");
			nf.setLength(666);

			nf = rfDao.makePersistent(nf);
			nfDao.getPersistenceManager().flush();

			nf0Id = nf.getId();

			nfDao.getObjectByIdOrFail(nf0Id);

			RepoFile nf0 = rfDao.getChildRepoFile(rootDir, nf0Name);
			requireNonNull(nf0, "nf0");
			if (nf0.getId() != nf0Id)
				throw new IllegalStateException(String.format("nf0.getId() != nf0Id :: %s != %s", nf0.getId(), nf0Id));
		} finally {
			targetPm.currentTransaction().rollback();
		}

		targetPm.currentTransaction().begin();
		try {
			NormalFileDao nfDao = getDao(NormalFileDao.class);
			RepoFileDao rfDao = getDao(RepoFileDao.class);

			RepoFile rootDir = rfDao.getChildRepoFile(null, "");
			requireNonNull(rootDir, "rootDir");

			RepoFile nf0 = rfDao.getChildRepoFile(rootDir, nf0Name);
			if (nf0 != null)
				throw new IllegalStateException("Data written in rolled-back transaction is still there!");

			nf0 = nfDao.getObjectByIdOrNull(nf0Id);
			if (nf0 != null)
				throw new IllegalStateException("Data written in rolled-back transaction is still there!");
		} finally {
			targetPm.currentTransaction().rollback();
		}

		sourcePm.currentTransaction().begin();
		targetPm.currentTransaction().begin();
		try {
			for (Class<?> pcClass : CloudStorePersistenceCapableClassesProvider.Helper.getPersistenceCapableClasses()) {
				if (! Entity.class.isAssignableFrom(pcClass))
					continue;

				if ((pcClass.getModifiers() & Modifier.ABSTRACT) != 0)
					continue;

				comparePersistentObjects(pcClass);
			}
		} finally {
			sourcePm.currentTransaction().rollback();
			targetPm.currentTransaction().rollback();
		}
	}

	protected void comparePersistentObjects(final Class<?> pcClass) {
		requireNonNull(pcClass, "pcClass");
		final long idBlockSize = 1000;
		final long minId = getMinId(sourcePm, pcClass); final long targetMinId = getMinId(targetPm, pcClass);
		final long maxId = getMaxId(sourcePm, pcClass); final long targetMaxId = getMaxId(targetPm, pcClass);

		if (minId != targetMinId)
			throw new IllegalStateException(String.format("%s: sourceMinId != targetMinId :: %d != %d", pcClass.getName(), minId, targetMinId));

		if (maxId != targetMaxId)
			throw new IllegalStateException(String.format("%s: sourceMaxId != targetMaxId :: %d != %d", pcClass.getName(), maxId, targetMaxId));

		if (minId == Long.MIN_VALUE || maxId == Long.MIN_VALUE) {
			if (minId != maxId)
				throw new IllegalStateException(String.format("%s: minId != maxId :: %d != %d", pcClass.getName(), minId, maxId));

			logger.debug("comparePersistentObjects: pcClass={}: *EMPTY*", pcClass.getName());
			return;
		}
		logger.debug("comparePersistentObjects: pcClass={}: minId={} maxId={}", pcClass.getName(), minId, maxId);

		long objectCount = 0;
		long fromIdIncl = minId;
		while (fromIdIncl <= maxId) {
			long toIdExcl = fromIdIncl + idBlockSize;
			objectCount += comparePersistentObjects(pcClass, fromIdIncl, toIdExcl);
			fromIdIncl = toIdExcl;
		}
		logger.info("comparePersistentObjects: pcClass={}: {} objects are equal.", pcClass.getName(), objectCount);
	}

	protected int comparePersistentObjects(final Class<?> pcClass, final long fromIdIncl, final long toIdExcl) {
		requireNonNull(pcClass, "pcClass");
		final List<Entity> sourceObjects = getPersistentObjects(sourcePm, pcClass, fromIdIncl, toIdExcl);
		final List<Entity> targetObjects = getPersistentObjects(targetPm, pcClass, fromIdIncl, toIdExcl);

		final int result = sourceObjects.size();
		if (result != targetObjects.size())
			throw new IllegalStateException(String.format("%s: fromIdIncl=%d toIdExcl=%d :: sourceObjects.size != targetObjects.size :: %d != %d",
					pcClass.getName(), fromIdIncl, toIdExcl, result, targetObjects.size()));

		final Iterator<Entity> targetIterator = targetObjects.iterator();
		for (final Entity sourceObject : sourceObjects) {
			final Entity targetObject = targetIterator.next();

			if (sourceObject.getId() != targetObject.getId())
				throw new IllegalStateException(String.format("%s: fromIdIncl=%d toIdExcl=%d :: sourceObject.id != targetObjects.id :: %d != %d",
						pcClass.getName(), fromIdIncl, toIdExcl, sourceObject.getId(), targetObject.getId()));

			comparePersistentObject(sourceObject, targetObject);
		}
		logger.debug("comparePersistentObjects: pcClass={} fromIdIncl={} toIdExcl={}: {} objects are equal.",
				pcClass.getName(), fromIdIncl, toIdExcl, result);

		sourcePm.currentTransaction().rollback();
		sourcePm.evictAll();
		sourcePm.currentTransaction().begin();

		targetPm.currentTransaction().rollback();
		targetPm.evictAll();
		targetPm.currentTransaction().begin();
		DebugUtil.logMemoryStats(logger);
		return result;
	}

	protected void comparePersistentObject(final Entity sourceObject, final Entity targetObject) {
		requireNonNull(sourceObject, "sourceObject");
		requireNonNull(targetObject, "targetObject");

		if (sourceObject.getId() != targetObject.getId())
			throw new IllegalStateException(String.format("sourceObject.id != targetObjects.id :: %d != %d",
					sourceObject.getId(), targetObject.getId()));

		final Class<? extends Entity> objectClass = sourceObject.getClass();
		if (objectClass != targetObject.getClass())
			throw new IllegalStateException(String.format("sourceObject.class != targetObjects.class :: %s != %s",
					objectClass.getName(), targetObject.getClass().getName()));

		for (final Method getter : getGetters(objectClass)) {
			final String propertyName = getPropertyName(getter);
			final Object sourceValue = invokeGetter(getter, sourceObject);
			final Object targetValue = invokeGetter(getter, targetObject);
			comparePropertyValue(sourceObject, targetObject, propertyName, sourceValue, targetValue);
		}
	}

	protected void comparePropertyValue(final Entity sourceObject, final Entity targetObject, final String propertyName,
			final Object sourceValue, final Object targetValue) {
		requireNonNull(sourceObject, "sourceObject");
		requireNonNull(targetObject, "targetObject");
		requireNonNull(propertyName, "propertyName");
		// sourceValue may be null!
		// targetValue may be null!

		if (sourceValue == null) {
			if (targetValue == null)
				return;

			throw new IllegalStateException(String.format("Property '%s' of %s differs between source and target: sourceValue=null targetValue='%s'",
					propertyName, sourceObject, targetValue));
		}
		if (targetValue == null)
			throw new IllegalStateException(String.format("Property '%s' of %s differs between source and target: sourceValue='%s' targetValue=null",
					propertyName, sourceObject, sourceValue));

		if (sourceValue.getClass() != targetValue.getClass())
			throw new IllegalStateException(String.format("Property '%s' of %s differs between source and target: Class mismatch! sourceValue.class=%s targetValue.class=%s sourceValue='%s' targetValue='%s'",
					propertyName, sourceObject, sourceValue.getClass().getName(), targetValue.getClass().getName(), sourceValue, targetValue));

		if (sourceValue.getClass().isArray()) {
			if (! arrayEquals(sourceValue, targetValue))
				throw new IllegalStateException(String.format("Property '%s' of %s differs between source and target: sourceValue=%s targetValue=%s",
						propertyName, sourceObject, arrayToString(sourceValue), arrayToString(targetValue)));
		}
		else {
			if (! sourceValue.equals(targetValue))
				throw new IllegalStateException(String.format("Property '%s' of %s differs between source and target: sourceValue='%s' targetValue='%s'",
						propertyName, sourceObject, sourceValue, targetValue));
		}
	}

	protected static String arrayToString(final Object array) {
		requireNonNull(array, "array");

		if (array instanceof boolean[])
			return Arrays.toString((boolean[]) array);

		if (array instanceof byte[])
			return Arrays.toString((byte[]) array);

		if (array instanceof char[])
			return Arrays.toString((char[]) array);

		if (array instanceof double[])
			return Arrays.toString((double[]) array);

		if (array instanceof float[])
			return Arrays.toString((float[]) array);

		if (array instanceof int[])
			return Arrays.toString((int[]) array);

		if (array instanceof long[])
			return Arrays.toString((long[]) array);

		if (array instanceof short[])
			return Arrays.toString((short[]) array);

		if (array instanceof Object[])
			return Arrays.toString((Object[]) array);

		throw new IllegalArgumentException("Unexpected type: " + array.getClass().getName());
	}

	protected static boolean arrayEquals(final Object sourceArray, final Object targetArray) {
		requireNonNull(sourceArray, "sourceArray");
		requireNonNull(targetArray, "targetArray");

		if (sourceArray instanceof boolean[])
			return Arrays.equals((boolean[]) sourceArray, (boolean[]) targetArray);

		if (sourceArray instanceof byte[])
			return Arrays.equals((byte[]) sourceArray, (byte[]) targetArray);

		if (sourceArray instanceof char[])
			return Arrays.equals((char[]) sourceArray, (char[]) targetArray);

		if (sourceArray instanceof double[])
			return Arrays.equals((double[]) sourceArray, (double[]) targetArray);

		if (sourceArray instanceof float[])
			return Arrays.equals((float[]) sourceArray, (float[]) targetArray);

		if (sourceArray instanceof int[])
			return Arrays.equals((int[]) sourceArray, (int[]) targetArray);

		if (sourceArray instanceof long[])
			return Arrays.equals((long[]) sourceArray, (long[]) targetArray);

		if (sourceArray instanceof short[])
			return Arrays.equals((short[]) sourceArray, (short[]) targetArray);

		if (sourceArray instanceof Object[])
			return Arrays.equals((Object[]) sourceArray, (Object[]) targetArray);

		throw new IllegalArgumentException("Unexpected type: " + sourceArray.getClass().getName());
	}

	private Map<Class<?>, List<Method>> objectClass2Getters = new HashMap<Class<?>, List<Method>>();

	protected Object invokeGetter(final Method getter, final Object object) {
		requireNonNull(getter, "getter");
		requireNonNull(object, "object");
		try {
			return getter.invoke(object);
		} catch (Exception e) {
			throw new RuntimeException(String.format("Getter for property '%s' on %s failed: %s", getPropertyName(getter), object, e), e);
		}
	}

	protected List<Method> getGetters(Class<? extends Entity> objectClass) {
		requireNonNull(objectClass, "objectClass");

		List<Method> getters = objectClass2Getters.get(objectClass);
		if (getters == null) {
			getters = new ArrayList<Method>();
			for (Method method : objectClass.getMethods()) {
				if (method.getParameterCount() == 0 && isGetterName(method.getName())) {
					IgnoreDatabaseMigraterComparison ignore = method.getAnnotation(IgnoreDatabaseMigraterComparison.class);
					if (ignore == null)
						getters.add(method);
				}
			}
			objectClass2Getters.put(objectClass, getters);
		}
		return getters;
	}

	protected static String getPropertyName(Method method) {
		String methodName = requireNonNull(method, "method").getName();
		String propertyName;
		if (methodName.startsWith("get"))
			propertyName = methodName.substring(3);
		else if (methodName.startsWith("is"))
			propertyName = methodName.substring(2);
		else
			throw new IllegalArgumentException("method.name is not a valid getter-name (wrong prefix): " + method);

		if (propertyName.isEmpty())
			throw new IllegalArgumentException("method.name is not a valid getter-name (too short): " + method);

		return Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
	}

	protected static boolean isGetterName(final String methodName) {
		requireNonNull(methodName, "methodName");
		return (methodName.startsWith("get") && methodName.length() > 3)
				|| (methodName.startsWith("is") && methodName.length() > 2);
	}

	protected long getMinId(PersistenceManager pm, Class<?> pcClass) {
		requireNonNull(pm, "pm");
		requireNonNull(pcClass, "pcClass");
		Query<?> query = pm.newQuery(pcClass);

		Long result = (Long) query
				.result("min(this.id)")
				.execute();

		query.closeAll();
		return result == null ? Long.MIN_VALUE : result;
	}

	protected long getMaxId(PersistenceManager pm, Class<?> pcClass) {
		requireNonNull(pm, "pm");
		requireNonNull(pcClass, "pcClass");
		Query<?> query = pm.newQuery(pcClass);

		Long result = (Long) query
				.result("max(this.id)")
				.execute();

		query.closeAll();
		return result == null ? Long.MIN_VALUE : result;
	}

	protected List<Entity> getPersistentObjects(PersistenceManager pm, Class<?> pcClass, long fromIdIncl, long toIdExcl) {
		requireNonNull(pm, "pm");
		requireNonNull(pcClass, "pcClass");

		Query<?> query = pm.newQuery(pcClass);

		FetchPlan fetchPlan = query.getFetchPlan();
		fetchPlan.clearGroups();
		fetchPlan.addGroup(FetchGroup.ALL);
		fetchPlan.setMaxFetchDepth(1);

		@SuppressWarnings("unchecked")
		List<Entity> result = (List<Entity>) query
				.filter("this.id >= :fromIdIncl && this.id < :toIdExcl")
				.orderBy("this.id ASCENDING")
				.execute(fromIdIncl, toIdExcl);

		result = new ArrayList<Entity>(result);
		query.closeAll();
		return result;
	}

	@Override
	public <D> D getDao(final Class<D> daoClass) {
		requireNonNull(daoClass, "daoClass");

		@SuppressWarnings("unchecked")
		D dao = (D) daoClass2Dao.get(daoClass);

		if (dao != null && ((Dao<?, ?>)dao).getPersistenceManager() != targetPm)
			dao = null;

		if (dao == null) {
			dao = createObject(daoClass);

			if (!(dao instanceof Dao))
				throw new IllegalStateException(String.format("dao class %s does not extend Dao!", daoClass.getName()));

			((Dao<?, ?>)dao).setPersistenceManager(targetPm);
			((Dao<?, ?>)dao).setDaoProvider(this);

			daoClass2Dao.put(daoClass, dao);
		}
		return dao;
	}
}
