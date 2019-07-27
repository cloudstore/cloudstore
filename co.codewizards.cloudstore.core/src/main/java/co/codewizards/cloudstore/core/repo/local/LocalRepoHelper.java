package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import co.codewizards.cloudstore.core.oio.File;

public final class LocalRepoHelper {

	private LocalRepoHelper() { }

	/**
	 * Gets the local root containing the given {@code file}.
	 * <p>
	 * If {@code file} itself is the root of a repository, it is returned directly.
	 * <p>
	 * If {@code file} is a directory or file inside the repository, the parent-directory
	 * being the repository's root is returned.
	 * <p>
	 * If {@code file} is not contained in any repository, <code>null</code> is returned.
	 *
	 * @param file the directory or file for which to search the repository's local root. Must not be <code>null</code>.
	 * @return the repository's local root. Is <code>null</code>, if {@code file} is not located inside a repository.
	 */
	public static File getLocalRootContainingFile(final File file) {
		File parentFile = requireNonNull(file, "file");
		while (parentFile != null) {
			final File parentMetaDir = createFile(parentFile, LocalRepoManager.META_DIR_NAME);
			if (parentMetaDir.exists())
				return parentFile;

			parentFile = parentFile.getParentFile();
		}
		return null;
	}

	public static Collection<File> getLocalRootsContainedInDirectory(File directory) {
		requireNonNull(directory, "directory");
		directory = directory.getAbsoluteFile();

		if (! directory.isDirectory())
			return Collections.emptyList();

		final String containerPath = directory.getPath() + java.io.File.separator;

		final List<File> result = new ArrayList<File>();
		final LocalRepoRegistry localRepoRegistry = LocalRepoRegistryImpl.getInstance();
		for (final UUID repositoryId : localRepoRegistry.getRepositoryIds()) {
			final File localRoot = localRepoRegistry.getLocalRoot(repositoryId);
			if (localRoot == null)
				continue;

			if (directory.equals(localRoot))
				result.add(localRoot);
			else {
				final String localRootPath = localRoot.getAbsolutePath();
				if (localRootPath.startsWith(containerPath))
					result.add(localRoot);
			}
		}
		return Collections.unmodifiableList(result);
	}
}
