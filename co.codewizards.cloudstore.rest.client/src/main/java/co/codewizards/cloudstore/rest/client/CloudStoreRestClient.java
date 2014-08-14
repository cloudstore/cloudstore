package co.codewizards.cloudstore.rest.client;

import static co.codewizards.cloudstore.core.util.Util.assertNotNull;
import static co.codewizards.cloudstore.core.util.Util.doNothing;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.net.URL;
import java.util.LinkedList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.concurrent.DeferredCompletionException;
import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.util.ExceptionUtil;
import co.codewizards.cloudstore.core.util.StringUtil;
import co.codewizards.cloudstore.rest.client.request.Request;
import co.codewizards.cloudstore.rest.client.ssl.CallbackDeniedTrustException;
import co.codewizards.cloudstore.rest.shared.GZIPReaderInterceptor;
import co.codewizards.cloudstore.rest.shared.GZIPWriterInterceptor;

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

	private static final int DEFAULT_SOCKET_CONNECT_TIMEOUT = 1 * 60 * 1000;
	private static final int DEFAULT_SOCKET_READ_TIMEOUT = 5 * 60 * 1000;

	/**
	 * The {@code key} for the connection timeout used with {@link Config#getPropertyAsInt(String, int)}.
	 * <p>
	 * The configuration can be overridden by a system property - see {@link Config#SYSTEM_PROPERTY_PREFIX}.
	 */
	public static final String CONFIG_KEY_SOCKET_CONNECT_TIMEOUT = "socket.connectTimeout"; //$NON-NLS-1$

	/**
	 * The {@code key} for the read timeout used with {@link Config#getPropertyAsInt(String, int)}.
	 * <p>
	 * The configuration can be overridden by a system property - see {@link Config#SYSTEM_PROPERTY_PREFIX}.
	 */
	public static final String CONFIG_KEY_SOCKET_READ_TIMEOUT = "socket.readTimeout"; //$NON-NLS-1$

	private Integer socketConnectTimeout;

	private Integer socketReadTimeout;

	private final String url;
	private String baseURL;

	private final LinkedList<Client> clientCache = new LinkedList<Client>();

	private boolean configFrozen;

	private HostnameVerifier hostnameVerifier;
	private SSLContext sslContext;

	private CredentialsProvider credentialsProvider;

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
	public CloudStoreRestClient(final URL url) {
		this(assertNotNull("url", url).toExternalForm());
	}

	/**
	 * Create a new client.
	 * @param url any URL to the server. Must not be <code>null</code>.
	 * May be the base-URL, any repository's remote-root-URL or any URL within a remote-root-URL.
	 * The base-URL is automatically determined by cutting sub-paths, step by step.
	 */
	public CloudStoreRestClient(final String url) {
		this.url = assertNotNull("url", url);
	}

	private static String appendFinalSlashIfNeeded(final String url) {
		return url.endsWith("/") ? url : url + "/";
	}

	private void determineBaseURL() {
		acquireClient();
		try {
			final Client client = getClientOrFail();
			String url = appendFinalSlashIfNeeded(this.url);
			while (true) {
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

				if (!url.endsWith("/"))
					throw new IllegalStateException("url does not end with '/'!");

				final int secondLastSlashIndex = url.lastIndexOf('/', url.length() - 2);
				url = url.substring(0, secondLastSlashIndex + 1);

				if (StringUtil.getIndexesOf(url, '/').size() < 3)
					throw new IllegalStateException("baseURL not found!");
			}
		} finally {
			releaseClient();
		}
	}

	public <R> R execute(final Request<R> request) {
		assertNotNull("request", request);
		request.setCloudStoreRESTClient(this);
		int retryCounter = 0;
		final int retryMax = 3;
		while (true) {
			acquireClient();
			try {
				final long start = System.currentTimeMillis();
				logger.info("Execution starting: {}/{}", retryCounter, retryMax);
				try {
					final R result = request.execute();
					logger.info("Execution ended: took {} ms", System.currentTimeMillis() - start);
					return result;
				} catch (final RuntimeException x) {
					markClientBroken(); // make sure we do not reuse this client
					if (++retryCounter <= retryMax && retryExecuteAfterException(x))
						continue;

					handleAndRethrowException(x);
					if (request.isResultNullable())
						return null;
					else
						throw x;
				}
			} finally {
				releaseClient();
				request.setCloudStoreRESTClient(null);
			}
		}
	}

	private boolean retryExecuteAfterException(final Exception x) {
		// If the user explicitly denied trust, we do not retry, because we don't want to ask the user
		// multiple times.
		if (ExceptionUtil.getCause(x, CallbackDeniedTrustException.class) != null)
			return false;

		final Class<?>[] exceptionClassesCausingRetry = new Class<?>[] {
				SSLException.class,
				SocketException.class
		};
		for (final Class<?> exceptionClass : exceptionClassesCausingRetry) {
			@SuppressWarnings("unchecked")
			final Class<? extends Throwable> xc = (Class<? extends Throwable>) exceptionClass;
			if (ExceptionUtil.getCause(x, xc) != null) {
				logger.warn(
						String.format("retryExecuteAfterException: Encountered %s and will retry.", xc.getSimpleName()),
						x);
				return true;
			}
		}
		return false;
	}

	public Invocation.Builder assignCredentials(final Invocation.Builder builder) {
		final CredentialsProvider credentialsProvider = getCredentialsProviderOrFail();
		builder.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, credentialsProvider.getUserName());
		builder.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, credentialsProvider.getPassword());
		return builder;
	}

	public synchronized HostnameVerifier getHostnameVerifier() {
		return hostnameVerifier;
	}
	public synchronized void setHostnameVerifier(final HostnameVerifier hostnameVerifier) {
		if (configFrozen)
			throw new IllegalStateException("Config already frozen! Cannot change hostnameVerifier anymore!");

		this.hostnameVerifier = hostnameVerifier;
	}

	public synchronized SSLContext getSslContext() {
		return sslContext;
	}
	public synchronized void setSslContext(final SSLContext sslContext) {
		if (configFrozen)
			throw new IllegalStateException("Config already frozen! Cannot change sslContext anymore!");

		this.sslContext = sslContext;
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
	private synchronized void acquireClient()
	{
		final ClientRef clientRef = clientThreadLocal.get();
		if (clientRef != null) {
			++clientRef.refCount;
			return;
		}

		Client client = clientCache.poll();
		if (client == null) {
			final SSLContext sslContext = this.sslContext;
			final HostnameVerifier hostnameVerifier = this.hostnameVerifier;

			final ClientConfig clientConfig = new ClientConfig(CloudStoreJaxbContextResolver.class);
			clientConfig.property(ClientProperties.CONNECT_TIMEOUT, getSocketConnectTimeout()); // must be a java.lang.Integer
			clientConfig.property(ClientProperties.READ_TIMEOUT, getSocketReadTimeout()); // must be a java.lang.Integer

			final ClientBuilder clientBuilder = ClientBuilder.newBuilder().withConfig(clientConfig);

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
			final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("anonymous", "");
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
	private void releaseClient() {
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

		} catch (final Exception y) {
			logger.error("handleException: " + x, x);
			logger.error("handleException: " + y, y);
		}

		if (error != null) {
			throwOriginalExceptionIfPossible(error);
			throw new RemoteException(error);
		}

		throw x;
	}

	public void throwOriginalExceptionIfPossible(final Error error) {
		Class<?> clazz;
		try {
			clazz = Class.forName(error.getClassName());
		} catch (final ClassNotFoundException e) {
			return;
		}
		if (!Throwable.class.isAssignableFrom(clazz))
			return;

		@SuppressWarnings("unchecked")
		final Class<? extends Throwable> clasz = (Class<? extends Throwable>) clazz;

		final RemoteException cause = new RemoteException(error);

		Throwable throwable = null;

		// trying XyzException(String message, Throwable cause)
		if (throwable == null)
			throwable = getObjectOrNull(clasz, new Class<?>[] { String.class, Throwable.class }, error.getMessage(), cause);

		// trying XyzException(String message)
		if (throwable == null)
			throwable = getObjectOrNull(clasz, new Class<?>[] { String.class }, error.getMessage());

		// trying XyzException(Throwable cause)
		if (throwable == null)
			throwable = getObjectOrNull(clasz, new Class<?>[] { Throwable.class }, cause);

		// trying XyzException()
		if (throwable == null)
			throwable = getObjectOrNull(clasz, null);

		if (throwable != null) {
			try {
				throwable.initCause(cause);
			} catch (final Exception x) {
				// This happens, if either the cause was already set in an appropriate constructor (see above)
				// or the concrete Throwable does not support it. If we were unable to set the cause we want,
				// we better use a RemoteException and not the original one.
				if (throwable.getCause() != cause)
					return;
			}
			if (throwable instanceof RuntimeException)
				throw (RuntimeException) throwable;

			if (throwable instanceof java.lang.Error)
				throw (java.lang.Error) throwable;

			throw new RuntimeException(throwable);
		}
	}

	private <T> T getObjectOrNull(final Class<T> clazz, Class<?>[] argumentTypes, final Object ... arguments) {
		T result = null;
		if (argumentTypes == null)
			argumentTypes = new Class<?> [0];

		if (argumentTypes.length == 0) {
			try {
				result = clazz.newInstance();
			} catch (final InstantiationException e) {
				return null;
			} catch (final IllegalAccessException e) {
				return null;
			}
		}

		if (result == null) {
			Constructor<T> constructor;
			try {
				constructor = clazz.getConstructor(argumentTypes);
			} catch (final NoSuchMethodException e) {
				return null;
			} catch (final SecurityException e) {
				return null;
			}

			try {
				result = constructor.newInstance(arguments);
			} catch (final InstantiationException e) {
				return null;
			} catch (final IllegalAccessException e) {
				return null;
			} catch (final IllegalArgumentException e) {
				return null;
			} catch (final InvocationTargetException e) {
				return null;
			}
		}

		return result;
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
