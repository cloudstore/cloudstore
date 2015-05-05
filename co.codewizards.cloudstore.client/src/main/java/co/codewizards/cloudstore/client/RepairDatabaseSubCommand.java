package co.codewizards.cloudstore.client;

import co.codewizards.cloudstore.local.RepairDatabase;

/**
 * {@link SubCommand} implementation for repairing a database.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class RepairDatabaseSubCommand extends SubCommandWithExistingLocalRepo
{
	private RepairDatabase repairDatabase;
	private RepoInfoSubCommand repoInfoSubCommand;

	public RepairDatabaseSubCommand() { }

	@Override
	public String getSubCommandDescription() {
		return "Check and repair the Derby database.";
	}

	@Override
	public void prepare() throws Exception {
		super.prepare();
		repairDatabase = new RepairDatabase(localRoot);

		repoInfoSubCommand = new RepoInfoSubCommand(localRoot);
		repoInfoSubCommand.prepare();
	}

	@Override
	public void run() throws Exception {
		repairDatabase.run();
		repoInfoSubCommand.run();
	}

	@Override
	public void cleanUp() throws Exception {
		repoInfoSubCommand.cleanUp();
		super.cleanUp();
	}
}
