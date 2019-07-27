package co.codewizards.cloudstore.ls.client;

import static java.util.Objects.*;

import javax.ws.rs.core.SecurityContext;

import co.codewizards.cloudstore.ls.core.invoke.ObjectRefConverter;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRefConverterFactory;

class ObjectRefConverterFactoryImpl implements ObjectRefConverterFactory {

	private final LocalServerClient localServerClient;

	public ObjectRefConverterFactoryImpl(final LocalServerClient localServerClient) {
		this.localServerClient = requireNonNull(localServerClient, "localServerClient");
	}

	@Override
	public ObjectRefConverter createObjectRefConverter(final SecurityContext securityContext) {
		return new ObjectRefConverterImpl(localServerClient);
	}
}
