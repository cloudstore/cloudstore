package co.codewizards.cloudstore.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.kohsuke.args4j.Argument;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryRequest;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryRequestDao;

/**
 * {@link SubCommand} implementation for cancelling a connection with a remote repository.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class DropRepoConnectionSubCommand extends SubCommandWithExistingLocalRepo
{
	@Argument(metaVar="<remote>", index=1, required=true, usage="An ID or URL of a remote repository.")
	private String remote;

//	@Option(name="-localOnly", required=false, usage="Do not attempt to unregister the repo-connection on the server-side.")
//	private boolean localOnly;

	private UUID remoteRepositoryId;
	private URL remoteRoot;

	@Override
	public String getSubCommandDescription() {
		return "Cancel a connection to a remote repository. IMPORTANT: This does currently only operate locally. Thus, you have to cancel a connection manually on both sides.";
	}

	@Override
	public void prepare() throws Exception {
		super.prepare();

		try {
			remoteRepositoryId = UUID.fromString(remote);
			remoteRoot = null;
		} catch (IllegalArgumentException x) {
			try {
				remoteRoot = new URL(remote);
				remoteRepositoryId = null;
			} catch (MalformedURLException y) {
				throw new IllegalArgumentException(String.format("<remote> '%s' is neither a valid repositoryId nor a valid URL!", remote));
			}
		}
	}

	@Override
	public void run() throws Exception {
		boolean foundSomethingToCancel = false;
		UUID localRepositoryId;
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
			localRepositoryId = localRepoManager.getRepositoryId();
			LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();
			try {
				RemoteRepositoryDao remoteRepositoryDao = transaction.getDao(RemoteRepositoryDao.class);
				if (remoteRepositoryId != null) {
					RemoteRepository remoteRepository = remoteRepositoryDao.getRemoteRepository(remoteRepositoryId);
					if (remoteRepository != null) {
						foundSomethingToCancel = true;
						remoteRoot = remoteRepository.getRemoteRoot();
						remoteRepositoryDao.deletePersistent(remoteRepository);
						remoteRepositoryDao.getPersistenceManager().flush();
					}

					RemoteRepositoryRequestDao remoteRepositoryRequestDao = transaction.getDao(RemoteRepositoryRequestDao.class);
					RemoteRepositoryRequest remoteRepositoryRequest = remoteRepositoryRequestDao.getRemoteRepositoryRequest(remoteRepositoryId);
					if (remoteRepositoryRequest != null) {
						foundSomethingToCancel = true;
						remoteRepositoryRequestDao.deletePersistent(remoteRepositoryRequest);
						remoteRepositoryRequestDao.getPersistenceManager().flush();
					}
				}

				if (remoteRoot != null) {
					RemoteRepository remoteRepository = remoteRepositoryDao.getRemoteRepository(remoteRoot);
					if (remoteRepository != null) {
						foundSomethingToCancel = true;
						remoteRepositoryId = remoteRepository.getRepositoryId();
						remoteRepositoryDao.deletePersistent(remoteRepository);
						remoteRepositoryDao.getPersistenceManager().flush();
					}
					// TODO automatically cancel on the remote side, too.
				}

				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}
		} finally {
			localRepoManager.close();
		}

		if (foundSomethingToCancel) {
			System.out.println("Successfully cancelled the connection from the local repository to the remote repository:");
			System.out.println();
			System.out.println("  localRepository.repositoryId = " + localRepositoryId);
			System.out.println("  localRepository.localRoot = " + localRoot);
			System.out.println();
			System.out.println("  remoteRepository.repositoryId = " + remoteRepositoryId);
			System.out.println("  remoteRepository.remoteRoot = " + remoteRoot);
			System.out.println();
			System.out.println("Important: This only cancelled the local side of the connection and you should cancel it on the other side, too, using this command (if you didn't do this yet):");
			System.out.println();
			System.out.println(String.format("  cloudstore dropRepoConnection %s %s", remoteRepositoryId, localRepositoryId));
		}
		else {
			System.out.println("There was nothing to be cancelled here. Maybe it was cancelled already before?!");
			System.out.println("Or maybe you want to instead run the following command on the other side (i.e. on the other computer - cancelling currently works only on one side):");
			System.out.println();
			System.out.println(String.format("  cloudstore dropRepoConnection %s %s", remoteRepositoryId, localRepositoryId));
		}
	}
}
