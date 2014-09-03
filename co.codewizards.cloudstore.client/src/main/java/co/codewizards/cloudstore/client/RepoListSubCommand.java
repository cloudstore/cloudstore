package co.codewizards.cloudstore.client;

import java.util.Collection;
import java.util.UUID;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;

/**
 * {@link SubCommand} implementation for listing all repositories in the local file system
 * (known to the {@link LocalRepoRegistry}).
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class RepoListSubCommand extends SubCommand
{
	public RepoListSubCommand() { }

	@Override
	public String getSubCommandDescription() {
		return "List all local repositories known to the registry.";
	}

	@Override
	public void run() throws Exception {
		LocalRepoRegistry localRepoRegistry = LocalRepoRegistry.getInstance();
		System.out.println("Local repositories:");
		for (UUID repositoryId : localRepoRegistry.getRepositoryIds()) {
			File localRoot = localRepoRegistry.getLocalRoot(repositoryId);
			Collection<String> repositoryAliases = localRepoRegistry.getRepositoryAliasesOrFail(repositoryId.toString());
			System.out.println(String.format("  * repository.repositoryId = %s", repositoryId));
			System.out.println(String.format("    repository.localRoot = %s", localRoot));
			System.out.println(String.format("    repository.aliases = %s", repositoryAliases));
			System.out.println();
		}
	}
}
