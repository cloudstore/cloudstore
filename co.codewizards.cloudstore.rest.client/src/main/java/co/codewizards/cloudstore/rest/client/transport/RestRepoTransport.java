package co.codewizards.cloudstore.rest.client.transport;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.client.ClientBuilder;

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
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.ConfigPropSetDto;
import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.dto.VersionInfoDto;
import co.codewizards.cloudstore.core.dto.jaxb.ChangeSetDtoIo;
import co.codewizards.cloudstore.core.io.TimeoutException;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.FileFilter;
import co.codewizards.cloudstore.core.repo.local.ContextWithLocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistryImpl;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;
import co.codewizards.cloudstore.rest.client.ClientBuilderDefaultValuesDecorator;
import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;
import co.codewizards.cloudstore.rest.client.CredentialsProvider;
import co.codewizards.cloudstore.rest.client.request.BeginPutFile;
import co.codewizards.cloudstore.rest.client.request.Copy;
import co.codewizards.cloudstore.rest.client.request.Delete;
import co.codewizards.cloudstore.rest.client.request.EndPutFile;
import co.codewizards.cloudstore.rest.client.request.EndSyncFromRepository;
import co.codewizards.cloudstore.rest.client.request.EndSyncToRepository;
import co.codewizards.cloudstore.rest.client.request.GetChangeSetDto;
import co.codewizards.cloudstore.rest.client.request.GetClientRepositoryDto;
import co.codewizards.cloudstore.rest.client.request.GetEncryptedSignedAuthToken;
import co.codewizards.cloudstore.rest.client.request.GetFileData;
import co.codewizards.cloudstore.rest.client.request.GetRepoFileDto;
import co.codewizards.cloudstore.rest.client.request.GetRepositoryDto;
import co.codewizards.cloudstore.rest.client.request.GetVersionInfoDto;
import co.codewizards.cloudstore.rest.client.request.MakeDirectory;
import co.codewizards.cloudstore.rest.client.request.MakeSymlink;
import co.codewizards.cloudstore.rest.client.request.Move;
import co.codewizards.cloudstore.rest.client.request.PutFileData;
import co.codewizards.cloudstore.rest.client.request.PutParentConfigPropSetDto;
import co.codewizards.cloudstore.rest.client.request.RequestRepoConnection;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import co.codewizards.cloudstore.rest.client.ssl.SSLContextBuilder;

public class RestRepoTransport extends AbstractRepoTransport implements CredentialsProvider, ContextWithLocalRepoManager {
	private static final Logger logger = LoggerFactory.getLogger(RestRepoTransport.class);

	public static final String CONFIG_KEY_GET_CHANGE_SET_DTO_TIMEOUT = "getChangeSetDtoTimeout";
	public static final long CONFIG_DEFAULT_GET_CHANGE_SET_DTO_TIMEOUT = 60L * 60L * 1000L;

	public static final String CONFIG_KEY_GET_REPO_FILE_DTO_WITH_FILE_CHUNK_DTOS_TIMEOUT = "getRepoFileDtoWithFileChunkDtosTimeout";
	public static final long CONFIG_DEFAULT_GET_REPO_FILE_DTO_WITH_FILE_CHUNK_DTOS_TIMEOUT = 60L * 60L * 1000L;

	private final long changeSetTimeout = ConfigImpl.getInstance().getPropertyAsPositiveOrZeroLong(
			CONFIG_KEY_GET_CHANGE_SET_DTO_TIMEOUT, CONFIG_DEFAULT_GET_CHANGE_SET_DTO_TIMEOUT);

	private final long fileChunkSetTimeout = ConfigImpl.getInstance().getPropertyAsPositiveOrZeroLong(
			CONFIG_KEY_GET_REPO_FILE_DTO_WITH_FILE_CHUNK_DTOS_TIMEOUT, CONFIG_DEFAULT_GET_REPO_FILE_DTO_WITH_FILE_CHUNK_DTOS_TIMEOUT);

//	public static final String CHANGE_SET_DTO_CACHE_FILE_NAME_TEMPLATE = "ChangeSetDto.${serverRepositoryId}.${lastRevisionSynced}.xml.gz";
	public static final String CHANGE_SET_DTO_CACHE_FILE_NAME_PREFIX = "ChangeSetDto.";
	public static final String CHANGE_SET_DTO_CACHE_FILE_NAME_SUFFIX = ".xml.gz";
	public static final String TMP_FILE_NAME_SUFFIX = ".tmp";

	private UUID repositoryId; // server-repository
	private byte[] publicKey;
	private String repositoryName; // server-repository
	private CloudStoreRestClient client;
	private final Map<UUID, AuthToken> clientRepositoryId2AuthToken = new HashMap<UUID, AuthToken>(1); // should never be more ;-)
	private LocalRepoManager localRepoManager;
	private File localRepoTmpDir;

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
		return requireNonNull(publicKey, "publicKey");
	}

	@Override
	public RepositoryDto getRepositoryDto() {
		return getClient().execute(new GetRepositoryDto(getRepositoryName()));
	}

	@Override
	public RepositoryDto getClientRepositoryDto() {
		getClientRepositoryIdOrFail();
		return getClient().execute(new GetClientRepositoryDto(getRepositoryName()));
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
	public ChangeSetDto getChangeSetDto(final boolean localSync, final Long lastSyncToRemoteRepoLocalRepositoryRevisionSynced) {
		File changeSetDtoCacheFile = null;
		ChangeSetDto result = null;

		try {
			changeSetDtoCacheFile = getChangeSetDtoCacheFile(lastSyncToRemoteRepoLocalRepositoryRevisionSynced);
			if (changeSetDtoCacheFile.isFile() && changeSetDtoCacheFile.length() > 0) {
				ChangeSetDtoIo changeSetDtoIo = createObject(ChangeSetDtoIo.class);
				result = changeSetDtoIo.deserializeWithGz(changeSetDtoCacheFile);
				logger.info("getChangeSetDto: Read ChangeSetDto-cache-file: {}", changeSetDtoCacheFile.getAbsolutePath());
				return result;
			} else {
				logger.info("getChangeSetDto: ChangeSetDto-cache-file NOT found: {}", changeSetDtoCacheFile.getAbsolutePath());
			}
		} catch (Exception x) {
			result = null;
			logger.error("getChangeSetDto: Reading ChangeSetDto-cache-file failed: " + x, x);
		}

		final long beginTimestamp = System.currentTimeMillis();
		while (true) {
			try {
				result = getClient().execute(new GetChangeSetDto(getRepositoryId().toString(), localSync, lastSyncToRemoteRepoLocalRepositoryRevisionSynced));
			} catch (final DeferredCompletionException x) {
				if (System.currentTimeMillis() > beginTimestamp + changeSetTimeout)
					throw new TimeoutException(String.format("Could not get change-set within %s milliseconds!", changeSetTimeout), x);

				logger.info("getChangeSetDto: Got DeferredCompletionException; will retry.");
			}

			if (result != null) {
				if (changeSetDtoCacheFile != null) {
					File tmpFile = changeSetDtoCacheFile.getParentFile().createFile(changeSetDtoCacheFile.getName() + TMP_FILE_NAME_SUFFIX);
					ChangeSetDtoIo changeSetDtoIo = createObject(ChangeSetDtoIo.class);
					changeSetDtoIo.serializeWithGz(result, tmpFile);
					if (! tmpFile.renameTo(changeSetDtoCacheFile)) {
						logger.error("getChangeSetDto: Could not rename temporary file to active ChangeSetDto-cache-file: {}", changeSetDtoCacheFile.getAbsolutePath());
					} else {
						logger.info("getChangeSetDto: Wrote ChangeSetDto-cache-file: {}", changeSetDtoCacheFile.getAbsolutePath());
					}
				}
				return result;
			}
		}
	}

	@Override
	public void prepareForChangeSetDto(ChangeSetDto changeSetDto) {
		// nothing to do here.
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
		for (final File file : getChangeSetDtoCacheFiles(true)) {
			file.delete();
		}
		File tmpDir = this.localRepoTmpDir;
		if (tmpDir != null) {
			tmpDir.delete(); // deletes only, if empty.
			this.localRepoTmpDir = null; // null this in order to ensure that it is re-created by getLocalRepoTmpDir().
		}
		getClient().execute(new EndSyncFromRepository(getRepositoryId().toString()));
	}

	@Override
	public void endSyncToRepository(final long fromLocalRevision) {
		getClient().execute(new EndSyncToRepository(getRepositoryId().toString(), fromLocalRevision));
	}

	@Override
	public void putParentConfigPropSetDto(ConfigPropSetDto parentConfigPropSetDto) {
		getClient().execute(new PutParentConfigPropSetDto(getRepositoryId().toString(), parentConfigPropSetDto));
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

			final File localRoot = LocalRepoRegistryImpl.getInstance().getLocalRoot(clientRepositoryId);
			final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
			try {
				final EncryptedSignedAuthToken encryptedSignedAuthToken = getClient().execute(new GetEncryptedSignedAuthToken(getRepositoryName(), localRepoManager.getRepositoryId()));

				final byte[] signedAuthTokenData = new SignedAuthTokenDecrypter(localRepoManager.getPrivateKey()).decrypt(encryptedSignedAuthToken);

				final SignedAuthToken signedAuthToken = new SignedAuthTokenIO().deserialise(signedAuthTokenData);

				final AuthTokenVerifier verifier = new AuthTokenVerifier(localRepoManager.getRemoteRepositoryPublicKeyOrFail(getRepositoryId()));
				verifier.verify(signedAuthToken);

				authToken = new AuthTokenIO().deserialise(signedAuthToken.getAuthTokenData());
				final Date expiryDate = requireNonNull(authToken.getExpiryDateTime(), "authToken.expiryDateTime").toDate();
				final Date renewalDate = requireNonNull(authToken.getRenewalDateTime(), "authToken.renewalDateTime").toDate();
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
		requireNonNull(authToken, "authToken");
		return System.currentTimeMillis() > authToken.getRenewalDateTime().getMillis();
	}

	protected CloudStoreRestClient getClient() {
		if (client == null) {
			ClientBuilder clientBuilder = createClientBuilder();
			final CloudStoreRestClient c = new CloudStoreRestClient(getRemoteRoot(), clientBuilder);
			c.setCredentialsProvider(this);
			client = c;
		}
		return client;
	}

	@Override
	protected URL determineRemoteRootWithoutPathPrefix() {
		final String repositoryName = getRepositoryName();
		final String baseURL = getClient().getBaseUrl();
		if (!baseURL.endsWith("/"))
			throw new IllegalStateException(String.format("baseURL does not end with a '/'! baseURL='%s'", baseURL));

		try {
			return new URL(baseURL + repositoryName);
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public String getRepositoryName() {
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

			final String baseURL = getClient().getBaseUrl();
			if (!baseURL.endsWith("/"))
				throw new IllegalStateException(String.format("baseURL does not end with a '/'! remoteRoot='%s' baseURL='%s'", remoteRoot, baseURL));

			final String remoteRootString = remoteRoot.toExternalForm();
			if (!remoteRootString.startsWith(baseURL))
				throw new IllegalStateException(String.format("remoteRoot does not start with baseURL! remoteRoot='%s' baseURL='%s'", remoteRoot, baseURL));

			this.pathAfterBaseURL = pathAfterBaseURL = remoteRootString.substring(baseURL.length());
		}
		return pathAfterBaseURL;
	}

	private ClientBuilder createClientBuilder(){
		final ClientBuilder builder = new ClientBuilderDefaultValuesDecorator();
		try {
			builder.sslContext(SSLContextBuilder.create()
					.remoteURL(getRemoteRoot())
					.callback(getDynamicX509TrustManagerCallback()).build());
		} catch (final GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
		return builder;
	}

	@Override
	public VersionInfoDto getVersionInfoDto() {
		final VersionInfoDto versionInfoDto = getClient().execute(new GetVersionInfoDto());
		return versionInfoDto;
	}

	protected File getChangeSetDtoCacheFile(final Long lastSyncToRemoteRepoLocalRepositoryRevisionSynced) {
		String fileName = CHANGE_SET_DTO_CACHE_FILE_NAME_PREFIX
				+ getRepositoryId() + "."
				+ lastSyncToRemoteRepoLocalRepositoryRevisionSynced
				+ CHANGE_SET_DTO_CACHE_FILE_NAME_SUFFIX;
		return getLocalRepoTmpDir().createFile(fileName);
	}

	protected List<File> getChangeSetDtoCacheFiles(final boolean includeTmpFiles) {
		File[] fileArray = getLocalRepoTmpDir().listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				if (! file.getName().startsWith(CHANGE_SET_DTO_CACHE_FILE_NAME_PREFIX))
					return false;

				if (file.getName().endsWith(CHANGE_SET_DTO_CACHE_FILE_NAME_SUFFIX))
					return true;

				if (includeTmpFiles && file.getName().endsWith(CHANGE_SET_DTO_CACHE_FILE_NAME_SUFFIX + TMP_FILE_NAME_SUFFIX))
					return true;
				else
					return false;
			}
		});
		return fileArray == null ? Collections.<File>emptyList() : Arrays.asList(fileArray);
	}

	protected File getLocalRepoTmpDir() {
		if (localRepoTmpDir == null) {
			try {
				final File metaDir = getLocalRepoMetaDir();
				if (! metaDir.isDirectory()) {
					if (metaDir.isFile())
						throw new IOException(String.format("Path '%s' already exists as ordinary file! It should be a directory!", metaDir.getAbsolutePath()));
					else
						throw new IOException(String.format("Directory '%s' does not exist!", metaDir.getAbsolutePath()));
				}

				final File tmpDir = metaDir.createFile(LocalRepoManager.REPO_TEMP_DIR_NAME);
				if (! tmpDir.isDirectory()) {
					tmpDir.mkdir();

					if (! tmpDir.isDirectory()) {
						if (tmpDir.isFile())
							throw new IOException(String.format("Cannot create directory '%s', because this path already exists as an ordinary file!", tmpDir.getAbsolutePath()));
						else
							throw new IOException(String.format("Creating directory '%s' failed for an unknown reason (permissions? disk full?)!", tmpDir.getAbsolutePath()));
					}
				}
				this.localRepoTmpDir = tmpDir;
			} catch (RuntimeException x) {
				throw x;
			} catch (Exception x) {
				throw new RuntimeException(x);
			}
		}
		return localRepoTmpDir;
	}

	protected File getLocalRepoMetaDir() {
		final File localRoot = LocalRepoRegistryImpl.getInstance().getLocalRootOrFail(getClientRepositoryIdOrFail());
		return createFile(localRoot, LocalRepoManager.META_DIR_NAME);
	}

	@Override
	public LocalRepoManager getLocalRepoManager() {
		if (localRepoManager == null) {
			logger.debug("getLocalRepoManager: Creating a new LocalRepoManager.");
			final File localRoot = LocalRepoRegistryImpl.getInstance().getLocalRoot(getClientRepositoryIdOrFail());
			localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		}
		return localRepoManager;
	}
}
