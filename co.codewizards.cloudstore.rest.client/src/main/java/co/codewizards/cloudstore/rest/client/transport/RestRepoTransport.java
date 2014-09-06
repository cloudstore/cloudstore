package co.codewizards.cloudstore.rest.client.transport;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.dto.ResumeFileDto;
import co.codewizards.cloudstore.core.io.TimeoutException;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;
import co.codewizards.cloudstore.core.util.AssertUtil;
import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;
import co.codewizards.cloudstore.rest.client.CredentialsProvider;
import co.codewizards.cloudstore.rest.client.request.BeginPutFile;
import co.codewizards.cloudstore.rest.client.request.Copy;
import co.codewizards.cloudstore.rest.client.request.Delete;
import co.codewizards.cloudstore.rest.client.request.EndPutFile;
import co.codewizards.cloudstore.rest.client.request.EndSyncFromRepository;
import co.codewizards.cloudstore.rest.client.request.EndSyncToRepository;
import co.codewizards.cloudstore.rest.client.request.GetChangeSetDto;
import co.codewizards.cloudstore.rest.client.request.GetEncryptedSignedAuthToken;
import co.codewizards.cloudstore.rest.client.request.GetFileData;
import co.codewizards.cloudstore.rest.client.request.GetRepoFileDto;
import co.codewizards.cloudstore.rest.client.request.GetRepositoryDto;
import co.codewizards.cloudstore.rest.client.request.MakeDirectory;
import co.codewizards.cloudstore.rest.client.request.MakeSymlink;
import co.codewizards.cloudstore.rest.client.request.Move;
import co.codewizards.cloudstore.rest.client.request.PutFileData;
import co.codewizards.cloudstore.rest.client.request.RequestRepoConnection;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import co.codewizards.cloudstore.rest.client.ssl.HostnameVerifierAllowingAll;
import co.codewizards.cloudstore.rest.client.ssl.SSLContextBuilder;

public class RestRepoTransport extends AbstractRepoTransport implements CredentialsProvider {
	private static final Logger logger = LoggerFactory.getLogger(RestRepoTransport.class);

	private final long changeSetTimeout = 60L * 60L * 1000L; // TODO make configurable!
	private final long fileChunkSetTimeout = 60L * 60L * 1000L; // TODO make configurable!

	private UUID repositoryId; // server-repository
	private byte[] publicKey;
	private String repositoryName; // server-repository
	private CloudStoreRestClient client;
	private final Map<UUID, AuthToken> clientRepositoryId2AuthToken = new HashMap<UUID, AuthToken>(1); // should never be more ;-)

	protected DynamicX509TrustManagerCallback getDynamicX509TrustManagerCallback() {
		final RestRepoTransportFactory repoTransportFactory = (RestRepoTransportFactory) getRepoTransportFactory();
		final Class<? extends DynamicX509TrustManagerCallback> klass = repoTransportFactory.getDynamicX509TrustManagerCallbackClass();
		if (klass == null)
			throw new IllegalStateException("dynamicX509TrustManagerCallbackClass is not set!");

		try {
			final DynamicX509TrustManagerCallback instance = klass.newInstance();
			return instance;
		} catch (final Exception e) {
			throw new RuntimeException(String.format("Could not instantiate class %s: %s", klass.getName(), e.toString()), e);
		}
	}

	public RestRepoTransport() { }

	@Override
	public UUID getRepositoryId() {
		if (repositoryId == null) {
			final RepositoryDto repositoryDto = getRepositoryDto();
			repositoryId = repositoryDto.getRepositoryId();
			publicKey = repositoryDto.getPublicKey();
		}
		return repositoryId;
	}

	@Override
	public byte[] getPublicKey() {
		getRepositoryId(); // ensure, the public key is loaded
		return AssertUtil.assertNotNull("publicKey", publicKey);
	}

	@Override
	public RepositoryDto getRepositoryDto() {
		return getClient().execute(new GetRepositoryDto(getRepositoryName()));
	}

	@Override
	public void requestRepoConnection(final byte[] publicKey) {
		final RepositoryDto repositoryDto = new RepositoryDto();
		repositoryDto.setRepositoryId(getClientRepositoryIdOrFail());
		repositoryDto.setPublicKey(publicKey);
		getClient().execute(new RequestRepoConnection(getRepositoryName(), getPathPrefix(), repositoryDto));
	}

	@Override
	public void close() {
		client = null;
		super.close();
	}

	@Override
	public ResumeFileDto getResumeFileDto() {
		// resuming is always only possible on local file system.
		return null;
	}

	@Override
	public ChangeSetDto getChangeSetDto(final boolean localSync) {
		final long beginTimestamp = System.currentTimeMillis();
		while (true) {
			try {
				return getClient().execute(new GetChangeSetDto(getRepositoryId().toString(), localSync));
			} catch (final DeferredCompletionException x) {
				if (System.currentTimeMillis() > beginTimestamp + changeSetTimeout)
					throw new TimeoutException(String.format("Could not get change-set within %s milliseconds!", changeSetTimeout), x);

				logger.info("getChangeSet: Got DeferredCompletionException; will retry.");
			}
		}
	}

	@Override
	public void makeDirectory(String path, final Date lastModified) {
		path = prefixPath(path);
		getClient().execute(new MakeDirectory(getRepositoryId().toString(), path, lastModified));
	}

	@Override
	public void makeSymlink(String path, final String target, final Date lastModified) {
		path = prefixPath(path);
		getClient().execute(new MakeSymlink(getRepositoryId().toString(), path, target, lastModified));
	}

	@Override
	public void copy(String fromPath, String toPath) {
		fromPath = prefixPath(fromPath);
		toPath = prefixPath(toPath);
		getClient().execute(new Copy(getRepositoryId().toString(), fromPath, toPath));
	}

	@Override
	public void move(String fromPath, String toPath) {
		fromPath = prefixPath(fromPath);
		toPath = prefixPath(toPath);
		getClient().execute(new Move(getRepositoryId().toString(), fromPath, toPath));
	}

	@Override
	public void delete(String path) {
		path = prefixPath(path);
		getClient().execute(new Delete(getRepositoryId().toString(), path));
	}

	@Override
	public RepoFileDto getRepoFileDto(String path) {
		path = prefixPath(path);
		final long beginTimestamp = System.currentTimeMillis();
		while (true) {
			try {
				return getClient().execute(new GetRepoFileDto(getRepositoryId().toString(), path));
			} catch (final DeferredCompletionException x) {
				if (System.currentTimeMillis() > beginTimestamp + fileChunkSetTimeout)
					throw new TimeoutException(String.format("Could not get file-chunk-set within %s milliseconds!", fileChunkSetTimeout), x);

				logger.info("getFileChunkSet: Got DeferredCompletionException; will retry.");
			}
		}
	}

	@Override
	public byte[] getFileData(String path, final long offset, final int length) {
		path = prefixPath(path);
		return getClient().execute(new GetFileData(getRepositoryId().toString(), path, offset, length));
	}

	@Override
	public void beginPutFile(String path) {
		path = prefixPath(path);
		getClient().execute(new BeginPutFile(getRepositoryId().toString(), path));
	}

	@Override
	public void putFileData(String path, final long offset, final byte[] fileData) {
		path = prefixPath(path);
		getClient().execute(new PutFileData(getRepositoryId().toString(), path, offset, fileData));
	}

	@Override
	public void endPutFile(String path, final Date lastModified, final long length, final String sha1) {
		path = prefixPath(path);
		getClient().execute(new EndPutFile(getRepositoryId().toString(), path, new DateTime(lastModified), length, sha1));
	}

	@Override
	public void endSyncFromRepository() {
		getClient().execute(new EndSyncFromRepository(getRepositoryId().toString()));
	}

	@Override
	public void endSyncToRepository(final long fromLocalRevision) {
		getClient().execute(new EndSyncToRepository(getRepositoryId().toString(), fromLocalRevision));
	}

	@Override
	public String getUserName() {
		final UUID clientRepositoryId = getClientRepositoryIdOrFail();
		return AuthConstants.USER_NAME_REPOSITORY_ID_PREFIX + clientRepositoryId;
	}

	@Override
	public String getPassword() {
		final AuthToken authToken = getAuthToken();
		return authToken.getPassword();
	}

	private AuthToken getAuthToken() {
		final UUID clientRepositoryId = getClientRepositoryIdOrFail();
		AuthToken authToken = clientRepositoryId2AuthToken.get(clientRepositoryId);
		if (authToken != null && isAfterRenewalDate(authToken)) {
			logger.debug("getAuthToken: old AuthToken passed renewal-date: clientRepositoryId={} serverRepositoryId={} renewalDateTime={} expiryDateTime={}",
					clientRepositoryId, getRepositoryId(), authToken.getRenewalDateTime(), authToken.getExpiryDateTime());

			authToken = null;
		}

		if (authToken == null) {
			logger.debug("getAuthToken: getting new AuthToken: clientRepositoryId={} serverRepositoryId={}",
					clientRepositoryId, getRepositoryId());

			final File localRoot = LocalRepoRegistry.getInstance().getLocalRoot(clientRepositoryId);
			final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
			try {
				final EncryptedSignedAuthToken encryptedSignedAuthToken = getClient().execute(new GetEncryptedSignedAuthToken(getRepositoryName(), localRepoManager.getRepositoryId()));

				final byte[] signedAuthTokenData = new SignedAuthTokenDecrypter(localRepoManager.getPrivateKey()).decrypt(encryptedSignedAuthToken);

				final SignedAuthToken signedAuthToken = new SignedAuthTokenIO().deserialise(signedAuthTokenData);

				final AuthTokenVerifier verifier = new AuthTokenVerifier(localRepoManager.getRemoteRepositoryPublicKeyOrFail(getRepositoryId()));
				verifier.verify(signedAuthToken);

				authToken = new AuthTokenIO().deserialise(signedAuthToken.getAuthTokenData());
				final Date expiryDate = AssertUtil.assertNotNull("authToken.expiryDateTime", authToken.getExpiryDateTime()).toDate();
				final Date renewalDate = AssertUtil.assertNotNull("authToken.renewalDateTime", authToken.getRenewalDateTime()).toDate();
				if (!renewalDate.before(expiryDate))
					throw new IllegalArgumentException(
							String.format("Invalid AuthToken: renewalDateTime >= expiryDateTime :: renewalDateTime=%s expiryDateTime=%s",
									authToken.getRenewalDateTime(), authToken.getExpiryDateTime()));

				clientRepositoryId2AuthToken.put(clientRepositoryId, authToken);
			} finally {
				localRepoManager.close();
			}

			logger.info("getAuthToken: got new AuthToken: clientRepositoryId={} serverRepositoryId={} renewalDateTime={} expiryDateTime={}",
					clientRepositoryId, getRepositoryId(), authToken.getRenewalDateTime(), authToken.getExpiryDateTime());
		}
		else
			logger.trace("getAuthToken: old AuthToken still valid: clientRepositoryId={} serverRepositoryId={} renewalDateTime={} expiryDateTime={}",
					clientRepositoryId, getRepositoryId(), authToken.getRenewalDateTime(), authToken.getExpiryDateTime());

		return authToken;
	}

	private boolean isAfterRenewalDate(final AuthToken authToken) {
		AssertUtil.assertNotNull("authToken", authToken);
		return System.currentTimeMillis() > authToken.getRenewalDateTime().getMillis();
	}

	protected CloudStoreRestClient getClient() {
		if (client == null) {
			final CloudStoreRestClient c = new CloudStoreRestClient(getRemoteRoot());
			c.setHostnameVerifier(new HostnameVerifierAllowingAll());
			try {
				c.setSslContext(SSLContextBuilder.create()
						.remoteURL(getRemoteRoot())
						.callback(getDynamicX509TrustManagerCallback()).build());
			} catch (final GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
			c.setCredentialsProvider(this);
			client = c;
		}
		return client;
	}

	@Override
	protected URL determineRemoteRootWithoutPathPrefix() {
		final String repositoryName = getRepositoryName();
		final String baseURL = getClient().getBaseURL();
		if (!baseURL.endsWith("/"))
			throw new IllegalStateException(String.format("baseURL does not end with a '/'! baseURL='%s'", baseURL));

		try {
			return new URL(baseURL + repositoryName);
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	protected String getRepositoryName() {
		if (repositoryName == null) {
			final String pathAfterBaseURL = getPathAfterBaseURL();
			final int indexOfFirstSlash = pathAfterBaseURL.indexOf('/');
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
			final URL remoteRoot = getRemoteRoot();
			if (remoteRoot == null)
				throw new IllegalStateException("remoteRoot not yet assigned!");

			final String baseURL = getClient().getBaseURL();
			if (!baseURL.endsWith("/"))
				throw new IllegalStateException(String.format("baseURL does not end with a '/'! remoteRoot='%s' baseURL='%s'", remoteRoot, baseURL));

			final String remoteRootString = remoteRoot.toExternalForm();
			if (!remoteRootString.startsWith(baseURL))
				throw new IllegalStateException(String.format("remoteRoot does not start with baseURL! remoteRoot='%s' baseURL='%s'", remoteRoot, baseURL));

			this.pathAfterBaseURL = pathAfterBaseURL = remoteRootString.substring(baseURL.length());
		}
		return pathAfterBaseURL;
	}

}
