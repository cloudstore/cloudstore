package co.codewizards.cloudstore.client;

import co.codewizards.cloudstore.core.updater.CloudStoreUpdaterCore;
import co.codewizards.cloudstore.core.updater.Version;

/**
 * <p>
 * {@link SubCommand} implementation being called after an update.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class AfterUpdateHookSubCommand extends SubCommand {

	@Override
	public String getSubCommandDescription() {
		return "Callback hook being invoked after an update.";
	}

	@Override
	public void run() throws Exception {
		System.out.println("Update completed! New local version: " + VersionSubCommand.getVersion());
	}

	@Override
	public boolean isVisibleInHelp() {
		return false;
	}
}
