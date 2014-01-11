package co.codewizards.cloudstore.client;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.persistence.RemoteRepository;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;

/**
 * {@link SubCommand} implementation for syncing a repository locally.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class RemoteSyncSubCommand extends SubCommand
{
	private static final Logger logger = LoggerFactory.getLogger(RemoteSyncSubCommand.class);

	// TODO support sub-dirs!
	@Argument(metaVar="<local>", required=false, usage="A path inside a repository in the local file system. This may be the local repository's root or any directory inside it. If it is not specified, it defaults to the current working directory. NOTE: Sub-dirs are NOT YET SUPPORTED!")
	private String local;

	private File localFile;

	@Override
	public String getSubCommandName() {
		return "remoteSync";
	}

	@Override
	public String getSubCommandDescription() {
		return "Synchronise a local repository with one or all remote repositories that are connected to it.";
	}

	@Override
	public void prepare() throws Exception {
		super.prepare();

		if (local == null)
			localFile = new File("").getAbsoluteFile();
		else
			localFile = new File(local).getAbsoluteFile();

		local = localFile.getPath();
	}

	@Override
	public void run() throws Exception {
		List<URL> remoteRoots = new ArrayList<URL>();

		File localRoot;
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(localFile);
		try {
			localRoot = localRepoManager.getLocalRoot();
			LocalRepoTransaction transaction = localRepoManager.beginTransaction();
			try {
				Collection<RemoteRepository> remoteRepositories = transaction.getDAO(RemoteRepositoryDAO.class).getObjects();
				for (RemoteRepository remoteRepository : remoteRepositories)
					remoteRoots.add(remoteRepository.getRemoteRoot());

				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}
		} finally {
			localRepoManager.close();
		}

		if (remoteRoots.isEmpty())
			System.err.println(String.format("The repository '%s' is not connected to any remote repository!", localRoot));
		else {
			for (URL remoteRoot : remoteRoots) {
				RepoToRepoSync repoToRepoSync = new RepoToRepoSync(localFile, remoteRoot);
				repoToRepoSync.sync(new LoggerProgressMonitor(logger));
			}
		}
	}
}
