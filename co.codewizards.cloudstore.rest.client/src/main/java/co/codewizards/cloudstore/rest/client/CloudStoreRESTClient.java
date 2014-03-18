package co.codewizards.cloudstore.rest.client;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.uri.UriComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.auth.EncryptedSignedAuthToken;
import co.codewizards.cloudstore.core.concurrent.DeferredCompletionException;
import co.codewizards.cloudstore.core.dto.ChangeSetDTO;
import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.util.StringUtil;
import co.codewizards.cloudstore.rest.client.jersey.CloudStoreJaxbContextResolver;
import co.codewizards.cloudstore.rest.shared.GZIPReaderInterceptor;
import co.codewizards.cloudstore.rest.shared.GZIPWriterInterceptor;

public class CloudStoreRESTClient {

	private static final Logger logger = LoggerFactory.getLogger(CloudStoreRESTClient.class);

	private static final int DEFAULT_SOCKET_CONNECT_TIMEOUT = 120 * 1000;
	private static final int DEFAULT_SOCKET_READ_TIMEOUT = 15 * 60 * 1000;

	public static final String SYSTEM_PROPERTY_SOCKET_CONNECT_TIMEOUT = "cloudstore.socketConnectTimeout";

	public static final String SYSTEM_PROPERTY_SOCKET_READ_TIMEOUT = "cloudstore.socketReadTimeout";

	private Integer socketConnectTimeout;

	private Integer socketReadTimeout;

	private final String url;
	private String baseURL;

	private LinkedList<Client> clientCache = new LinkedList<Client>();

	private boolean configFrozen;

	private HostnameVerifier hostnameVerifier;
	private SSLContext sslContext;

	private CredentialsProvider credentialsProvider;

	public Integer getSocketConnectTimeout() {
		if (socketConnectTimeout == null)
			socketConnectTimeout = getTimeoutFromConfig(SYSTEM_PROPERTY_SOCKET_CONNECT_TIMEOUT, DEFAULT_SOCKET_CONNECT_TIMEOUT);

		return socketConnectTimeout;
	}
	public void setSocketConnectTimeout(Integer socketConnectTimeout) {
		if (socketConnectTimeout != null && socketConnectTimeout < 0)
			socketConnectTimeout = null;

		this.socketConnectTimeout = socketConnectTimeout;
	}

	public Integer getSocketReadTimeout() {
		if (socketReadTimeout == null)
			socketReadTimeout = getTimeoutFromConfig(SYSTEM_PROPERTY_SOCKET_READ_TIMEOUT, DEFAULT_SOCKET_READ_TIMEOUT);

		return socketReadTimeout;
	}
	public void setSocketReadTimeout(Integer socketReadTimeout) {
		if (socketReadTimeout != null && socketReadTimeout < 0)
			socketReadTimeout = null;

		this.socketReadTimeout = socketReadTimeout;
	}

	private Integer getTimeoutFromConfig(String systemProperty, int defaultValue) {
		// TODO read properties file in ~/.cloudstore/ with sys-prop as override, prop-file as normal and default as third-level-fallback.
		String value = System.getProperty(systemProperty);
		if (value == null || value.isEmpty()) {
			logger.warn("System property '{}' is undefined! Using default value {}.", systemProperty, defaultValue);
			return defaultValue;
		}
		try {
			Integer result = Integer.valueOf(value);
			return result;
		} catch (NumberFormatException x) {
			logger.warn("System property '{}' is set to the illegal value '{}'! Falling back to default value {}.", systemProperty, value, defaultValue);
			return defaultValue;
		}
	}

	/**
	 * Get the server's base-URL.
	 * <p>
	 * This base-URL is the base of the <code>CloudStoreREST</code> application. Hence all URLs
	 * beneath this base-URL are processed by the <code>CloudStoreREST</code> application.
	 * <p>
	 * In other words: All repository-names are located directly beneath this base-URL. The special services, too,
	 * are located directly beneath this base-URL.
	 * <p>
	 * For example, if the server's base-URL is "https://host.domain:8443/", then the test-service is
	 * available via "https://host.domain:8443/_test" and the repository with the alias "myrepo" is
	 * "https://host.domain:8443/myrepo".
	 * @return the base-URL. This URL always ends with "/".
	 */
	public synchronized String getBaseURL() {
		if (baseURL == null) {
			determineBaseURL();
		}
		return baseURL;
	}

	/**
	 * Create a new client.
	 * @param url any URL to the server. Must not be <code>null</code>.
	 * May be the base-URL, any repository's remote-root-URL or any URL within a remote-root-URL.
	 * The base-URL is automatically determined by cutting sub-paths, step by step.
	 */
	public CloudStoreRESTClient(URL url)
	{
		this(assertNotNull("url", url).toExternalForm());
	}

	/**
	 * Create a new client.
	 * @param url any URL to the server. Must not be <code>null</code>.
	 * May be the base-URL, any repository's remote-root-URL or any URL within a remote-root-URL.
	 * The base-URL is automatically determined by cutting sub-paths, step by step.
	 */
	public CloudStoreRESTClient(String url)
	{
		this.url = assertNotNull("url", url);
	}

	private static String appendFinalSlashIfNeeded(String url) {
		return url.endsWith("/") ? url : url + "/";
	}

	private void determineBaseURL() {
		acquireClient();
		try {
			Client client = getClientOrFail();
			String url = appendFinalSlashIfNeeded(this.url);
			while (true) {
				String testUrl = url + "_test";
				try {
					String response = client.target(testUrl).request(MediaType.TEXT_PLAIN).get(String.class);
					if ("SUCCESS".equals(response)) {
						baseURL = url;
						break;
					}
				} catch (WebApplicationException x) { doNothing(); }

				if (!url.endsWith("/"))
					throw new IllegalStateException("url does not end with '/'!");

				int secondLastSlashIndex = url.lastIndexOf('/', url.length() - 2);
				url = url.substring(0, secondLastSlashIndex + 1);

				if (StringUtil.getIndexesOf(url, '/').size() < 3)
					throw new IllegalStateException("baseURL not found!");
			}
		} finally {
			releaseClient();
		}
	}

	private static final void doNothing() { }

	public void testSuccess() {
		acquireClient();
		try {
			String response = createWebTarget("_test").request(MediaType.TEXT_PLAIN).get(String.class);
			if (!"SUCCESS".equals(response)) {
				throw new IllegalStateException("Server response invalid: " + response);
			}
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // we do not expect null
		} finally {
			releaseClient();
		}
	}

	public void testException() {
		acquireClient();
		try {
			Response response = createWebTarget("_test").queryParam("exception", true).request().get();
			assertResponseIndicatesSuccess(response);
			throw new IllegalStateException("Server sent response instead of exception: " + response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // we do not expect null
		} finally {
			releaseClient();
		}
	}

	private void assertResponseIndicatesSuccess(Response response) {
		if (400 <= response.getStatus() && response.getStatus() <= 599) {
			response.bufferEntity();
			if (response.hasEntity()) {
				Error error = null;
				try {
					error = response.readEntity(Error.class);
				} catch (Exception y) {
					logger.error("handleException: " + y, y);
				}
				if (error != null) {
					throwOriginalExceptionIfPossible(error);
					throw new RemoteException(error);
				}
			}
			throw new WebApplicationException(response);
		}
	}

	public RepositoryDTO getRepositoryDTO(String repositoryName) {
		assertNotNull("repositoryName", repositoryName);
		acquireClient();
		try {
			RepositoryDTO repositoryDTO = createWebTarget(getPath(RepositoryDTO.class), urlEncode(repositoryName))
					.request().get(RepositoryDTO.class);
			return repositoryDTO;
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // we do not expect null
		} finally {
			releaseClient();
		}
	}

	private String getPath(Class<?> dtoClass) {
		return "_" + dtoClass.getSimpleName();
	}

	public ChangeSetDTO getChangeSet(String repositoryName, boolean localSync) {
		assertNotNull("repositoryName", repositoryName);
		acquireClient();
		try {
			WebTarget webTarget = createWebTarget(getPath(ChangeSetDTO.class), urlEncode(repositoryName));

			if (localSync)
				webTarget = webTarget.queryParam("localSync", localSync);

			ChangeSetDTO changeSetDTO = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(ChangeSetDTO.class);
			return changeSetDTO;
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // we do not expect null
		} finally {
			releaseClient();
		}
	}

	public void requestRepoConnection(String repositoryName, String pathPrefix, RepositoryDTO clientRepositoryDTO) {
		assertNotNull("clientRepositoryDTO", clientRepositoryDTO);
		assertNotNull("clientRepositoryDTO.repositoryId", clientRepositoryDTO.getRepositoryId());
		assertNotNull("clientRepositoryDTO.publicKey", clientRepositoryDTO.getPublicKey());
		acquireClient();
		try {
			Response response = createWebTarget("_requestRepoConnection", urlEncode(repositoryName), pathPrefix)
					.request().post(Entity.entity(clientRepositoryDTO, MediaType.APPLICATION_XML));
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x;
		} finally {
			releaseClient();
		}
	}

	public EncryptedSignedAuthToken getEncryptedSignedAuthToken(String repositoryName, UUID clientRepositoryId) {
		assertNotNull("repositoryName", repositoryName);
		assertNotNull("clientRepositoryId", clientRepositoryId);
		acquireClient();
		try {
			EncryptedSignedAuthToken encryptedSignedAuthToken = createWebTarget(
					getPath(EncryptedSignedAuthToken.class), urlEncode(repositoryName), clientRepositoryId.toString())
					.request(MediaType.APPLICATION_XML).get(EncryptedSignedAuthToken.class);
			return encryptedSignedAuthToken;
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // we should never receive (and return) null.
		} finally {
			releaseClient();
		}
	}

	public RepoFileDTO getRepoFileDTO(String repositoryName, String path) {
		assertNotNull("repositoryName", repositoryName);
		acquireClient();
		try {
			WebTarget webTarget = createWebTarget(getPath(RepoFileDTO.class), urlEncode(repositoryName), encodePath(path));
			RepoFileDTO repoFileDTO = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(RepoFileDTO.class);
			return repoFileDTO;
		} catch (RuntimeException x) {
			handleException(x);
			return null;
		} finally {
			releaseClient();
		}
	}

	public void beginPutFile(String repositoryName, String path) {
		assertNotNull("repositoryName", repositoryName);
		acquireClient();
		try {
			Response response = assignCredentials(
					createWebTarget("_beginPutFile", urlEncode(repositoryName), encodePath(path))
					.request()).post(null);
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x;
		} finally {
			releaseClient();
		}
	}

	public void endSyncFromRepository(String repositoryName) {
		assertNotNull("repositoryName", repositoryName);
		acquireClient();
		try {
			Response response = assignCredentials(
					createWebTarget("_endSyncFromRepository", urlEncode(repositoryName))
					.request()).post(null);
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x;
		} finally {
			releaseClient();
		}
	}

	public void endSyncToRepository(String repositoryName, long fromLocalRevision) {
		assertNotNull("repositoryName", repositoryName);
		if (fromLocalRevision < 0)
			throw new IllegalArgumentException("fromLocalRevision < 0");

		acquireClient();
		try {
			Response response = assignCredentials(
					createWebTarget("_endSyncToRepository", urlEncode(repositoryName))
					.queryParam("fromLocalRevision", fromLocalRevision)
					.request()).post(null);
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x;
		} finally {
			releaseClient();
		}
	}

	public void endPutFile(String repositoryName, String path, DateTime lastModified, long length) {
		assertNotNull("repositoryName", repositoryName);
		acquireClient();
		try {
			Response response = assignCredentials(
					createWebTarget("_endPutFile", urlEncode(repositoryName), encodePath(path))
					.queryParam("lastModified", lastModified.toString())
					.queryParam("length", length)
					.request()).post(null);
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x;
		} finally {
			releaseClient();
		}
	}

	private Invocation.Builder assignCredentials(Invocation.Builder builder) {
		CredentialsProvider credentialsProvider = getCredentialsProviderOrFail();
		builder.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, credentialsProvider.getUserName());
		builder.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, credentialsProvider.getPassword());
		return builder;
	}

	public byte[] getFileData(String repositoryName, String path, long offset, int length) {
		assertNotNull("repositoryName", repositoryName);
		acquireClient();
		try {
			WebTarget webTarget = createWebTarget(urlEncode(repositoryName), encodePath(path));

			if (offset > 0) // defaults to 0
				webTarget = webTarget.queryParam("offset", offset);

			if (length >= 0) // defaults to -1 meaning "all"
				webTarget = webTarget.queryParam("length", length);

			return assignCredentials(webTarget.request(MediaType.APPLICATION_OCTET_STREAM)).get(byte[].class);
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // we should never receive (and return) null.
		} finally {
			releaseClient();
		}
	}

	public void putFileData(String repositoryName, String path, long offset, byte[] fileData) {
		assertNotNull("repositoryName", repositoryName);
		assertNotNull("path", path);
		assertNotNull("fileData", fileData);
		acquireClient();
		try {
			WebTarget webTarget = createWebTarget(urlEncode(repositoryName), encodePath(path));

			if (offset > 0)
				webTarget = webTarget.queryParam("offset", offset);

			Response response = assignCredentials(webTarget.request()).put(Entity.entity(fileData, MediaType.APPLICATION_OCTET_STREAM));
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x;
		} finally {
			releaseClient();
		}
	}

	public void delete(String repositoryName, String path) {
		assertNotNull("repositoryName", repositoryName);
		acquireClient();
		try {
			Response response = assignCredentials(
					createWebTarget(urlEncode(repositoryName), encodePath(path)).request()).delete();
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // delete should never throw an exception, if it didn't have a real problem
		} finally {
			releaseClient();
		}
	}

	public void copy(String repositoryName, String fromPath, String toPath) {
		assertNotNull("repositoryName", repositoryName);
		acquireClient();
		try {
			Response response = assignCredentials(createWebTarget("_copy", urlEncode(repositoryName), encodePath(fromPath))
					.queryParam("to", encodePath(toPath))
					.request()).post(null);
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x;
		} finally {
			releaseClient();
		}
	}

	public void move(String repositoryName, String fromPath, String toPath) {
		assertNotNull("repositoryName", repositoryName);
		acquireClient();
		try {
			Response response = assignCredentials(createWebTarget("_move", urlEncode(repositoryName), encodePath(fromPath))
					.queryParam("to", encodePath(toPath))
					.request()).post(null);
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x;
		} finally {
			releaseClient();
		}
	}

//	public void localSync(String repositoryName) {
//		assertNotNull("repositoryName", repositoryName);
//		Client client = acquireClient();
//		try {
//			Response response = client.target(getBaseURL()).path("_localSync").path(repositoryName).request().post(null);
//			assertResponseIndicatesSuccess(response);
//		} catch (RuntimeException x) {
//			handleException(x);
//			throw x; // delete should never throw an exception, if it didn't have a real problem
//		} finally {
//			releaseClient(client);
//		}
//	}

	public void makeDirectory(String repositoryName, String path, Date lastModified) {
		assertNotNull("repositoryName", repositoryName);
		assertNotNull("path", path);
		acquireClient();
		try {
//			WebTarget webTarget = client.target(getBaseURL()).path(repositoryName).path(removeLeadingAndTrailingSlash(path));
//
//			if (lastModified != null)
//				webTarget = webTarget.queryParam("lastModified", new DateTime(lastModified));
//
//			Response response = webTarget.request().method("MKCOL");
//			assertResponseIndicatesSuccess(response);

			// The HTTP verb "MKCOL" is not yet supported by Jersey (and not even the unterlying HTTP client)
			// by default. We first have to add this. This will be done later (for the WebDAV support). For
			// now, we'll use the alternative MakeDirectoryService.

			WebTarget webTarget = createWebTarget("_makeDirectory", urlEncode(repositoryName), encodePath(path));

			if (lastModified != null)
				webTarget = webTarget.queryParam("lastModified", new DateTime(lastModified));

			Response response = assignCredentials(webTarget.request()).post(null);
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x;
		} finally {
			releaseClient();
		}
	}

	/**
	 * Create a {@link WebTarget} from the given path segments.
	 * <p>
	 * This method prefixes the path with the {@link #getBaseURL() base-URL} and appends
	 * all path segments separated via slashes ('/').
	 * <p>
	 * We do not use <code>client.target(getBaseURL()).path("...")</code>, because the
	 * {@link WebTarget#path(String) path(...)} method does not encode curly braces
	 * (which might be part of a file name!).
	 * Instead it resolves them using {@linkplain WebTarget#matrixParam(String, Object...) matrix-parameters}.
	 * The matrix-parameters need to be encoded manually, too (at least I tried it and it failed, if I didn't).
	 * Because of these reasons and in order to make the calls more compact, we assemble the path
	 * ourselves here.
	 * @param pathSegments the parts of the path. May be <code>null</code>. The path segments are
	 * appended to the path as they are. They are not encoded at all! If you require encoding,
	 * use {@link #encodePath(String)} or {@link #urlEncode(String)} before! Furthermore, all path segments
	 * are separated with a slash inbetween them, but <i>not</i> at the end. If a single path segment
	 * already contains a slash, duplicate slashes might occur.
	 * @return the target. Never <code>null</code>.
	 */
	private WebTarget createWebTarget(String ... pathSegments) {
		Client client = getClientOrFail();

		StringBuilder sb = new StringBuilder();
		sb.append(getBaseURL());

		boolean first = true;
		if (pathSegments != null && pathSegments.length != 0) {
			for (String pathSegment : pathSegments) {
				if (!first) // the base-URL already ends with a slash!
					sb.append('/');
				first = false;
				sb.append(pathSegment);
			}
		}

		WebTarget webTarget = client.target(URI.create(sb.toString()));
		return webTarget;
	}

	/**
	 * Encodes the given {@code path} (using {@link #urlEncode(String)}) and removes leading &amp; trailing slashes.
	 * <p>
	 * Slashes are not encoded, but retained as they are; only the path segments (the strings between the slashes) are
	 * encoded.
	 * <p>
	 * Duplicate slashes are removed.
	 * <p>
	 * The result of this method can be used in both URL-paths and URL-query-parameters.
	 * <p>
	 * For example the input "/some//ex ample///path/" becomes "some/ex%20ample/path".
	 * @param path the path to be encoded. Must not be <code>null</code>.
	 * @return the encoded path. Never <code>null</code>.
	 */
	private String encodePath(String path) {
		assertNotNull("path", path);

		StringBuilder sb = new StringBuilder();
		String[] segments = path.split("/");
		for (String segment : segments) {
			if (segment.isEmpty())
				continue;

			if (sb.length() != 0)
				sb.append('/');

			sb.append(urlEncode(segment));
		}

		return sb.toString();
	}

	/**
	 * Encodes the given {@code string}.
	 * <p>
	 * This method does <i>not</i> use {@link java.net.URLEncoder URLEncoder}, because of
	 * <a href="https://java.net/jira/browse/JERSEY-417">JERSEY-417</a>.
	 * <p>
	 * The result of this method can be used in both URL-paths and URL-query-parameters.
	 * @param string the {@code String} to be encoded. Must not be <code>null</code>.
	 * @return the encoded {@code String}.
	 */
	private static String urlEncode(String string) {
		assertNotNull("string", string);
		// This UriComponent method is safe. It does not try to handle the '{' and '}'
		// specially and with type PATH_SEGMENT, it encodes spaces using '%20' instead of '+'.
		// It can therefore be used for *both* path segments *and* query parameters.
		return UriComponent.encode(string, UriComponent.Type.PATH_SEGMENT);
	}

	public synchronized HostnameVerifier getHostnameVerifier() {
		return hostnameVerifier;
	}
	public synchronized void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
		if (configFrozen)
			throw new IllegalStateException("Config already frozen! Cannot change hostnameVerifier anymore!");

		this.hostnameVerifier = hostnameVerifier;
	}

	public synchronized SSLContext getSslContext() {
		return sslContext;
	}
	public synchronized void setSslContext(SSLContext sslContext) {
		if (configFrozen)
			throw new IllegalStateException("Config already frozen! Cannot change sslContext anymore!");

		this.sslContext = sslContext;
	}

	private ThreadLocal<ClientRef> clientThreadLocal = new ThreadLocal<ClientRef>();

	private static class ClientRef {
		public final Client client;
		public int refCount = 1;

		public ClientRef(Client client) {
			this.client = assertNotNull("client", client);
		}
	}

	/**
	 * Acquire a {@link Client} and bind it to the current thread.
	 * <p>
	 * <b>Important: You must {@linkplain #releaseClient() release} the client!</b> Use a try/finally block!
	 * @see #releaseClient()
	 * @see #getClientOrFail()
	 */
	private synchronized void acquireClient()
	{
		ClientRef clientRef = clientThreadLocal.get();
		if (clientRef != null) {
			++clientRef.refCount;
			return;
		}

		Client client = clientCache.poll();
		if (client == null) {
			SSLContext sslContext = this.sslContext;
			HostnameVerifier hostnameVerifier = this.hostnameVerifier;

			ClientConfig clientConfig = new ClientConfig(CloudStoreJaxbContextResolver.class);
			clientConfig.property(ClientProperties.CONNECT_TIMEOUT, getSocketConnectTimeout()); // must be a java.lang.Integer
			clientConfig.property(ClientProperties.READ_TIMEOUT, getSocketReadTimeout()); // must be a java.lang.Integer

			ClientBuilder clientBuilder = ClientBuilder.newBuilder().withConfig(clientConfig);

			if (sslContext != null)
				clientBuilder.sslContext(sslContext);

			if (hostnameVerifier != null)
				clientBuilder.hostnameVerifier(hostnameVerifier);

			clientBuilder.register(GZIPReaderInterceptor.class);
			clientBuilder.register(GZIPWriterInterceptor.class);

			client = clientBuilder.build();

			// An authentication is always required. Otherwise Jersey throws an exception.
			// Hence, we set it to "anonymous" here and set it to the real values for those
			// requests really requiring it.
			HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("anonymous", "");
			client.register(feature);

			configFrozen = true;
		}
		clientThreadLocal.set(new ClientRef(client));
	}

	/**
	 * Get the {@link Client} which was previously {@linkplain #acquireClient() acquired} (and not yet
	 * {@linkplain #releaseClient() released}) on the same thread.
	 * @return the {@link Client}. Never <code>null</code>.
	 * @throws IllegalStateException if there is no {@link Client} bound to the current thread.
	 * @see #acquireClient()
	 */
	private Client getClientOrFail() {
		ClientRef clientRef = clientThreadLocal.get();
		if (clientRef == null)
			throw new IllegalStateException("acquireClient() not called on the same thread (or releaseClient() already called)!");

		return clientRef.client;
	}

	/**
	 * Release a {@link Client} which was previously {@linkplain #acquireClient() acquired}.
	 * @see #acquireClient()
	 */
	private void releaseClient() {
		ClientRef clientRef = clientThreadLocal.get();
		if (clientRef == null)
			throw new IllegalStateException("acquireClient() not called on the same thread (or releaseClient() called more often than acquireClient())!");

		if (--clientRef.refCount == 0) {
			clientThreadLocal.remove();
			clientCache.add(clientRef.client);
		}
	}

	private void handleException(RuntimeException x)
	{
		Response response = null;
		if (x instanceof WebApplicationException)
			response = ((WebApplicationException)x).getResponse();
		else if (x instanceof ResponseProcessingException)
			response = ((ResponseProcessingException)x).getResponse();

		if (response == null)
			throw x;

		// Instead of returning null, jersey throws a com.sun.jersey.api.client.UniformInterfaceException
		// when the server does not send a result. We therefore check for the result code 204 here.
		if (Response.Status.NO_CONTENT.getStatusCode() == response.getStatus())
			return;

		Error error = null;
		try {
			response.bufferEntity();
			if (response.hasEntity())
				error = response.readEntity(Error.class);

			if (error != null && DeferredCompletionException.class.getName().equals(error.getClassName()))
				logger.debug("handleException: " + x, x);
			else
				logger.error("handleException: " + x, x);

		} catch (Exception y) {
			logger.error("handleException: " + x, x);
			logger.error("handleException: " + y, y);
		}

		if (error != null) {
			throwOriginalExceptionIfPossible(error);
			throw new RemoteException(error);
		}

		throw x;
	}

	private void throwOriginalExceptionIfPossible(Error error) {
		Class<?> clazz;
		try {
			clazz = Class.forName(error.getClassName());
		} catch (ClassNotFoundException e) {
			return;
		}
		if (!Throwable.class.isAssignableFrom(clazz))
			return;

		Object throwableO = null;
		if (throwableO == null) {
			throwableO = getObjectOrNull(clazz, new Class<?>[] { String.class }, error.getMessage());
		}

		if (throwableO == null) {
			throwableO = getObjectOrNull(clazz, null);
		}

		if (throwableO != null) {
			Throwable throwable = (Throwable) throwableO;
			throwable.initCause(new RemoteException(error));
			if (throwable instanceof RuntimeException)
				throw (RuntimeException) throwable;

			if (throwable instanceof java.lang.Error)
				throw (java.lang.Error) throwable;

			throw new RuntimeException(throwable);
		}
	}

	private Object getObjectOrNull(Class<?> clazz, Class<?>[] argumentTypes, Object ... arguments) {
		Object result = null;
		if (argumentTypes == null)
			argumentTypes = new Class<?> [0];

		if (argumentTypes.length == 0) {
			try {
				result = clazz.newInstance();
			} catch (InstantiationException e) {
				return null;
			} catch (IllegalAccessException e) {
				return null;
			}
		}

		if (result == null) {
			Constructor<?> constructor;
			try {
				constructor = clazz.getConstructor(argumentTypes);
			} catch (NoSuchMethodException e) {
				return null;
			} catch (SecurityException e) {
				return null;
			}

			try {
				result = constructor.newInstance(arguments);
			} catch (InstantiationException e) {
				return null;
			} catch (IllegalAccessException e) {
				return null;
			} catch (IllegalArgumentException e) {
				return null;
			} catch (InvocationTargetException e) {
				return null;
			}
		}

		return result;
	}

	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}
	private CredentialsProvider getCredentialsProviderOrFail() {
		CredentialsProvider credentialsProvider = getCredentialsProvider();
		if (credentialsProvider == null)
			throw new IllegalStateException("credentialsProvider == null");
		return credentialsProvider;
	}
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}
}
