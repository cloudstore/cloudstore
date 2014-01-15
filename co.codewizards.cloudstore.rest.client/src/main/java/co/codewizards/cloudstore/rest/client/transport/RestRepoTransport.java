package co.codewizards.cloudstore.rest.client.transport;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import co.codewizards.cloudstore.core.auth.AuthConstants;
import co.codewizards.cloudstore.core.auth.AuthToken;
import co.codewizards.cloudstore.core.auth.AuthTokenIO;
import co.codewizards.cloudstore.core.auth.AuthTokenVerifier;
import co.codewizards.cloudstore.core.auth.EncryptedSignedAuthToken;
import co.codewizards.cloudstore.core.auth.SignedAuthToken;
import co.codewizards.cloudstore.core.auth.SignedAuthTokenDecrypter;
import co.codewizards.cloudstore.core.auth.SignedAuthTokenIO;
import co.codewizards.cloudstore.core.dto.ChangeSet;
import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.FileChunkSet;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.persistence.RemoteRepository;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;
import co.codewizards.cloudstore.rest.client.CloudStoreRESTClient;
import co.codewizards.cloudstore.rest.client.CredentialsProvider;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import co.codewizards.cloudstore.rest.client.ssl.HostnameVerifierAllowingAll;
import co.codewizards.cloudstore.rest.client.ssl.SSLContextUtil;

public class RestRepoTransport extends AbstractRepoTransport implements CredentialsProvider {

	private EntityID repositoryID; // server-repository
	private EntityID clientRepositoryID; // client-repository
	private byte[] publicKey;
	private String repositoryName;
	private CloudStoreRESTClient client;
	private Map<EntityID, AuthToken> clientRepositoryID2AuthToken = new HashMap<EntityID, AuthToken>(1); // should never be more ;-)

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
	public void requestRepoConnection(EntityID remoteRepositoryID, byte[] publicKey) {
		RepositoryDTO repositoryDTO = new RepositoryDTO();
		repositoryDTO.setEntityID(remoteRepositoryID);
		repositoryDTO.setPublicKey(publicKey);
		getClient().requestRepoConnection(getRepositoryName(), repositoryDTO);
	}

	@Override
	public void close() {
		client = null;
	}

	@Override
	public ChangeSet getChangeSet(EntityID toRepositoryID, boolean localSync) {
		prepareAuth(toRepositoryID);
		return getClient().getChangeSet(getRepositoryID().toString(), localSync, toRepositoryID);
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
		prepareAuth(fromRepositoryID);
		getClient().endSyncFromRepository(getRepositoryID().toString(), fromRepositoryID);
	}

	@Override
	public void endSyncToRepository(EntityID fromRepositoryID, long fromLocalRevision) {
		prepareAuth(fromRepositoryID);
		getClient().endSyncToRepository(getRepositoryID().toString(), fromRepositoryID, fromLocalRevision);
	}

	@Override
	public String getUserName() {
		if (clientRepositoryID == null)
			throw new IllegalStateException("prepareAuth(...) not called!");

		return AuthConstants.USER_NAME_REPOSITORY_ID_PREFIX + clientRepositoryID;
	}

	@Override
	public String getPassword() {
		AuthToken authToken = getAuthToken();
		return authToken.getPassword();
	}

	private void prepareAuth(EntityID clientRepositoryID) {
		this.clientRepositoryID = assertNotNull("clientRepositoryID", clientRepositoryID);
	}

	private AuthToken getAuthToken() {
		if (clientRepositoryID == null)
			throw new IllegalStateException("prepareAuth(...) not called!");

		AuthToken authToken = clientRepositoryID2AuthToken.get(clientRepositoryID);
		if (authToken != null && (isAfterRenewalDate(authToken) || isExpired(authToken)))
			authToken = null;

		if (authToken == null) {
			File localRoot = LocalRepoRegistry.getInstance().getLocalRoot(clientRepositoryID);
			LocalRepoManager localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
			try {
				LocalRepoTransaction transaction = localRepoManager.beginTransaction();
				try {
					RemoteRepository remoteRepository = transaction.getDAO(RemoteRepositoryDAO.class).getObjectByIdOrFail(getRepositoryID());

					EncryptedSignedAuthToken encryptedSignedAuthToken = getClient().getEncryptedSignedAuthToken(getRepositoryName(), localRepoManager.getRepositoryID());

					byte[] signedAuthTokenData = new SignedAuthTokenDecrypter(localRepoManager.getPrivateKey()).decrypt(encryptedSignedAuthToken);

					SignedAuthToken signedAuthToken = new SignedAuthTokenIO().deserialise(signedAuthTokenData);

					AuthTokenVerifier verifier = new AuthTokenVerifier(remoteRepository.getPublicKey());
					verifier.verify(signedAuthToken);

					authToken = new AuthTokenIO().deserialise(signedAuthToken.getAuthTokenData());
					clientRepositoryID2AuthToken.put(clientRepositoryID, authToken);

					transaction.commit();
				} finally {
					transaction.rollbackIfActive();
				}
			} finally {
				localRepoManager.close();
			}
		}
		return authToken;
	}

	private boolean isAfterRenewalDate(AuthToken authToken) {
		assertNotNull("authToken", authToken);
		final int reserveMillis = 60000; // in case client or server are not exactly on time
		return System.currentTimeMillis() + reserveMillis > authToken.getRenewalDateTime().getMillis();
	}

	private boolean isExpired(AuthToken authToken) {
		assertNotNull("authToken", authToken);
		final int reserveMillis = 60000; // in case client or server are not exactly on time
		return System.currentTimeMillis() + reserveMillis > authToken.getExpiryDateTime().getMillis();
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
			c.setCredentialsProvider(this);
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
