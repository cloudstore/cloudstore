package co.codewizards.cloudstore.rest.client;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.concurrent.DeferredCompletionException;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.RemoteException;
import co.codewizards.cloudstore.core.dto.RemoteExceptionUtil;
import co.codewizards.cloudstore.core.util.ExceptionUtil;
import co.codewizards.cloudstore.rest.client.request.Request;
import co.codewizards.cloudstore.rest.client.ssl.CallbackDeniedTrustException;

/**
 * Client for executing REST requests.
 * <p>
 * An instance of this class is used to send data to, query data from or execute logic on the server.
 * <p>
 * If a series of multiple requests is to be sent to the server, it is recommended to keep an instance of
 * this class (because it caches resources) and invoke multiple requests with it.
 * <p>
 * This class is thread-safe.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class CloudStoreRestClient {

	private static final Logger logger = LoggerFactory.getLogger(CloudStoreRestClient.class);

	private final URL url;
	private String baseURL;

	private final LinkedList<Client> clientCache = new LinkedList<Client>();

	private ClientBuilder clientBuilder;

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
	 * @return the base-URL. This URL always ends with "/".
	 */
	public synchronized String getBaseUrl() {
		if (baseURL == null) {
			determineBaseUrl();
		}
		return baseURL;
	}

	/**
	 * Create a new client.
	 * @param url any URL to the server. Must not be <code>null</code>.
	 * May be the base-URL, any repository's remote-root-URL or any URL within a remote-root-URL.
	 * The base-URL is automatically determined by cutting sub-paths, step by step.
	 */
	public CloudStoreRestClient(final URL url, final ClientBuilder clientBuilder) {
		this.url = assertNotNull("url", url);
		this.clientBuilder = assertNotNull("clientBuilder", clientBuilder);
	}

	/**
	 * Create a new client.
	 * @param url any URL to the server. Must not be <code>null</code>.
	 * May be the base-URL, any repository's remote-root-URL or any URL within a remote-root-URL.
	 * The base-URL is automatically determined by cutting sub-paths, step by step.
	 */
	public CloudStoreRestClient(final String url, final ClientBuilder clientBuilder) {
		try{
			this.url = assertNotNull("url", new URL(url));
		} catch (MalformedURLException e){
			throw new IllegalStateException("url is invalid", e);
		}
		this.clientBuilder = assertNotNull("clientBuilder", clientBuilder);
	}

	private void determineBaseUrl() {
		acquireClient();
		try {
			final Client client = getClientOrFail();
			String url = getHostUrl();
			for(String part : getPathParts()){
				if(!part.isEmpty()) // part is always empty in first iteration
					url += part + "/";
				final String testUrl = url + "_test";
				try {
					final String response = client.target(testUrl).request(MediaType.TEXT_PLAIN).get(String.class);
					if ("SUCCESS".equals(response)) {
						baseURL = url;
						break;
					}
				} catch (final WebApplicationException wax) {
					doNothing();
				}
			}

			if (baseURL == null)
				throw new IllegalStateException("baseURL not found!");
		} finally {
			releaseClient();
		}
	}

	private List<String> getPathParts(){
		List<String> pathParts = new ArrayList<String>(Arrays.asList(url.getPath().split("/")));
		if(pathParts.isEmpty()){
			pathParts.add("");
		}
		return pathParts;
	}

	private String getHostUrl(){
		String hostUrl = url.getProtocol() + "://" + url.getHost();
		if(url.getPort() != -1){
			hostUrl += ":" + url.getPort();
		}
		return hostUrl +  "/";
	}

	public <R> R execute(final Request<R> request) {
		assertNotNull("request", request);
		int retryCounter = 0; // *re*-try: first (normal) invocation is 0, first re-try is 1
		final int retryMax = 2; // *re*-try: 2 retries means 3 invocations in total
		while (true) {
			acquireClient();
			try {
				final long start = System.currentTimeMillis();

				if (logger.isDebugEnabled())
					logger.debug("execute: starting try {} of {}", retryCounter + 1, retryMax + 1);

				try {
					request.setCloudStoreRestClient(this);
					final R result = request.execute();

					if (logger.isDebugEnabled())
						logger.debug("execute: invocation took {} ms", System.currentTimeMillis() - start);

					if (result == null && !request.isResultNullable())
						throw new IllegalStateException("result == null, but request.resultNullable == false!");

					return result;
				} catch (final RuntimeException x) {
					markClientBroken(); // make sure we do not reuse this client
					if (++retryCounter > retryMax || !retryExecuteAfterException(x)) {
						logger.warn("execute: invocation failed (will NOT retry): " + x, x);
						handleAndRethrowException(x);
						throw x;
					}
					logger.warn("execute: invocation failed (will retry): " + x, x);

					// Wait a bit before retrying (increasingly longer).
					try { Thread.sleep(retryCounter * 1000L); } catch (Exception y) { doNothing(); }
				}
			} finally {
				releaseClient();
				request.setCloudStoreRestClient(null);
			}
		}
	}

	private boolean retryExecuteAfterException(final Exception x) {
		// If the user explicitly denied trust, we do not retry, because we don't want to ask the user
		// multiple times.
		if (ExceptionUtil.getCause(x, CallbackDeniedTrustException.class) != null)
			return false;

//		final Class<?>[] exceptionClassesCausingRetry = new Class<?>[] {
//				SSLException.class,
//				SocketException.class
//		};
//		for (final Class<?> exceptionClass : exceptionClassesCausingRetry) {
//			@SuppressWarnings("unchecked")
//			final Class<? extends Throwable> xc = (Class<? extends Throwable>) exceptionClass;
//			if (ExceptionUtil.getCause(x, xc) != null) {
//				logger.warn(
//						String.format("retryExecuteAfterException: Encountered %s and will retry.", xc.getSimpleName()),
//						x);
//				return true;
//			}
//		}
//		return false;
		return true;
	}

	public Invocation.Builder assignCredentials(final Invocation.Builder builder) {
		final CredentialsProvider credentialsProvider = getCredentialsProviderOrFail();
		builder.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, credentialsProvider.getUserName());
		builder.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, credentialsProvider.getPassword());
		return builder;
	}

	private final ThreadLocal<ClientRef> clientThreadLocal = new ThreadLocal<ClientRef>();

	private static class ClientRef {
		public final Client client;
		public int refCount = 1;
		public boolean broken;

		public ClientRef(final Client client) {
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
	private synchronized void acquireClient(){
		final ClientRef clientRef = clientThreadLocal.get();
		if (clientRef != null) {
			++clientRef.refCount;
			return;
		}

		Client client = clientCache.poll();
		if (client == null) {
			client = clientBuilder.build();

			// An authentication is always required. Otherwise Jersey throws an exception.
			// Hence, we set it to "anonymous" here and set it to the real values for those
			// requests really requiring it.
			final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("anonymous", "");
			client.register(feature);
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
	public Client getClientOrFail() {
		final ClientRef clientRef = clientThreadLocal.get();
		if (clientRef == null)
			throw new IllegalStateException("acquireClient() not called on the same thread (or releaseClient() already called)!");

		return clientRef.client;
	}

	/**
	 * Release a {@link Client} which was previously {@linkplain #acquireClient() acquired}.
	 * @see #acquireClient()
	 */
	private synchronized void releaseClient() {
		final ClientRef clientRef = clientThreadLocal.get();
		if (clientRef == null)
			throw new IllegalStateException("acquireClient() not called on the same thread (or releaseClient() called more often than acquireClient())!");

		if (--clientRef.refCount == 0) {
			clientThreadLocal.remove();

			if (!clientRef.broken)
				clientCache.add(clientRef.client);
		}
	}

	private void markClientBroken() {
		final ClientRef clientRef = clientThreadLocal.get();
		if (clientRef == null)
			throw new IllegalStateException("acquireClient() not called on the same thread (or releaseClient() called more often than acquireClient())!");

		clientRef.broken = true;
	}

	public void handleAndRethrowException(final RuntimeException x)
	{
		Response response = null;
		if (x instanceof WebApplicationException)
			response = ((WebApplicationException)x).getResponse();
		else if (x instanceof ResponseProcessingException)
			response = ((ResponseProcessingException)x).getResponse();

		if (response == null)
			throw x;

		Error error = null;
		try {
			response.bufferEntity();
			if (response.hasEntity())
				error = response.readEntity(Error.class);

			if (error != null && DeferredCompletionException.class.getName().equals(error.getClassName()))
				logger.debug("handleException: " + x, x);
			else
				logger.error("handleException: " + x, x);

		} catch (final Exception y) {
			logger.error("handleException: " + x, x);
			logger.error("handleException: " + y, y);
		}

		if (error != null) {
			RemoteExceptionUtil.throwOriginalExceptionIfPossible(error);
			throw new RemoteException(error);
		}

		throw x;
	}

	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}
	private CredentialsProvider getCredentialsProviderOrFail() {
		final CredentialsProvider credentialsProvider = getCredentialsProvider();
		if (credentialsProvider == null)
			throw new IllegalStateException("credentialsProvider == null");
		return credentialsProvider;
	}
	public void setCredentialsProvider(final CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}
}
