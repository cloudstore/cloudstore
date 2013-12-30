package co.codewizards.cloudstore.shared.repo.sync;

import static co.codewizards.cloudstore.shared.util.Util.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import co.codewizards.cloudstore.shared.dto.ChangeSetRequest;
import co.codewizards.cloudstore.shared.progress.ProgressMonitor;
import co.codewizards.cloudstore.shared.repo.transport.RepoTransport;
import co.codewizards.cloudstore.shared.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.shared.repo.transport.RepoTransportFactoryRegistry;

/**
 * Logic for synchronising a local with a remote repository.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class RepoSync {

	private final File localRoot;
	private final URL remoteRoot;
	private final RepoTransport localRepoTransport;
	private final RepoTransport remoteRepoTransport;

	public RepoSync(File localRoot, URL remoteRoot) {
		this.localRoot = assertNotNull("localRoot", localRoot);
		this.remoteRoot = assertNotNull("remoteRoot", remoteRoot);
		localRepoTransport = createRepoTransport(localRoot);
		remoteRepoTransport = createRepoTransport(remoteRoot);
	}

	public void sync(ProgressMonitor monitor) {
		assertNotNull("monitor", monitor);
		sync(remoteRepoTransport, localRepoTransport, monitor);
		sync(localRepoTransport, remoteRepoTransport, monitor);
	}

	private RepoTransport createRepoTransport(File rootFile) {
		URL rootURL;
		try {
			rootURL = rootFile.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return createRepoTransport(rootURL);
	}

	private RepoTransport createRepoTransport(URL remoteRoot) {
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(remoteRoot);
		return repoTransportFactory.createRepoTransport(remoteRoot);
	}

	private void sync(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, ProgressMonitor monitor) {
		ChangeSetRequest changeSetRequest = new ChangeSetRequest();

		fromRepoTransport.getChangeSet(changeSetRequest);
	}

}
