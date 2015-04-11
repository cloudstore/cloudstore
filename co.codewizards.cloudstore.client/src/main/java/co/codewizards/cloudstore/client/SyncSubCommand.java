package co.codewizards.cloudstore.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;

public class SyncSubCommand extends SubCommandWithExistingLocalRepo {
	private static final Logger logger = LoggerFactory.getLogger(SyncSubCommand.class);

	@Argument(metaVar="<remote>", index=1, required=false, usage="An ID or URL of a remote repository. If none is specified, all remote repositories are synced.")
	private String remote;

	private UUID remoteRepositoryId;
	private URL remoteRoot;

	@Option(name="-localOnly", required=false, usage="Synchronise locally only. Do not communicate with any remote repository.")
	private boolean localOnly;

	@Override
	public String getSubCommandDescription() {
		return "Synchronise a local repository. Depending on the parameters, it synchronises only locally or with one or more remote repositories.";
	}

	@Override
	public void prepare() throws Exception {
		super.prepare();
		remoteRepositoryId = null;
		remoteRoot = null;
		if (remote != null && !remote.isEmpty()) {
			try {
				remoteRepositoryId = UUID.fromString(remote);
			} catch (final IllegalArgumentException x) {
				try {
					remoteRoot = new URL(remote);
				} catch (final MalformedURLException y) {
					throw new IllegalArgumentException(String.format("<remote> '%s' is neither a valid repositoryId nor a valid URL!", remote));
				}
			}
		}
	}

	@Override
	protected void assertLocalRootNotNull() {
		if (!isAll())
			super.assertLocalRootNotNull();
	}

	@Override
	public void run() throws Exception {
		if (isAll()) {
			for (final UUID repositoryId : LocalRepoRegistry.getInstance().getRepositoryIds())
				sync(repositoryId);
		}
		else
			sync(localRoot);
	}

	private void sync(final UUID repositoryId) {
		final File localRoot = LocalRepoRegistry.getInstance().getLocalRootOrFail(repositoryId);
		sync(localRoot);
	}

	private void sync(File localRoot) {
		final List<URL> remoteRoots = new ArrayList<URL>();
		final Map<UUID, URL> filteredRemoteRepositoryId2RemoteRoot = new HashMap<UUID, URL>();
		UUID repositoryId;
		final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
			if (localOnly) {
				localRepoManager.localSync(new LoggerProgressMonitor(logger));
				return;
			}

			repositoryId = localRepoManager.getRepositoryId();
			localRoot = localRepoManager.getLocalRoot();
			for (final Map.Entry<UUID, URL> me : localRepoManager.getRemoteRepositoryId2RemoteRootMap().entrySet()) {
				final UUID id = me.getKey();
				final URL url = me.getValue();
				
				remoteRoots.add(url);
				if ((remoteRepositoryId == null && remoteRoot == null)
						|| (remoteRepositoryId != null && remoteRepositoryId.equals(id))
						|| (remoteRoot != null && remoteRoot.equals(url)))
					filteredRemoteRepositoryId2RemoteRoot.put(id, url);
			}
		} finally {
			localRepoManager.close();
		}

		if (remoteRoots.isEmpty())
			System.err.println(String.format("WARNING: The repository %s ('%s') is not connected to any remote repository as client!", repositoryId, localRoot));
		else if (filteredRemoteRepositoryId2RemoteRoot.isEmpty())
			System.err.println(String.format("WARNING: The repository %s ('%s') is not connected to the specified remote repository ('%s')!", repositoryId, localRoot, remote));
		else {
			for (final Map.Entry<UUID, URL> me : filteredRemoteRepositoryId2RemoteRoot.entrySet()) {
				final UUID remoteRepositoryId = me.getKey();
				final URL remoteRoot = me.getValue();
				System.out.println("********************************************************************************");
				System.out.println(String.format("Syncing %s ('%s') with %s ('%s').", repositoryId, localRoot, remoteRepositoryId, remoteRoot));
				System.out.println("********************************************************************************");
				final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(localRoot, remoteRoot);
				try {
					repoToRepoSync.sync(new LoggerProgressMonitor(logger));
				} finally {
					repoToRepoSync.close();
				}
			}
		}
	}

	private boolean isAll() {
		return "ALL".equals(local);
	}
}
