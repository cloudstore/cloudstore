package co.codewizards.cloudstore.client;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;

import org.kohsuke.args4j.Argument;

import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;

public abstract class SubCommandWithExistingLocalRepo extends SubCommand {

	@Argument(metaVar="<local>", required=true, index=0, usage="A path inside a repository in the local file system or a "
			+ "repository-ID or a repository-alias (optionally with a path).\n\nIf this matches both a locally "
			+ "existing directory and a repository-ID/-alias, it is assumed to be a repository-ID/-alias. "
			+ "Note, that it may be a sub-directory inside the repository specified in the form "
			+ "<repositoryID>/path (this must be a '/' even on Windows).")
	protected String local;

	/**
	 * {@link File} referencing a directory inside the repository (or its root).
	 */
	protected File localFile;

	/**
	 * The root directory of the repository.
	 * <p>
	 * This may be the same as {@link #localFile} or it may be
	 * a direct or indirect parent-directory of {@code #localFile}.
	 */
	protected File localRoot;

	@Override
	public void prepare() throws Exception {
		super.prepare();
		assertNotNull("local", local);

		String path;
		String repositoryName;
		int slashIndex = local.indexOf('/');
		if (slashIndex < 0) {
			repositoryName = local;
			path = "";
		}
		else {
			repositoryName = local.substring(0, slashIndex);
			path = local.substring(slashIndex + 1);
		}

		localRoot = LocalRepoRegistry.getInstance().getLocalRootForRepositoryName(repositoryName);
		if (localRoot != null)
			localFile = path.isEmpty() ? localRoot : new File(localRoot, path);
		else {
			localFile = new File(local).getAbsoluteFile();
			local = localFile.getPath();
		}
	}

}
