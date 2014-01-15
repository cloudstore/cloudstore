package co.codewizards.cloudstore.client;

import java.net.URL;

import org.kohsuke.args4j.Argument;

import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.core.util.HashUtil;

/**
 * {@link SubCommand} implementation for requesting a connection at a remote repository.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class RequestRepoConnectionSubCommand extends SubCommandWithExistingLocalRepo
{
	@Argument(metaVar="<remote>", index=1, required=true, usage="A URL to a remote repository. This may be the remote repository's root or any sub-directory. If a sub-directory is specified here, only this sub-directory is connected with the local repository. NOTE: Sync of sub-dirs is NOT YET SUPPORTED!")
	private String remote;

	private URL remoteURL;

	@Override
	public String getSubCommandName() {
		return "requestRepoConnection";
	}

	@Override
	public String getSubCommandDescription() {
		return "Request a remote repository to allow a connection with a local repository.";
	}

	@Override
	public void prepare() throws Exception {
		super.prepare();

		remoteURL = new URL(remote);
	}

	@Override
	public void run() throws Exception {
		// TODO support sub-dir-connections to "check-out" only a branch of a repo! Bidirectionally (only upload a sub-branch to a certain server, too)!
		EntityID remoteRepositoryID;
		byte[] remotePublicKey;
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
			EntityID localRepositoryID = localRepoManager.getRepositoryID();
			RepoTransport repoTransport = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteURL).createRepoTransport(remoteURL);
			remoteRepositoryID = repoTransport.getRepositoryID();
			remotePublicKey = repoTransport.getPublicKey();
			localRepoManager.putRemoteRepository(remoteRepositoryID, remoteURL, remotePublicKey);
			repoTransport.requestRepoConnection(localRepositoryID, localRepoManager.getPublicKey());
		} finally {
			localRepoManager.close();
		}

		System.out.println("Successfully requested to connect the following remote repository:");
		System.out.println("  remoteRepository.repositoryID = " + remoteRepositoryID);
		System.out.println("  remoteRepository.remoteURL = " + remoteURL);
		System.out.println("  remoteRepository.publicKeySha1 = " + HashUtil.sha1ForHuman(localRepoManager.getPublicKey()));
		System.out.println("");
		System.out.println("Please verify the 'publicKeySha1' fingerprint! If it does not match the repository's fingerprint as shown locally on the server, you must cancel this request immediately using 'cloudstore cancelRepoConnection <remote>'!");
	}
}
