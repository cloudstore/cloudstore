package co.codewizards.cloudstore.client;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;

import org.kohsuke.args4j.Argument;

import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.core.util.UrlUtil;

public abstract class SubCommandWithExistingLocalRepo extends SubCommand {



	@Argument(metaVar="<local>", required=true, index=0, usage="A path inside a repository in the local file system or a "
			+ "repository-ID or a repository-alias (optionally with a path). If this matches both a locally "
			+ "existing directory and a repository-ID/-alias, it is assumed to be a repository-ID/-alias. "
			+ "Note, that it may be a sub-directory inside the repository specified in the form "
			+ "<repositoryId>/path (this must be a '/' even on Windows).")
	protected String local;

	/** Must be an empty String ("") or start with the '/' character. */
	protected String localPathPrefix;

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

		String repositoryName;
		int slashIndex = local.indexOf('/');
		if (slashIndex < 0) {
			repositoryName = local;
			localPathPrefix = "";
		}
		else {
			repositoryName = local.substring(0, slashIndex);
			localPathPrefix = local.substring(slashIndex);

			if (!localPathPrefix.startsWith("/"))
				throw new IllegalStateException("localPathPrefix does not start with '/': " + localPathPrefix);
		}

		if ("/".equals(localPathPrefix))
			localPathPrefix = "";

		localRoot = LocalRepoRegistry.getInstance().getLocalRootForRepositoryName(repositoryName);
		if (localRoot != null) {
			localFile = UrlUtil.getFile(localRoot, localPathPrefix);
		} else {
			localFile = new File(local).getAbsoluteFile();
			localRoot = LocalRepoHelper.getLocalRootContainingFile(localFile);
			if (localRoot == null)
				localRoot = localFile;

			if (localRoot.equals(localFile))
				localPathPrefix = "";
			else {
				localPathPrefix = IOUtil.getRelativePath(localRoot, localFile);
			}
		}
		assertLocalRootNotNull();
	}

	protected void assertLocalRootNotNull() {
		assertNotNull("localRoot", localRoot);
	}

}
