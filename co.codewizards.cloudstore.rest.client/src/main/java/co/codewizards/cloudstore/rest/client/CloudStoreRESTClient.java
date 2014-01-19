package co.codewizards.cloudstore.rest.client;

import static co.codewizards.cloudstore.core.util.Util.*;

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
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.auth.EncryptedSignedAuthToken;
import co.codewizards.cloudstore.core.dto.ChangeSet;
import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.FileChunkSet;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.util.StringUtil;
import co.codewizards.cloudstore.rest.client.jersey.CloudStoreJaxbContextResolver;

public class CloudStoreRESTClient {

	private static final Logger logger = LoggerFactory.getLogger(CloudStoreRESTClient.class);

	private static final int TIMEOUT_SOCKET_CONNECT_MS = 10 * 1000; // TODO make timeout configurable
	private static final int TIMEOUT_SOCKET_READ_MS = 90 * 1000; // TODO make timeout configurable

	private final String url;
	private String baseURL;

	private LinkedList<Client> clientCache = new LinkedList<Client>();

	private boolean configFrozen;

	private HostnameVerifier hostnameVerifier;
	private SSLContext sslContext;

	private CredentialsProvider credentialsProvider;

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
				if (error != null)
					throw new RemoteException(error);
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

	public ChangeSet getChangeSet(String repositoryName, boolean localSync, EntityID toRepositoryID) {
		assertNotNull("repositoryName", repositoryName);
		Client client = acquireClient();
		try {
			WebTarget webTarget = client.target(getBaseURL())
					.path(getPath(ChangeSet.class))
					.path(repositoryName)
					.path(toRepositoryID.toString());

			if (localSync)
				webTarget = webTarget.queryParam("localSync", localSync);

			ChangeSet changeSet = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(ChangeSet.class);
			return changeSet;
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // we do not expect null
		} finally {
			releaseClient(client);
		}
	}

	public void requestRepoConnection(String repositoryName, RepositoryDTO clientRepositoryDTO) {
		assertNotNull("repositoryDTO", clientRepositoryDTO);
		assertNotNull("repositoryDTO.entityID", clientRepositoryDTO.getEntityID());
		assertNotNull("repositoryDTO.publicKey", clientRepositoryDTO.getPublicKey());
		Client client = acquireClient();
		try {
			Response response = client.target(getBaseURL())
			.path("_requestRepoConnection").path(repositoryName)
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

	public FileChunkSet getFileChunkSet(String repositoryName, String path, boolean allowHollow) {
		assertNotNull("repositoryName", repositoryName);
		Client client = acquireClient();
		try {
			WebTarget webTarget = client.target(getBaseURL())
					.path(getPath(FileChunkSet.class))
					.path(repositoryName)
					.path(removeLeadingAndTrailingSlashes(path));

			if (allowHollow)
				webTarget = webTarget.queryParam("allowHollow", allowHollow);

			FileChunkSet fileChunkSet = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(FileChunkSet.class);
			return fileChunkSet;
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // we do not expect null
		} finally {
			releaseClient(client);
		}
	}

	public void beginPutFile(EntityID fromRepositoryID, String repositoryName, String path) {
		assertNotNull("fromRepositoryID", fromRepositoryID);
		assertNotNull("repositoryName", repositoryName);
		Client client = acquireClient();
		try {
			Response response = assignCredentials(client.target(getBaseURL())
			.path("_beginPutFile")
			.path(repositoryName)
			.path(removeLeadingAndTrailingSlashes(path))
			.queryParam("fromRepositoryID", fromRepositoryID.toString())
			.request()).post(null);
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // delete should never throw an exception, if it didn't have a real problem
		} finally {
			releaseClient(client);
		}
	}

	public void endSyncFromRepository(String repositoryName, EntityID fromRepositoryID) {
		assertNotNull("repositoryName", repositoryName);
		Client client = acquireClient();
		try {
			Response response = assignCredentials(client.target(getBaseURL())
			.path("_endSyncFromRepository")
			.path(repositoryName)
			.path(fromRepositoryID.toString())
			.request()).post(null);
			assertResponseIndicatesSuccess(response);
		} catch (RuntimeException x) {
			handleException(x);
			throw x; // delete should never throw an exception, if it didn't have a real problem
		} finally {
			releaseClient(client);
		}
	}

	public void endSyncToRepository(String repositoryName, EntityID fromRepositoryID, long fromLocalRevision) {
		assertNotNull("repositoryName", repositoryName);
		if (fromLocalRevision < 0)
			throw new IllegalArgumentException("fromLocalRevision < 0");

		Client client = acquireClient();
		try {
			Response response = assignCredentials(client.target(getBaseURL())
			.path("_endSyncToRepository")
			.path(repositoryName)
			.path(fromRepositoryID.toString())
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

	public void endPutFile(EntityID fromRepositoryID, String repositoryName, String path, DateTime lastModified, long length) {
		assertNotNull("fromRepositoryID", fromRepositoryID);
		assertNotNull("repositoryName", repositoryName);
		Client client = acquireClient();
		try {
			Response response = assignCredentials(client.target(getBaseURL())
					.path("_endPutFile")
					.path(repositoryName)
					.path(removeLeadingAndTrailingSlashes(path))
					.queryParam("fromRepositoryID", fromRepositoryID.toString())
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

	public void delete(EntityID fromRepositoryID, String repositoryName, String path) {
		assertNotNull("repositoryName", repositoryName);
		Client client = acquireClient();
		try {
			Response response = assignCredentials(client.target(getBaseURL())
					.path(repositoryName).path(removeLeadingAndTrailingSlashes(path))
					.queryParam("fromRepositoryID", fromRepositoryID)
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

	public void makeDirectory(EntityID fromRepositoryID, String repositoryName, String path, Date lastModified) {
		assertNotNull("fromRepositoryID", fromRepositoryID);
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
					.path(repositoryName).path(removeLeadingAndTrailingSlashes(path))
					.queryParam("fromRepositoryID", fromRepositoryID);

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
//			X509HostnameVerifier x509HostnameVerifier = this.x509HostnameVerifier;


//			ClientConfig clientConfig = new DefaultClientConfig(CloudStoreJaxbContextResolver.class);
//			clientConfig.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, Integer.valueOf(TIMEOUT_SOCKET_CONNECT_MS)); // must be a java.lang.Integer
//			clientConfig.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, Integer.valueOf(TIMEOUT_SOCKET_READ_MS)); // must be a java.lang.Integer
//
//			if (sslContext != null) {
//				HTTPSProperties httpsProperties = new HTTPSProperties(hostnameVerifier, sslContext); // hostnameVerifier is optional
//				clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, httpsProperties);
//			}
//			else if (hostnameVerifier != null)
//				throw new IllegalStateException("sslContext must not be null, if hostnameVerifier is set!");
//
//			client = Client.create(clientConfig);

//			RequestConfig defaultRequestConfig = RequestConfig.custom()
//				    .setSocketTimeout(TIMEOUT_SOCKET_READ_MS)
//				    .setConnectTimeout(TIMEOUT_SOCKET_CONNECT_MS)
//				    .setConnectionRequestTimeout(TIMEOUT_SOCKET_READ_MS)
//				    .setStaleConnectionCheckEnabled(true)
//				    .build();
//
//			HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().setDefaultRequestConfig(defaultRequestConfig);
//
//			if (sslContext != null)
//				httpClientBuilder.setSslcontext(sslContext);
//
//			if (x509HostnameVerifier != null)
//				httpClientBuilder.setHostnameVerifier(x509HostnameVerifier);
//
//			HttpClient apacheClient = httpClientBuilder.build();
//			client = new Client(new ApacheConnectorProvider(apacheClient, new BasicCookieStore(), true));

			// TODO Timeouts!
			Configuration clientConfig = new ClientConfig(CloudStoreJaxbContextResolver.class);

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

		logger.error("handleException: " + x, x);
		Error error = null;
		try {
			response.bufferEntity();
			if (response.hasEntity())
				error = response.readEntity(Error.class);
		} catch (Exception y) {
			logger.error("handleException: " + y, y);
		}

		if (error != null)
			throw new RemoteException(error);

		throw x;
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
