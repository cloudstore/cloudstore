package co.codewizards.cloudstore.core.repo.local;

import java.net.URL;
import java.util.Collection;
import java.util.UUID;

import co.codewizards.cloudstore.core.oio.File;

public interface LocalRepoRegistry {
	String LOCAL_REPO_REGISTRY_FILE = "repoRegistry.properties"; // new name since 0.9.1
	String CONFIG_KEY_EVICT_DEAD_ENTRIES_PERIOD = "repoRegistry.evictDeadEntriesPeriod";
	long DEFAULT_EVICT_DEAD_ENTRIES_PERIOD = 24 * 60 * 60 * 1000L;


	Collection<UUID> getRepositoryIds();

	UUID getRepositoryId(String repositoryName);

	UUID getRepositoryIdOrFail(String repositoryName);

	URL getLocalRootURLForRepositoryNameOrFail(String repositoryName);

	URL getLocalRootURLForRepositoryName(String repositoryName);

	File getLocalRootForRepositoryNameOrFail(String repositoryName);

	/**
	 * Get the local root for the given {@code repositoryName}.
	 * @param repositoryName the String-representation of the repositoryId or
	 * a repositoryAlias. Must not be <code>null</code>.
	 * @return the repository's local root or <code>null</code>, if the given {@code repositoryName} is neither
	 * a repositoryId nor a repositoryAlias known to this registry.
	 */
	File getLocalRootForRepositoryName(String repositoryName);

	File getLocalRoot(UUID repositoryId);

	File getLocalRootOrFail(UUID repositoryId);

	/**
	 * Puts an alias into the registry.
	 * <p>
	 * <b>Important:</b> Do <b>not</b> call this method directly. Most likely, you should use
	 * {@link LocalRepoManager#putRepositoryAlias(String)} instead!
	 * @param repositoryAlias
	 * @param repositoryId
	 */
	void putRepositoryAlias(String repositoryAlias, UUID repositoryId);

	void removeRepositoryAlias(String repositoryAlias);

	void putRepository(UUID repositoryId, File localRoot);

	/**
	 * Gets all aliases known for the specified repository.
	 * @param repositoryName the repository-ID or -alias. Must not be <code>null</code>.
	 * @return the known aliases. Never <code>null</code>, but maybe empty (if there are no aliases for this repository).
	 * @throws IllegalArgumentException if the repository with the given {@code repositoryName} does not exist,
	 * i.e. it's neither a repository-ID nor a repository-alias of a known repository.
	 */
	Collection<String> getRepositoryAliasesOrFail(String repositoryName)
			throws IllegalArgumentException;

	/**
	 * Gets all aliases known for the specified repository.
	 * @param repositoryName the repository-ID or -alias. Must not be <code>null</code>.
	 * @return the known aliases. <code>null</code>, if there is no repository with
	 * the given {@code repositoryName}. Empty, if the repository is known, but there
	 * are no aliases for it.
	 */
	Collection<String> getRepositoryAliases(String repositoryName);

}