package co.codewizards.cloudstore.client;

import java.io.File;
import java.net.URL;

import org.kohsuke.args4j.Option;

import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;

/**
 * {@link SubCommand} implementation for requesting a connection at a remote repository.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class RequestRepoConnectionSubCommand extends SubCommand
{
	// TODO support sub-dirs!
	@Option(name="-local", metaVar="<path>", required=false, usage="A path inside a repository in the local file system. This may be the local repository's root or any directory inside it. If it is not specified, it defaults to the current working directory. If this is a sub-directory (i.e. not the root), only this sub-directory is connected with the remote repository. NOTE: Sub-dirs are NOT YET SUPPORTED!")
	private String local;

	private File localFile;

	@Option(name="-remote", metaVar="<url>", required=true, usage="A URL to a remote repository. This may be the remote repository's root or any sub-directory. If a sub-directory is specified here, only this sub-directory is connected with the local repository. NOTE: Sub-dirs are NOT YET SUPPORTED!")
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

		if (local == null)
			localFile = new File("").getAbsoluteFile();
		else
			localFile = new File(local).getAbsoluteFile();

		local = localFile.getPath();

		remoteURL = new URL(remote);
	}

	@Override
	public void run() throws Exception {
		File localRoot = localFile;
		// TODO support sub-dir-connections to "check-out" only a branch of a repo! Bidirectionally (only upload a sub-branch to a certain server, too)!
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
			EntityID localRepositoryID = localRepoManager.getRepositoryID();
			RepoTransport repoTransport = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteURL).createRepoTransport(remoteURL);
			EntityID remoteRepositoryID = repoTransport.getRepositoryID();
			localRepoManager.putRemoteRepository(remoteRepositoryID, remoteURL, repoTransport.getPublicKey());
			repoTransport.requestRepoConnection(localRepositoryID, localRepoManager.getPublicKey());
		} finally {
			localRepoManager.close();
		}
	}
}
