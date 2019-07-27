package co.codewizards.cloudstore.ls.rest.server;

import static java.util.Objects.*;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRefConverter;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRefConverterFactory;

class ObjectRefConverterFactoryImpl implements ObjectRefConverterFactory {

	@Override
	public ObjectRefConverter createObjectRefConverter(final SecurityContext securityContext) {
		final Principal userPrincipal = requireNonNull(securityContext, "securityContext").getUserPrincipal();
		requireNonNull(userPrincipal, "securityContext.userPrincipal");

		final Uid clientId = new Uid(securityContext.getUserPrincipal().getName());
		final ObjectManager objectManager = ObjectManager.getInstance(clientId);

		return new ObjectRefConverterImpl(objectManager);
	}
}
