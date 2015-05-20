package co.codewizards.cloudstore.ls.rest.client;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.concurrent.DeferredCompletionException;
import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.RemoteException;
import co.codewizards.cloudstore.core.dto.RemoteExceptionUtil;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.util.AssertUtil;
import co.codewizards.cloudstore.ls.core.LocalServerPropertiesManager;
import co.codewizards.cloudstore.ls.core.provider.JavaNativeMessageBodyReader;
import co.codewizards.cloudstore.ls.core.provider.JavaNativeMessageBodyWriter;
import co.codewizards.cloudstore.ls.rest.client.request.Request;

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
public class LocalServerRestClient {

	private static final Logger logger = LoggerFactory.getLogger(LocalServerRestClient.class);

	private static final int DEFAULT_SOCKET_CONNECT_TIMEOUT = 1 * 60 * 1000;
	private static final int DEFAULT_SOCKET_READ_TIMEOUT = 15 * 60 * 1000;

	/**
	 * The {@code key} for the connection timeout used with {@link Config#getPropertyAsInt(String, int)}.
	 * <p>
	 * The configuration can be overridden by a system property - see {@link Config#SYSTEM_PROPERTY_PREFIX}.
	 */
	public static final String CONFIG_KEY_SOCKET_CONNECT_TIMEOUT = "localServer.socket.connectTimeout"; //$NON-NLS-1$

	/**
	 * The {@code key} for the read timeout used with {@link Config#getPropertyAsInt(String, int)}.
	 * <p>
	 * The configuration can be overridden by a system property - see {@link Config#SYSTEM_PROPERTY_PREFIX}.
	 */
	public static final String CONFIG_KEY_SOCKET_READ_TIMEOUT = "localServer.socket.readTimeout"; //$NON-NLS-1$

	private Integer socketConnectTimeout;

	private Integer socketReadTimeout;

	private String baseURL;

	private final LinkedList<Client> clientCache = new LinkedList<Client>();

	private boolean configFrozen;

	private CredentialsProvider credentialsProvider;

	private static final class Holder {
		public static final LocalServerRestClient instance = new LocalServerRestClient();
	}

	public static LocalServerRestClient getInstance() {
		return Holder.instance;
	}

	public synchronized Integer getSocketConnectTimeout() {
		if (socketConnectTimeout == null)
			socketConnectTimeout = Config.getInstance().getPropertyAsPositiveOrZeroInt(
					CONFIG_KEY_SOCKET_CONNECT_TIMEOUT,
					DEFAULT_SOCKET_CONNECT_TIMEOUT);

		return socketConnectTimeout;
	}
	public synchronized void setSocketConnectTimeout(Integer socketConnectTimeout) {
		if (socketConnectTimeout != null && socketConnectTimeout < 0)
			socketConnectTimeout = null;

		this.socketConnectTimeout = socketConnectTimeout;
	}

	public synchronized Integer getSocketReadTimeout() {
		if (socketReadTimeout == null)
			socketReadTimeout = Config.getInstance().getPropertyAsPositiveOrZeroInt(
					CONFIG_KEY_SOCKET_READ_TIMEOUT,
					DEFAULT_SOCKET_READ_TIMEOUT);

		return socketReadTimeout;
	}
	public synchronized void setSocketReadTimeout(Integer socketReadTimeout) {
		if (socketReadTimeout != null && socketReadTimeout < 0)
			socketReadTimeout = null;

		this.socketReadTimeout = socketReadTimeout;
	}

	/**
	 * Get the server's base-URL.
	 * <p>
	 * This base-URL is the base of the <code>LocalServerRest</code> application. Hence all URLs
	 * beneath this base-URL are processed by the <code>LocalServerRest</code> application.
	 * @return the base-URL. This URL always ends with "/".
	 */
	public synchronized String getBaseUrl() {
		if (baseURL == null) {
			final int port = LocalServerPropertiesManager.getInstance().getPort();
			baseURL = "http://127.0.0.1:" + port + '/';
		}
		return baseURL;
	}

	/**
	 * Create a new client.
	 */
	protected LocalServerRestClient() {
		// The clientId is used for memory management in the server: if a client is closed or disappears, i.e. doesn't
		// send keep-alives regularly, anymore, the server removes all objectRef-references kept for this client in its ObjectManager.
		final String clientId = new Uid().toString();

		setCredentialsProvider(new CredentialsProvider() {
			@Override
			public String getUserName() {
				return clientId;
			}
			@Override
			public String getPassword() {
				return LocalServerPropertiesManager.getInstance().getPassword();
			}
		});
	}

//	private static String appendFinalSlashIfNeeded(final String url) {
//		return url.endsWith("/") ? url : url + "/";
//	}

	public <R> R execute(final Request<R> request) {
		AssertUtil.assertNotNull("request", request);
		int retryCounter = 0; // *re*-try: first (normal) invocation is 0, first re-try is 1
		final int retryMax = 2; // *re*-try: 2 retries means 3 invocations in total
		while (true) {
			acquireClient();
			try {
				final long start = System.currentTimeMillis();

				if (logger.isDebugEnabled())
					logger.debug("execute: starting try {} of {}", retryCounter + 1, retryMax + 1);

				try {
					request.setLocalServerRestClient(this);
					final R result = request.execute();

					if (logger.isDebugEnabled())
						logger.debug("execute: invocation took {} ms", System.currentTimeMillis() - start);

					if (result == null && !request.isResultNullable())
						throw new IllegalStateException("result == null, but request.resultNullable == false!");

					return result;
				} catch (final RuntimeException x) {
					final String oldBaseUrl = getBaseUrl();
					baseURL = null;
					if (!oldBaseUrl.equals(getBaseUrl())) {
						retryCounter = 0; // reset to make sure we really try again with the new URL
						clearClientCache();
					}

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
				request.setLocalServerRestClient(null);
			}
		}
	}

	private synchronized void clearClientCache() {
		clientCache.clear();
	}

	private boolean retryExecuteAfterException(final Exception x) {
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
			this.client = AssertUtil.assertNotNull("client", client);
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
		final ClientRef clientRef = clientThreadLocal.get();
		if (clientRef != null) {
			++clientRef.refCount;
			return;
		}

		Client client = clientCache.poll();
		if (client == null) {
			final ClientConfig clientConfig = new ClientConfig(CloudStoreJaxbContextResolver.class);
			clientConfig.property(ClientProperties.CONNECT_TIMEOUT, getSocketConnectTimeout()); // must be a java.lang.Integer
			clientConfig.property(ClientProperties.READ_TIMEOUT, getSocketReadTimeout()); // must be a java.lang.Integer

			final ClientBuilder clientBuilder = ClientBuilder.newBuilder().withConfig(clientConfig);

			clientBuilder.register(JavaNativeMessageBodyReader.class);
			clientBuilder.register(JavaNativeMessageBodyWriter.class);

			for (final Object restComponent : restComponents) {
				if (restComponent instanceof Class<?>)
					clientBuilder.register((Class<?>) restComponent);
				else
					clientBuilder.register(restComponent);
			}

			client = clientBuilder.build();

			// An authentication is always required. Otherwise Jersey throws an exception.
			// Hence, we set it to "anonymous" here and set it to the real values for those
			// requests really requiring it.
			final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("anonymous", "");
			client.register(feature);

			configFrozen = true;
		}
		clientThreadLocal.set(new ClientRef(client));
	}

	private final List<Object> restComponents = new CopyOnWriteArrayList<Object>();

	public void registerRestComponent(Object restComponent) {
		restComponents.add(restComponent);
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
