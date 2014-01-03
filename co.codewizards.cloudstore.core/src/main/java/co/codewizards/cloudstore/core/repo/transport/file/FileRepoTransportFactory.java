package co.codewizards.cloudstore.core.repo.transport.file;

import java.net.URL;

import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

public class FileRepoTransportFactory extends AbstractRepoTransportFactory {

	public static final String PROTOCOL_FILE = "file";

	@Override
	public String getName() {
		return "File";
	}

	@Override
	public String getDescription() {
		return "Repository in the local file system.";
	}

	@Override
	public boolean isSupported(URL remoteRoot) {
		return PROTOCOL_FILE.equals(remoteRoot.getProtocol());
	}

	@Override
	protected RepoTransport _createRepoTransport(URL remoteRoot) {
		return new FileRepoTransport();
	}

}
