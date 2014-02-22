package co.codewizards.cloudstore.client;

/**
 * <p>
 * {@link SubCommand} implementation for showing the help.
 * </p>
 * <p>
 * Since the 'help' sub-command is currently handled by {@link CloudStoreClient} internally,
 * this is a dummy class at the moment (just to show the 'help' in the help, for example).
 * </p>
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class HelpSubCommand extends SubCommand {

	@Override
	public String getSubCommandDescription() {
		return "Get help.";
	}

	@Override
	public void run() throws Exception {
		throw new UnsupportedOperationException("The help command is handled by the CloudStoreClient itself.");
	}

}
