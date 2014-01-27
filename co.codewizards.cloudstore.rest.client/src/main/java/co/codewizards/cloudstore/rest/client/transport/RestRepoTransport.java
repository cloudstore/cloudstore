package co.codewizards.cloudstore.rest.client.transport;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.auth.AuthConstants;
import co.codewizards.cloudstore.core.auth.AuthToken;
import co.codewizards.cloudstore.core.auth.AuthTokenIO;
import co.codewizards.cloudstore.core.auth.AuthTokenVerifier;
import co.codewizards.cloudstore.core.auth.EncryptedSignedAuthToken;
import co.codewizards.cloudstore.core.auth.SignedAuthToken;
import co.codewizards.cloudstore.core.auth.SignedAuthTokenDecrypter;
import co.codewizards.cloudstore.core.auth.SignedAuthTokenIO;
import co.codewizards.cloudstore.core.concurrent.DeferredCompletionException;
import co.codewizards.cloudstore.core.dto.ChangeSetDTO;
import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.FileChunkSetDTO;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.io.TimeoutException;
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
	private static final Logger logger = LoggerFactory.getLogger(RestRepoTransport.class);

	private long changeSetTimeout = 60L * 60L * 1000L; // TODO make configurable!
	private long fileChunkSetTimeout = 60L * 60L * 1000L; // TODO make configurable!

	private EntityID repositoryID; // server-repository
	private byte[] publicKey;
	private String repositoryName; // server-repository
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
	public void requestRepoConnection(byte[] publicKey) {
		RepositoryDTO repositoryDTO = new RepositoryDTO();
		repositoryDTO.setEntityID(getClientRepositoryIDOrFail());
		repositoryDTO.setPublicKey(publicKey);
		getClient().requestRepoConnection(getRepositoryName(), getPathPrefix(), repositoryDTO);
	}

	@Override
	public void close() {
		client = null;
	}

	@Override
	public ChangeSetDTO getChangeSet(boolean localSync) {
		long beginTimestamp = System.currentTimeMillis();
		while (true) {
			try {
				return getClient().getChangeSet(getRepositoryID().toString(), localSync);
			} catch (DeferredCompletionException x) {
				if (System.currentTimeMillis() > beginTimestamp + changeSetTimeout)
					throw new TimeoutException(String.format("Could not get change-set within %s milliseconds!", changeSetTimeout), x);

				logger.info("getChangeSet: Got DeferredCompletionException; will retry.");
			}
		}
	}

	@Override
	public void makeDirectory(String path, Date lastModified) {
		path = prefixPath(path);
		getClient().makeDirectory(getRepositoryID().toString(), path, lastModified);
	}

	@Override
	public void delete(String path) {
		path = prefixPath(path);
		getClient().delete(getRepositoryID().toString(), path);
	}

	@Override
	public FileChunkSetDTO getFileChunkSet(String path) {
		path = prefixPath(path);
		long beginTimestamp = System.currentTimeMillis();
		while (true) {
			try {
				return getClient().getFileChunkSet(getRepositoryID().toString(), path);
			} catch (DeferredCompletionException x) {
				if (System.currentTimeMillis() > beginTimestamp + fileChunkSetTimeout)
					throw new TimeoutException(String.format("Could not get file-chunk-set within %s milliseconds!", fileChunkSetTimeout), x);

				logger.info("getFileChunkSet: Got DeferredCompletionException; will retry.");
			}
		}
	}

	@Override
	public byte[] getFileData(String path, long offset, int length) {
		path = prefixPath(path);
		return getClient().getFileData(getRepositoryID().toString(), path, offset, length);
	}

	@Override
	public void beginPutFile(String path) {
		path = prefixPath(path);
		getClient().beginPutFile(getRepositoryID().toString(), path);
	}

	@Override
	public void putFileData(String path, long offset, byte[] fileData) {
		path = prefixPath(path);
		getClient().putFileData(getRepositoryID().toString(), path, offset, fileData);
	}

	@Override
	public void endPutFile(String path, Date lastModified, long length) {
		path = prefixPath(path);
		getClient().endPutFile(getRepositoryID().toString(), path, new DateTime(lastModified),length);
	}

	@Override
	public void endSyncFromRepository() {
		getClient().endSyncFromRepository(getRepositoryID().toString());
	}

	@Override
	public void endSyncToRepository(long fromLocalRevision) {
		getClient().endSyncToRepository(getRepositoryID().toString(), fromLocalRevision);
	}

	@Override
	public String getUserName() {
		EntityID clientRepositoryID = getClientRepositoryIDOrFail();
		return AuthConstants.USER_NAME_REPOSITORY_ID_PREFIX + clientRepositoryID;
	}

	@Override
	public String getPassword() {
		AuthToken authToken = getAuthToken();
		return authToken.getPassword();
	}

	private AuthToken getAuthToken() {
		EntityID clientRepositoryID = getClientRepositoryIDOrFail();
		AuthToken authToken = clientRepositoryID2AuthToken.get(clientRepositoryID);
		if (authToken != null && isAfterRenewalDate(authToken)) {
			logger.debug("getAuthToken: old AuthToken passed renewal-date: clientRepositoryID={} serverRepositoryID={} renewalDateTime={} expiryDateTime={}",
					clientRepositoryID, getRepositoryID(), authToken.getRenewalDateTime(), authToken.getExpiryDateTime());

			authToken = null;
		}

		if (authToken == null) {
			logger.debug("getAuthToken: getting new AuthToken: clientRepositoryID={} serverRepositoryID={}",
					clientRepositoryID, getRepositoryID());

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
					Date expiryDate = assertNotNull("authToken.expiryDateTime", authToken.getExpiryDateTime()).toDate();
					Date renewalDate = assertNotNull("authToken.renewalDateTime", authToken.getRenewalDateTime()).toDate();
					if (!renewalDate.before(expiryDate))
						throw new IllegalArgumentException(
								String.format("Invalid AuthToken: renewalDateTime >= expiryDateTime :: renewalDateTime=%s expiryDateTime=%s",
										authToken.getRenewalDateTime(), authToken.getExpiryDateTime()));

					clientRepositoryID2AuthToken.put(clientRepositoryID, authToken);

					transaction.commit();
				} finally {
					transaction.rollbackIfActive();
				}
			} finally {
				localRepoManager.close();
			}

			logger.info("getAuthToken: got new AuthToken: clientRepositoryID={} serverRepositoryID={} renewalDateTime={} expiryDateTime={}",
					clientRepositoryID, getRepositoryID(), authToken.getRenewalDateTime(), authToken.getExpiryDateTime());
		}
		else
			logger.trace("getAuthToken: old AuthToken still valid: clientRepositoryID={} serverRepositoryID={} renewalDateTime={} expiryDateTime={}",
					clientRepositoryID, getRepositoryID(), authToken.getRenewalDateTime(), authToken.getExpiryDateTime());

		return authToken;
	}

	private boolean isAfterRenewalDate(AuthToken authToken) {
		assertNotNull("authToken", authToken);
		return System.currentTimeMillis() > authToken.getRenewalDateTime().getMillis();
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

	@Override
	protected URL determineRemoteRootWithoutPathPrefix() {
		String repositoryName = getRepositoryName();
		String baseURL = getClient().getBaseURL();
		if (!baseURL.endsWith("/"))
			throw new IllegalStateException(String.format("baseURL does not end with a '/'! baseURL='%s'", baseURL));

		try {
			return new URL(baseURL + repositoryName);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	protected String getRepositoryName() {
		if (repositoryName == null) {
			String pathAfterBaseURL = getPathAfterBaseURL();
			int indexOfFirstSlash = pathAfterBaseURL.indexOf('/');
			if (indexOfFirstSlash < 0) {
				repositoryName = pathAfterBaseURL;
			}
			else {
				repositoryName = pathAfterBaseURL.substring(0, indexOfFirstSlash);
			}
			if (repositoryName.isEmpty())
				throw new IllegalStateException("repositoryName is empty!");
		}
		return repositoryName;
	}

	private String pathAfterBaseURL;

	protected String getPathAfterBaseURL() {
		String pathAfterBaseURL = this.pathAfterBaseURL;
		if (pathAfterBaseURL == null) {
			URL remoteRoot = getRemoteRoot();
			if (remoteRoot == null)
				throw new IllegalStateException("remoteRoot not yet assigned!");

			String baseURL = getClient().getBaseURL();
			if (!baseURL.endsWith("/"))
				throw new IllegalStateException(String.format("baseURL does not end with a '/'! remoteRoot='%s' baseURL='%s'", remoteRoot, baseURL));

			String remoteRootString = remoteRoot.toExternalForm();
			if (!remoteRootString.startsWith(baseURL))
				throw new IllegalStateException(String.format("remoteRoot does not start with baseURL! remoteRoot='%s' baseURL='%s'", remoteRoot, baseURL));

			this.pathAfterBaseURL = pathAfterBaseURL = remoteRootString.substring(baseURL.length());
		}
		return pathAfterBaseURL;
	}

}
