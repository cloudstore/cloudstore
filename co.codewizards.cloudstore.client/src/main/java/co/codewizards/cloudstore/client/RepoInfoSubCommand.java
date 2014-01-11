package co.codewizards.cloudstore.client;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;

import org.kohsuke.args4j.Argument;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;

/**
 * {@link SubCommand} implementation for showing information about a repository in the local file system.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class RepoInfoSubCommand extends SubCommand
{
	@Argument(metaVar="<localRoot>", required=false, usage="The path of the repository's root in the local file system. If it is not specified, the current working directory is used as local root.")
	private String localRoot;

	private File localRootFile;

	public RepoInfoSubCommand() { }

	protected RepoInfoSubCommand(File localRootFile) {
		this.localRootFile = assertNotNull("localRootFile", localRootFile);
		this.localRoot = localRootFile.getPath();
	}

	@Override
	public String getSubCommandName() {
		return "repoInfo";
	}

	@Override
	public String getSubCommandDescription() {
		return "Show information about an existing repository.";
	}

	@Override
	public void prepare() throws Exception {
		super.prepare();

		if (localRoot == null)
			localRootFile = new File("").getAbsoluteFile();
		else
			localRootFile = new File(localRoot).getAbsoluteFile();

		localRoot = localRootFile.getPath();
	}

	@Override
	public void run() throws Exception {
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(localRootFile);
		try {
			System.out.println("repository.localRoot = " + localRepoManager.getLocalRoot());
			System.out.println("repository.repositoryID = " + localRepoManager.getRepositoryID());
		} finally {
			localRepoManager.close();
		}
	}
}
