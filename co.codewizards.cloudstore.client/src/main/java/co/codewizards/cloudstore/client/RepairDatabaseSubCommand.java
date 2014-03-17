package co.codewizards.cloudstore.client;

import java.util.Map;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.PersistencePropertiesProvider;

/**
 * {@link SubCommand} implementation for showing information about a repository in the local file system.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class RepairDatabaseSubCommand extends SubCommandWithExistingLocalRepo
{
	public RepairDatabaseSubCommand() { }

	@Override
	public String getSubCommandDescription() {
		return "Check and repair the Derby database.";
	}

	@Override
	public void run() throws Exception {
		Map<String, String> persistenceProperties = new PersistencePropertiesProvider(localRoot).getPersistenceProperties(false);
		String connectionURL = persistenceProperties.get(LocalRepoManager.CONNECTION_URL_KEY);

		// http://objectmix.com/apache/646586-derby-db-files-get-corrupted-2.html
		// SELECT schemaname, tablename, SYSCS_UTIL.SYSCS_CHECK_TABLE(schemaname, tablename) FROM sys.sysschemas s, sys.systables t WHERE s.schemaid = t.schemaid
		// TODO execute the above command!
		// TODO maybe additionally drop all indices (maybe depending on arguments?)

		new RepoInfoSubCommand(localRoot).run();
	}
}
