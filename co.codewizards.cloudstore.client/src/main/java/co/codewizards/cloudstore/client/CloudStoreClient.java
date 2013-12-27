package co.codewizards.cloudstore.client;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class CloudStoreClient
{
	private static final Logger logger = LoggerFactory.getLogger(CloudStoreClient.class);

	private static final int TIMEOUT_SOCKET_CONNECT_MS = 10 * 1000; // TODO make timeout configurable
	private static final int TIMEOUT_SOCKET_READ_MS = 90 * 1000; // TODO make timeout configurable

	private String baseURL;

	private LinkedList<Client> clientCache = new LinkedList<Client>();

	public CloudStoreClient()
	{
		this("http", "cloudstore.codewizards.com", 80); // TODO switch to HTTPS (and port 443)!!!
	}

	public CloudStoreClient(String protocol, String host, int port)
	{
		this(protocol, host, "co.codewizards.cloudstore.webapp", port);
	}

	private CloudStoreClient(String protocol, String host, String webAppName, int port)
	{
		this.baseURL = protocol + "://" + host + ":" + port + '/';

		if (webAppName != null && !webAppName.isEmpty())
			this.baseURL += webAppName + '/';

		this.baseURL += "CloudStoreREST/";
	}


//	public CloudStoreValidationResponse validate(CloudStoreValidationRequest request) throws RemoteException
//	{
//		logger.trace("validate: request={}", request);
//
//		if (request == null)
//			throw new IllegalArgumentException("request == null");
//
//		if (request.getProductID() == null)
//			throw new IllegalArgumentException("request.productID == null");
//
//		if (request.getEmail() == null)
//			throw new IllegalArgumentException("request.email == null");
//
//		if (request.getCloudStoreKey() == null)
//			throw new IllegalArgumentException("request.licenceKey == null");
//
//		Client client = acquireClient();
//		try {
//			Builder builder = getChildVMAppResource(client, "validate").type(MediaType.APPLICATION_XML_TYPE).accept(MediaType.APPLICATION_XML_TYPE);
//			CloudStoreValidationResponse response = builder.post(CloudStoreValidationResponse.class, request);
//			return response;
//		} catch (UniformInterfaceException x) {
//			handleUniformInterfaceException(x);
//			throw x; // we do not expect null
//		} finally {
//			releaseClient(client);
//		}
//	}
//
//
//	protected WebResource.Builder getChildVMAppResourceBuilder(Client client, Class<?> dtoClass, RelativePathPart ... relativePathParts)
//	{
//		return getChildVMAppResource(client, dtoClass, relativePathParts).accept(MediaType.APPLICATION_XML_TYPE);
//	}
//
//	protected WebResource getChildVMAppResource(Client client, Class<?> dtoClass, RelativePathPart ... relativePathParts)
//	{
//		StringBuilder relativePath = new StringBuilder();
//		relativePath.append(dtoClass.getSimpleName());
//		if (relativePathParts != null && relativePathParts.length > 0) {
//			boolean isFirstQueryParam = true;
//			for (RelativePathPart relativePathPart : relativePathParts) {
//				if (relativePathPart == null)
//					continue;
//
//				if (relativePathPart instanceof PathSegment) {
//					relativePath.append('/');
//				}
//				else if (relativePathPart instanceof QueryParameter) {
//					if (isFirstQueryParam) {
//						isFirstQueryParam = false;
//						relativePath.append('?');
//					}
//					else
//						relativePath.append('&');
//				}
//
//				relativePath.append(relativePathPart.toString());
//			}
//		}
//		return getChildVMAppResource(client, relativePath.toString());
//	}

	protected WebResource getChildVMAppResource(Client client, String relativePath)
	{
		return client.resource(baseURL + relativePath);
	}

	private synchronized Client acquireClient()
	{
		Client client = clientCache.poll();
		if (client == null) {
//			ClientConfig clientConfig = new DefaultClientConfig(JavaNativeMessageBodyReader.class, JavaNativeMessageBodyWriter.class);
			ClientConfig clientConfig = new DefaultClientConfig();
			clientConfig.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, Integer.valueOf(TIMEOUT_SOCKET_CONNECT_MS)); // must be a java.lang.Integer
			clientConfig.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, Integer.valueOf(TIMEOUT_SOCKET_READ_MS)); // must be a java.lang.Integer
			client = Client.create(clientConfig);
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
		co.codewizards.cloudstore.shared.dto.Error error = null;
		try {
			ClientResponse clientResponse = x.getResponse();

			clientResponse.bufferEntity();
			if (clientResponse.hasEntity())
				error = clientResponse.getEntity(co.codewizards.cloudstore.shared.dto.Error.class);
		} catch (Exception y) {
			logger.error("handleUniformInterfaceException: " + y, y);
		}

//		if (error != null)
//			throw new RemoteException(error);

		throw x;
	}
}
