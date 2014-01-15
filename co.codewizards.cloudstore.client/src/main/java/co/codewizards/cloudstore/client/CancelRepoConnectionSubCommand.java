package co.codewizards.cloudstore.client;

import java.net.MalformedURLException;
import java.net.URL;

import org.kohsuke.args4j.Argument;

import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.persistence.RemoteRepository;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryRequest;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryRequestDAO;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

/**
 * {@link SubCommand} implementation for cancelling a connection with a remote repository.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class CancelRepoConnectionSubCommand extends SubCommandWithExistingLocalRepo
{
	@Argument(metaVar="<remote>", index=1, required=true, usage="An ID or URL of a remote repository.")
	private String remote;

//	@Option(name="-localOnly", required=false, usage="Do not attempt to unregister the repo-connection on the server-side.")
//	private boolean localOnly;

	private EntityID remoteRepositoryID;
	private URL remoteRoot;

	@Override
	public String getSubCommandName() {
		return "cancelRepoConnection";
	}

	@Override
	public String getSubCommandDescription() {
		return "Cancel a connection to a remote repository. IMPORTANT: This does currently only operate locally. Thus, you have to cancel a connection manually on both sides.";
	}

	@Override
	public void prepare() throws Exception {
		super.prepare();

		try {
			remoteRepositoryID = new EntityID(remote);
			remoteRoot = null;
		} catch (IllegalArgumentException x) {
			try {
				remoteRoot = new URL(remote);
				remoteRepositoryID = null;
			} catch (MalformedURLException y) {
				throw new IllegalArgumentException(String.format("<remote> '%s' is neither a valid repositoryID nor a valid URL!", remote));
			}
		}
	}

	@Override
	public void run() throws Exception {
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
			LocalRepoTransaction transaction = localRepoManager.beginTransaction();
			try {
				RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
				if (remoteRepositoryID != null) {
					RemoteRepository remoteRepository = remoteRepositoryDAO.getObjectByIdOrNull(remoteRepositoryID);
					if (remoteRepository != null) {
						remoteRoot = remoteRepository.getRemoteRoot();
						remoteRepositoryDAO.deletePersistent(remoteRepository);
						remoteRepositoryDAO.getPersistenceManager().flush();
					}

					RemoteRepositoryRequestDAO remoteRepositoryRequestDAO = transaction.getDAO(RemoteRepositoryRequestDAO.class);
					RemoteRepositoryRequest remoteRepositoryRequest = remoteRepositoryRequestDAO.getRemoteRepositoryRequest(remoteRepositoryID);
					if (remoteRepositoryRequest != null) {
						remoteRepositoryRequestDAO.deletePersistent(remoteRepositoryRequest);
						remoteRepositoryRequestDAO.getPersistenceManager().flush();
					}
				}

				if (remoteRoot != null) {
					RemoteRepository remoteRepository = remoteRepositoryDAO.getRemoteRepository(remoteRoot);
					if (remoteRepository != null) {
						remoteRepositoryDAO.deletePersistent(remoteRepository);
						remoteRepositoryDAO.getPersistenceManager().flush();
					}
					// TODO cancel on the remote side, too.
				}

				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}
		} finally {
			localRepoManager.close();
		}
	}
}
