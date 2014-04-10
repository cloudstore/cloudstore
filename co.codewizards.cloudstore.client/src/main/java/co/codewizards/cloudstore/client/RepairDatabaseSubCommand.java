package co.codewizards.cloudstore.client;

import co.codewizards.cloudstore.local.RepairDatabase;

/**
 * {@link SubCommand} implementation for repairing a database.
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
		new RepairDatabase(localRoot).run();
		new RepoInfoSubCommand(localRoot).run();
	}
}
