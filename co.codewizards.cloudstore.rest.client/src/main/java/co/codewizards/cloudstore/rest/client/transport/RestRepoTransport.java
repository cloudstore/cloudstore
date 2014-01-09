package co.codewizards.cloudstore.rest.client.transport;

import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Date;

import co.codewizards.cloudstore.core.dto.ChangeSet;
import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.FileChunkSet;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;
import co.codewizards.cloudstore.rest.client.CloudStoreRESTClient;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import co.codewizards.cloudstore.rest.client.ssl.HostnameVerifierAllowingAll;
import co.codewizards.cloudstore.rest.client.ssl.SSLContextUtil;

public class RestRepoTransport extends AbstractRepoTransport {

	private EntityID repositoryID;
	private byte[] publicKey;
	private String repositoryName;
	private CloudStoreRESTClient client;

	protected DynamicX509TrustManagerCallback getDynamicX509TrustManagerCallback() {
		RestRepoTransportFactory repoTransportFactory = (RestRepoTransportFactory) getRepoTransportFactory();
		Class<? extends DynamicX509TrustManagerCallback> klass = repoTransportFactory.getDynamicX509TrustManagerCallbackClass();
		if (klass == null)
			throw new IllegalStateException("dynamicX509TrustManagerCallbackClass is not set!");

		try {
			DynamicX509TrustManagerCallback instance = klass.newInstance();
			return instance;
		} catch (Exception e) {
			throw new RuntimeException(String.format("Could not instantiate class %s: %s", klass.getName(), e.toString()), e);
		}
	}

	public RestRepoTransport() { }

	@Override
	public EntityID getRepositoryID() {
		if (repositoryID == null) {
			RepositoryDTO repositoryDTO = getRepositoryDTO();
			repositoryID = repositoryDTO.getEntityID();
			publicKey = repositoryDTO.getPublicKey();
		}
		return repositoryID;
	}

	@Override
	public byte[] getPublicKey() {
		getRepositoryID(); // ensure, the public key is loaded
		return publicKey;
	}

	@Override
	public RepositoryDTO getRepositoryDTO() {
		return getClient().getRepositoryDTO(getRepositoryName());
	}

	@Override
	public void requestConnection(EntityID remoteRepositoryID, byte[] publicKey) {
		RepositoryDTO repositoryDTO = new RepositoryDTO();
		repositoryDTO.setEntityID(remoteRepositoryID);
		repositoryDTO.setPublicKey(publicKey);
		getClient().requestConnection(repositoryDTO);
	}

	@Override
	public void close() {
		client = null;
	}

	@Override
	public ChangeSet getChangeSet(EntityID toRepositoryID) {
		return getClient().getChangeSet(getRepositoryID().toString(), toRepositoryID);
	}

	@Override
	public void makeDirectory(String path, Date lastModified) {
		getClient().makeDirectory(getRepositoryID().toString(), path, lastModified);
	}

	@Override
	public void delete(String path) {
		getClient().delete(getRepositoryID().toString(), path);
	}

	@Override
	public FileChunkSet getFileChunkSet(String path) {
		return getClient().getFileChunkSet(getRepositoryID().toString(), path);
	}

	@Override
	public byte[] getFileData(String path, long offset, int length) {
		return getClient().getFileData(getRepositoryID().toString(), path, offset, length);
	}

	@Override
	public void beginPutFile(String path) {
		getClient().beginPutFile(getRepositoryID().toString(), path);
	}

	@Override
	public void putFileData(String path, long offset, byte[] fileData) {
		getClient().putFileData(getRepositoryID().toString(), path, offset, fileData);
	}

	@Override
	public void endPutFile(String path, Date lastModified, long length) {
		getClient().endPutFile(getRepositoryID().toString(), path,new DateTime(lastModified), length);
	}

	@Override
	public void endSyncFromRepository(EntityID fromRepositoryID) {
		getClient().endSyncFromRepository(getRepositoryID().toString(), fromRepositoryID);
	}

	@Override
	public void endSyncToRepository(EntityID fromRepositoryID, long fromLocalRevision) {
		getClient().endSyncToRepository(getRepositoryID().toString(), fromRepositoryID, fromLocalRevision);
	}

	protected CloudStoreRESTClient getClient() {
		if (client == null) {
			CloudStoreRESTClient c = new CloudStoreRESTClient(getRemoteRoot());
			c.setHostnameVerifier(new HostnameVerifierAllowingAll());
			try {
				c.setSslContext(SSLContextUtil.getSSLContext(getRemoteRoot(), getDynamicX509TrustManagerCallback()));
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
			client = c;
		}
		return client;
	}

	protected String getRepositoryName() {
		if (repositoryName == null) {
			URL remoteRoot = getRemoteRoot();
			if (remoteRoot == null)
				throw new IllegalStateException("remoteRoot not yet assigned!");

			String baseURL = getClient().getBaseURL();
			if (!baseURL.endsWith("/"))
				throw new IllegalStateException(String.format("baseURL does not end with a '/'! remoteRoot='%s' baseURL='%s'", remoteRoot, baseURL));

			String remoteRootString = remoteRoot.toExternalForm();
			if (!remoteRootString.startsWith(baseURL))
				throw new IllegalStateException(String.format("remoteRoot does not start with baseURL! remoteRoot='%s' baseURL='%s'", remoteRoot, baseURL));

			String pathAfterBaseURL = remoteRootString.substring(baseURL.length());
			int indexOfFirstSlash = pathAfterBaseURL.indexOf('/');
			if (indexOfFirstSlash < 0) {
				repositoryName = pathAfterBaseURL;
			}
			else {
				repositoryName = pathAfterBaseURL.substring(indexOfFirstSlash);
			}
			if (repositoryName.isEmpty())
				throw new IllegalStateException("repositoryName is empty!");
		}
		return repositoryName;
	}
}
