package co.codewizards.cloudstore.client;

import java.io.File;
import java.util.UUID;

import org.kohsuke.args4j.Argument;

import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;

/**
 * {@link SubCommand} implementation for showing information about a repository in the local file system.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class DropRepoAliasSubCommand extends SubCommand
{
	@Argument(metaVar="<alias>", index=0, required=true, usage="The alias to be dropped.")
	private String alias;

	public DropRepoAliasSubCommand() { }

	@Override
	public String getSubCommandDescription() {
		return "Drop an alias.";
	}

	@Override
	public void run() throws Exception {
		LocalRepoRegistry localRepoRegistry = LocalRepoRegistry.getInstance();
		UUID oldRepositoryId = localRepoRegistry.getRepositoryId(alias);

		File oldLocalRoot = null;
		if (oldRepositoryId != null) {
			oldLocalRoot = localRepoRegistry.getLocalRoot(oldRepositoryId);
			if (oldLocalRoot == null || !oldLocalRoot.exists()) {
				// orphaned entry to be ignored (should be cleaned up after a while, anyway)
				oldRepositoryId = null;
				oldLocalRoot = null;
			}
		}

		if (oldRepositoryId != null) {
			localRepoRegistry.removeRepositoryAlias(alias);
			System.out.println(String.format("Dropped alias '%s' for repository %s (local-root '%s').", alias, oldRepositoryId, oldLocalRoot));
		}
		else {
			System.out.println(String.format("WARNING: The alias '%s' does not exist.", alias));
		}
	}
}
