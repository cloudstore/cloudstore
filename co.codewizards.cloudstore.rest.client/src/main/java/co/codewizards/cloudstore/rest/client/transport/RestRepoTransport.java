package co.codewizards.cloudstore.rest.client.transport;

import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Date;

import co.codewizards.cloudstore.core.dto.ChangeSet;
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
	private String repositoryName;
	private CloudStoreRESTClient client;
	private static volatile Class<? extends DynamicX509TrustManagerCallback> dynamicX509TrustManagerCallbackClass;

	public static Class<? extends DynamicX509TrustManagerCallback> getDynamicX509TrustManagerCallbackClass() {
		return dynamicX509TrustManagerCallbackClass;
	}
	public static void setDynamicX509TrustManagerCallbackClass(Class<? extends DynamicX509TrustManagerCallback> dynamicX509TrustManagerCallbackClass) {
		RestRepoTransport.dynamicX509TrustManagerCallbackClass = dynamicX509TrustManagerCallbackClass;
	}

	protected static DynamicX509TrustManagerCallback getDynamicX509TrustManagerCallback() {
		Class<? extends DynamicX509TrustManagerCallback> klass = dynamicX509TrustManagerCallbackClass;
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
			repositoryID = getRepositoryDTO().getEntityID();
		}
		return repositoryID;
	}

	@Override
	public RepositoryDTO getRepositoryDTO() {
		return getClient().getRepositoryDTO(getRepositoryName());
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
		getClient().testSuccess();
	}

	@Override
	public void delete(String path) {
		getClient().deleteFile(getRepositoryID().toString(), path);
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
	public void beginPutFile(String path) {
		// TODO Auto-generated method stub

	}

	@Override
	public void putFileData(String path, long offset, byte[] fileData) {
		// TODO Auto-generated method stub

	}

	@Override
	public void endPutFile(String path, Date lastModified, long length) {
		// TODO Auto-generated method stub

	}

	@Override
	public void endSyncFromRepository(EntityID toRepositoryID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void endSyncToRepository(EntityID fromRepositoryID, long fromLocalRevision) {
		// TODO Auto-generated method stub
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
