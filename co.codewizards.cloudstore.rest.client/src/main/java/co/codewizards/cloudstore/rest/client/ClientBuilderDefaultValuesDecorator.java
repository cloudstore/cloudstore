package co.codewizards.cloudstore.rest.client;

import java.security.KeyStore;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.rest.client.ssl.HostnameVerifierAllowingAll;
import co.codewizards.cloudstore.rest.shared.GZIPReaderInterceptor;
import co.codewizards.cloudstore.rest.shared.GZIPWriterInterceptor;

public class ClientBuilderDefaultValuesDecorator extends ClientBuilder{
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
	
	private final ClientBuilder builder;
	
	public ClientBuilderDefaultValuesDecorator(){
		this(ClientBuilder.newBuilder());
	}
	
	public ClientBuilderDefaultValuesDecorator(ClientBuilder builder){
		this.builder = builder;
		
		final ClientConfig clientConfig = new ClientConfig(CloudStoreJaxbContextResolver.class);
		final Integer socketReadTimeout = Config.getInstance().getPropertyAsPositiveOrZeroInt(
				CONFIG_KEY_SOCKET_READ_TIMEOUT,
				DEFAULT_SOCKET_READ_TIMEOUT);
		
		final Integer socketConnectTimeout = Config.getInstance().getPropertyAsPositiveOrZeroInt(
				CONFIG_KEY_SOCKET_CONNECT_TIMEOUT,
				DEFAULT_SOCKET_CONNECT_TIMEOUT);

		clientConfig.property(ClientProperties.CONNECT_TIMEOUT, socketConnectTimeout); // must be a java.lang.Integer
		clientConfig.property(ClientProperties.READ_TIMEOUT, socketReadTimeout); // must be a java.lang.Integer

		this.builder.withConfig(clientConfig)
			.register(GZIPReaderInterceptor.class)
			.register(GZIPWriterInterceptor.class)
			.hostnameVerifier(new HostnameVerifierAllowingAll());
	}
	
	@Override
	public Client build(){
		return builder.build();
	}
	
	public ClientBuilderDefaultValuesDecorator sslContext(final SSLContext sslContext){
		builder.sslContext(sslContext);
		return this;
	}
	
	public ClientBuilderDefaultValuesDecorator hostnameVerifier(final HostnameVerifier hostnameVerifier){
		builder.hostnameVerifier(hostnameVerifier);
		return this;
	}

	@Override
	public Configuration getConfiguration() {
		return builder.getConfiguration();
	}

	@Override
	public ClientBuilder property(String name, Object value) {
		builder.property(name, value);
		return this;
	}

	@Override
	public ClientBuilder register(Class<?> componentClass) {
		builder.register(componentClass);
		return this;
	}

	@Override
	public ClientBuilder register(Class<?> componentClass, int priority) {
		builder.register(componentClass, priority);
		return this;
	}

	@Override
	public ClientBuilder register(Class<?> componentClass, Class<?>... contracts) {
		builder.register(componentClass, contracts);
		return this;
	}

	@Override
	public ClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
		builder.register(componentClass, contracts);
		return this;
	}

	@Override
	public ClientBuilder register(Object component) {
		builder.register(component);
		return this;
	}

	@Override
	public ClientBuilder register(Object component, int priority) {
		builder.register(component, priority);
		return this;
	}

	@Override
	public ClientBuilder register(Object component, Class<?>... contracts) {
		builder.register(component, contracts);
		return this;
	}

	@Override
	public ClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {
		builder.register(component, contracts);
		return this;
	}

	@Override
	public ClientBuilder withConfig(Configuration config) {
		builder.withConfig(config);
		return this;
	}

	@Override
	public ClientBuilder keyStore(KeyStore keyStore, char[] password) {
		builder.keyStore(keyStore, password);
		return this;
	}

	@Override
	public ClientBuilder trustStore(KeyStore trustStore) {
		builder.trustStore(trustStore);
		return this;
	}

}
