package co.codewizards.cloudstore.rest.client;

import java.util.LinkedList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Error;
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

	private String baseURL;

	private LinkedList<Client> clientCache = new LinkedList<Client>();

	private boolean configFrozen;
	private HostnameVerifier hostnameVerifier;
	private SSLContext sslContext;

	public CloudStoreRESTClient(String protocol, String host, int port)
	{
		this(protocol, host, "co.codewizards.cloudstore.webapp", port);
	}

	private CloudStoreRESTClient(String protocol, String host, String webAppName, int port)
	{
		this.baseURL = protocol + "://" + host + ":" + port + '/';

		if (webAppName != null && !webAppName.isEmpty())
			this.baseURL += webAppName + '/';

//		this.baseURL += "CloudStoreREST/"; // Using the root of the web-app directly (no suffix).
	}

	public void testSuccess() {
		Client client = acquireClient();
		try {
			String response = getResource(client, "test").accept(MediaType.TEXT_PLAIN).get(String.class);
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
			String response = getResource(client, "test?exception=true").accept(MediaType.TEXT_PLAIN).get(String.class);
			throw new IllegalStateException("Server sent response instead of exception: " + response);
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
		relativePath.append(dtoClass.getSimpleName());
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
		return client.resource(baseURL + relativePath);
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
