package co.codewizards.cloudstore.rest.client;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.auth.EncryptedSignedAuthToken;
import co.codewizards.cloudstore.core.concurrent.DeferredCompletionException;
import co.codewizards.cloudstore.core.dto.ChangeSetDTO;
import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.FileChunkSetDTO;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.util.StringUtil;
import co.codewizards.cloudstore.rest.client.jersey.CloudStoreJaxbContextResolver;

public class CloudStoreRESTClient {

	private static final Logger logger = LoggerFactory.getLogger(CloudStoreRESTClient.class);

	private static final int DEFAULT_SOCKET_CONNECT_TIMEOUT = 60 * 1000;
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
	 * @return
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
		Client client = acquireClient();
		try {
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
			releaseClient(client);
		}
	}

	private static final void doNothing() { }

	public void testSuccess() {
		Client client = acquireClient();
		try {
			String response = client.target(getBaseURL()).path("_test").request(MediaType.TEXT_PLAIN).get(String.class);
			if (!"SUCCESS".equals(response)) {
				throw new IllegalStateException("Server response invalid: " + response);
			}
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // we do not expect null
		} finally {
			releaseClient(client);
		}
	}

	public void testException() {
		Client client = acquireClient();
		try {
			Response response = client.target(getBaseURL()).path("_test").queryParam("exception", true).request().get();
			assertResponseIndicatesSuccess(response);
			throw new IllegalStateException("Server sent response instead of exception: " + response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // we do not expect null
		} finally {
			releaseClient(client);
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
		Client client = acquireClient();
		try {
			RepositoryDTO repositoryDTO = client.target(getBaseURL())
					.path(getPath(RepositoryDTO.class))
					.path(repositoryName)
					.request().get(RepositoryDTO.class);
			return repositoryDTO;
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // we do not expect null
		} finally {
			releaseClient(client);
		}
	}

	private String getPath(Class<?> dtoClass) {
		return "_" + dtoClass.getSimpleName();
	}

	public ChangeSetDTO getChangeSet(String repositoryName, boolean localSync) {
		assertNotNull("repositoryName", repositoryName);
		Client client = acquireClient();
		try {
			WebTarget webTarget = client.target(getBaseURL())
					.path(getPath(ChangeSetDTO.class))
					.path(repositoryName);

			if (localSync)
				webTarget = webTarget.queryParam("localSync", localSync);

			ChangeSetDTO changeSetDTO = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(ChangeSetDTO.class);
			return changeSetDTO;
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // we do not expect null
		} finally {
			releaseClient(client);
		}
	}

	public void requestRepoConnection(String repositoryName, String pathPrefix, RepositoryDTO clientRepositoryDTO) {
		assertNotNull("clientRepositoryDTO", clientRepositoryDTO);
		assertNotNull("clientRepositoryDTO.entityID", clientRepositoryDTO.getEntityID());
		assertNotNull("clientRepositoryDTO.publicKey", clientRepositoryDTO.getPublicKey());
		Client client = acquireClient();
		try {
			Response response = client.target(getBaseURL())
			.path("_requestRepoConnection").path(repositoryName).path(pathPrefix)
			.request().post(Entity.entity(clientRepositoryDTO, MediaType.APPLICATION_XML));
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // delete should never throw an exception, if it didn't have a real problem
		} finally {
			releaseClient(client);
		}
	}

	public EncryptedSignedAuthToken getEncryptedSignedAuthToken(String repositoryName, EntityID clientRepositoryID) {
		Client client = acquireClient();
		try {
			EncryptedSignedAuthToken encryptedSignedAuthToken = client.target(getBaseURL())
			.path(getPath(EncryptedSignedAuthToken.class)).path(repositoryName).path(clientRepositoryID.toString())
			.request(MediaType.APPLICATION_XML).get(EncryptedSignedAuthToken.class);
			return encryptedSignedAuthToken;
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // delete should never throw an exception, if it didn't have a real problem
		} finally {
			releaseClient(client);
		}
	}

	public FileChunkSetDTO getFileChunkSet(String repositoryName, String path) {
		assertNotNull("repositoryName", repositoryName);
		Client client = acquireClient();
		try {
			WebTarget webTarget = client.target(getBaseURL())
					.path(getPath(FileChunkSetDTO.class))
					.path(repositoryName)
					.path(removeLeadingAndTrailingSlashes(path));

			FileChunkSetDTO fileChunkSetDTO = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(FileChunkSetDTO.class);
			return fileChunkSetDTO;
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // we do not expect null
		} finally {
			releaseClient(client);
		}
	}

	public void beginPutFile(String repositoryName, String path) {
		assertNotNull("repositoryName", repositoryName);
		Client client = acquireClient();
		try {
			Response response = assignCredentials(client.target(getBaseURL())
			.path("_beginPutFile")
			.path(repositoryName)
			.path(removeLeadingAndTrailingSlashes(path))
			.request()).post(null);
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // delete should never throw an exception, if it didn't have a real problem
		} finally {
			releaseClient(client);
		}
	}

	public void endSyncFromRepository(String repositoryName) {
		assertNotNull("repositoryName", repositoryName);
		Client client = acquireClient();
		try {
			Response response = assignCredentials(client.target(getBaseURL())
			.path("_endSyncFromRepository")
			.path(repositoryName)
			.request()).post(null);
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // delete should never throw an exception, if it didn't have a real problem
		} finally {
			releaseClient(client);
		}
	}

	public void endSyncToRepository(String repositoryName, long fromLocalRevision) {
		assertNotNull("repositoryName", repositoryName);
		if (fromLocalRevision < 0)
			throw new IllegalArgumentException("fromLocalRevision < 0");

		Client client = acquireClient();
		try {
			Response response = assignCredentials(client.target(getBaseURL())
			.path("_endSyncToRepository")
			.path(repositoryName)
			.queryParam("fromLocalRevision", fromLocalRevision)
			.request()).post(null);
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // delete should never throw an exception, if it didn't have a real problem
		} finally {
			releaseClient(client);
		}
	}

	public void endPutFile(String repositoryName, String path, DateTime lastModified, long length) {
		assertNotNull("repositoryName", repositoryName);
		Client client = acquireClient();
		try {
			Response response = assignCredentials(client.target(getBaseURL())
					.path("_endPutFile")
					.path(repositoryName)
					.path(removeLeadingAndTrailingSlashes(path))
					.queryParam("lastModified", lastModified.toString())
					.queryParam("length",length)
					.request()).post(null);
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // delete should never throw an exception, if it didn't have a real problem
		} finally {
			releaseClient(client);
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
		Client client = acquireClient();
		try {
			WebTarget webTarget = client.target(getBaseURL()).path(repositoryName).path(removeLeadingAndTrailingSlashes(path));

			if (offset > 0) // defaults to 0
				webTarget = webTarget.queryParam("offset", offset);

			if (length >= 0) // defaults to -1 meaning "all"
				webTarget = webTarget.queryParam("length", length);

			return assignCredentials(webTarget.request(MediaType.APPLICATION_OCTET_STREAM)).get(byte[].class);
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // delete should never throw an exception, if it didn't have a real problem
		} finally {
			releaseClient(client);
		}
	}

	public void putFileData(String repositoryName, String path, long offset, byte[] fileData) {
		assertNotNull("repositoryName", repositoryName);
		assertNotNull("path", path);
		assertNotNull("fileData", fileData);
		Client client = acquireClient();
		try {
			WebTarget webTarget = client.target(getBaseURL()).path(repositoryName).path(removeLeadingAndTrailingSlashes(path));

			if (offset > 0)
				webTarget = webTarget.queryParam("offset", offset);

			Response response = assignCredentials(webTarget.request()).put(Entity.entity(fileData, MediaType.APPLICATION_OCTET_STREAM));
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // delete should never throw an exception, if it didn't have a real problem
		} finally {
			releaseClient(client);
		}
	}

	public void delete(String repositoryName, String path) {
		assertNotNull("repositoryName", repositoryName);
		Client client = acquireClient();
		try {
			Response response = assignCredentials(client.target(getBaseURL())
					.path(repositoryName).path(removeLeadingAndTrailingSlashes(path))
					.request()).delete();
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // delete should never throw an exception, if it didn't have a real problem
		} finally {
			releaseClient(client);
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
		Client client = acquireClient();
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

			WebTarget webTarget = client.target(getBaseURL())
					.path("_makeDirectory")
					.path(repositoryName).path(removeLeadingAndTrailingSlashes(path));

			if (lastModified != null)
				webTarget = webTarget.queryParam("lastModified", new DateTime(lastModified));

			Response response = assignCredentials(webTarget.request()).post(null);
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // delete should never throw an exception, if it didn't have a real problem
		} finally {
			releaseClient(client);
		}
	}

	private String removeLeadingAndTrailingSlashes(String path) {
		if (path == null)
			return null;

		String result = path;
		while (result.startsWith("/"))
			result = result.substring(1);

		while (result.endsWith("/"))
			result = result.substring(0, result.length() - 1);

		return result;
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

	private synchronized Client acquireClient()
	{
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


			client = clientBuilder.build();

			HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("anonymous", "");
//			HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder().build();
			client.register(feature);

			configFrozen = true;
		}
		return client;
	}

	private synchronized void releaseClient(Client client)
	{
		clientCache.add(client);
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
