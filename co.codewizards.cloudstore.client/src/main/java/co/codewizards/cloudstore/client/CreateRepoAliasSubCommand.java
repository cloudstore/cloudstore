package co.codewizards.cloudstore.client;

import java.util.UUID;

import org.kohsuke.args4j.Argument;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.oio.api.File;

/**
 * {@link SubCommand} implementation for showing information about a repository in the local file system.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class CreateRepoAliasSubCommand extends SubCommandWithExistingLocalRepo
{
	@Argument(metaVar="<alias>", index=1, required=true, usage="The alias to be created.")
	private String alias;

	public CreateRepoAliasSubCommand() { }

	@Override
	public String getSubCommandDescription() {
		return "Create an alias for an existing repository.";
	}

	@Override
	public void run() throws Exception {
		LocalRepoRegistry localRepoRegistry = LocalRepoRegistry.getInstance();
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
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

			if (oldRepositoryId != null)
				System.err.println(String.format("ERROR: There is already a repository registered with the alias '%s'! The existing repository's ID is '%s' and its local-root is '%s'.", alias, oldRepositoryId, oldLocalRoot));
			else {
				localRepoManager.putRepositoryAlias(alias);
				System.out.println(String.format("Created alias '%s' for repository %s (local-root '%s').", alias, localRepoManager.getRepositoryId(), localRepoManager.getLocalRoot()));
			}
		} finally {
			localRepoManager.close();
		}
	}
}
