package co.codewizards.cloudstore.rest.client;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.net.URL;
import java.util.LinkedList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.ChangeSet;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.util.StringUtil;
import co.codewizards.cloudstore.rest.client.internal.PathSegment;
import co.codewizards.cloudstore.rest.client.internal.QueryParameter;
import co.codewizards.cloudstore.rest.client.internal.RelativePathPart;
import co.codewizards.cloudstore.rest.client.jersey.CloudStoreJaxbContextResolver;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

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
					String response = client.resource(testUrl).accept(MediaType.TEXT_PLAIN).get(String.class);
					if ("SUCCESS".equals(response)) {
						baseURL = url;
						break;
					}
				} catch (UniformInterfaceException x) { doNothing(); }

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
			String response = getResource(client, "_test").accept(MediaType.TEXT_PLAIN).get(String.class);
			if (!"SUCCESS".equals(response)) {
				throw new IllegalStateException("Server response invalid: " + response);
			}
		} catch (UniformInterfaceException x) {
			handleUniformInterfaceException(x);
			throw x; // we do not expect null
		} finally {
			releaseClient(client);
		}
	}

	public void testException() {
		Client client = acquireClient();
		try {
			String response = getResource(client, "_test?exception=true").accept(MediaType.TEXT_PLAIN).get(String.class);
			throw new IllegalStateException("Server sent response instead of exception: " + response);
		} catch (UniformInterfaceException x) {
			handleUniformInterfaceException(x);
			throw x; // we do not expect null
		} finally {
			releaseClient(client);
		}
	}

	public RepositoryDTO getRepositoryDTO(String repositoryName) {
		assertNotNull("repositoryName", repositoryName);
		Client client = acquireClient();
		try {
			RepositoryDTO repositoryDTO = getResourceBuilder(client, RepositoryDTO.class, new PathSegment(repositoryName)).get(RepositoryDTO.class);
			return repositoryDTO;
		} catch (UniformInterfaceException x) {
			handleUniformInterfaceException(x);
			throw x; // we do not expect null
		} finally {
			releaseClient(client);
		}
	}

	public ChangeSet getChangeSet(String repositoryName, EntityID toRepositoryID) {
		Client client = acquireClient();
		try {
			ChangeSet changeSet = getResourceBuilder(client, ChangeSet.class,
					new PathSegment(repositoryName), new PathSegment(toRepositoryID)).get(ChangeSet.class);
			return changeSet;
		} catch (UniformInterfaceException x) {
			handleUniformInterfaceException(x);
			throw x; // we do not expect null
		} finally {
			releaseClient(client);
		}
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

	protected WebResource.Builder getResourceBuilder(Client client, Class<?> dtoClass, RelativePathPart ... relativePathParts)
	{
		return getResource(client, dtoClass, relativePathParts).accept(MediaType.APPLICATION_XML_TYPE);
	}

	protected WebResource getResource(Client client, Class<?> dtoClass, RelativePathPart ... relativePathParts)
	{
		StringBuilder relativePath = new StringBuilder();
		relativePath.append('_').append(dtoClass.getSimpleName());
		if (relativePathParts != null && relativePathParts.length > 0) {
			boolean isFirstQueryParam = true;
			for (RelativePathPart relativePathPart : relativePathParts) {
				if (relativePathPart == null)
					continue;

				if (relativePathPart instanceof PathSegment) {
					relativePath.append('/');
				}
				else if (relativePathPart instanceof QueryParameter) {
					if (isFirstQueryParam) {
						isFirstQueryParam = false;
						relativePath.append('?');
					}
					else
						relativePath.append('&');
				}

				relativePath.append(relativePathPart.toString());
			}
		}
		return getResource(client, relativePath.toString());
	}

	protected WebResource getResource(Client client, String relativePath)
	{
		return client.resource(getBaseURL() + relativePath);
	}

	private synchronized Client acquireClient()
	{
		Client client = clientCache.poll();
		if (client == null) {
			ClientConfig clientConfig = new DefaultClientConfig(CloudStoreJaxbContextResolver.class);
			clientConfig.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, Integer.valueOf(TIMEOUT_SOCKET_CONNECT_MS)); // must be a java.lang.Integer
			clientConfig.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, Integer.valueOf(TIMEOUT_SOCKET_READ_MS)); // must be a java.lang.Integer

			SSLContext sslContext = this.sslContext;
			HostnameVerifier hostnameVerifier = this.hostnameVerifier;
			if (sslContext != null) {
				HTTPSProperties httpsProperties = new HTTPSProperties(hostnameVerifier, sslContext); // hostnameVerifier is optional
				clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, httpsProperties);
			}
			else if (hostnameVerifier != null)
				throw new IllegalStateException("sslContext must not be null, if hostnameVerifier is set!");

			client = Client.create(clientConfig);
			configFrozen = true;
		}
		return client;
	}

	private synchronized void releaseClient(Client client)
	{
		clientCache.add(client);
	}

	private void handleUniformInterfaceException(UniformInterfaceException x)
	{
		// Instead of returning null, jersey throws a com.sun.jersey.api.client.UniformInterfaceException
		// when the server does not send a result. We therefore check for the result code 204 here.
		if (ClientResponse.Status.NO_CONTENT == x.getResponse().getClientResponseStatus())
			return;

		logger.error("handleUniformInterfaceException: " + x, x);
		Error error = null;
		try {
			ClientResponse clientResponse = x.getResponse();

			clientResponse.bufferEntity();
			if (clientResponse.hasEntity())
				error = clientResponse.getEntity(Error.class);
		} catch (Exception y) {
			logger.error("handleUniformInterfaceException: " + y, y);
		}

		if (error != null)
			throw new RemoteException(error);

		throw x;
	}

}
