package co.codewizards.cloudstore.rest.client.transport;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import co.codewizards.cloudstore.core.dto.ChangeSet;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.FileChunkSet;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;
import co.codewizards.cloudstore.rest.client.CloudStoreRESTClient;

public class RestRepoTransport extends AbstractRepoTransport {

	private EntityID repositoryID;
	private URL baseURL;
	private CloudStoreRESTClient client;

	@Override
	public EntityID getRepositoryID() {
		// TODO support repository-aliases! Do not expect the repositoryID to be in the remoteRoot! Ask the server instead!
		return repositoryID;
	}

	@Override
	public void close() {
		client = null;
	}

	@Override
	public ChangeSet getChangeSet(EntityID toRepositoryID) {
		return getClient().getChangeSet(getRepositoryID(), toRepositoryID);
	}

	@Override
	public void makeDirectory(String path, Date lastModified) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(String path) {
		// TODO Auto-generated method stub

	}

	@Override
	public FileChunkSet getFileChunkSet(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] getFileData(String path, long offset, int length) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void beginFile(String path) {
		// TODO Auto-generated method stub

	}

	@Override
	public void putFileData(String path, long offset, byte[] fileData) {
		// TODO Auto-generated method stub

	}

	@Override
	public void endFile(String path, Date lastModified, long length) {
		// TODO Auto-generated method stub

	}

	@Override
	public void endSync(EntityID toRepositoryID) {
		// TODO Auto-generated method stub

	}

	public CloudStoreRESTClient getClient() {
		if (client == null)
			client = new CloudStoreRESTClient(getBaseURL());

		return client;
	}

	@Override
	public void setRemoteRoot(URL remoteRoot) {
		super.setRemoteRoot(remoteRoot);
		baseURL = null;
		repositoryID = null;
		if (remoteRoot != null) {
			String remoteRootString = remoteRoot.toString();
			if (remoteRootString.indexOf('?') >= 0)
				throw new IllegalStateException("remoteRoot must not contain query part (no '?')!");

			if (remoteRootString.endsWith("/"))
				remoteRootString = remoteRootString.substring(0, remoteRootString.length() - 1);

			int lastSlashIndex = remoteRootString.lastIndexOf('/');
			if (lastSlashIndex < 0)
				throw new IllegalStateException("remoteRoot does not contain a '/' before the repositoryName!");

			// TODO support repository-aliases! Do not expect the repositoryID to be in the remoteRoot! Ask the server instead!
			String repositoryIDString = remoteRootString.substring(lastSlashIndex + 1);
			repositoryID = new EntityID(repositoryIDString);

			String baseURLString = remoteRootString.substring(0, lastSlashIndex);
			try {
				baseURL = new URL(baseURLString);
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private URL getBaseURL() {
		return baseURL;
	}
}
