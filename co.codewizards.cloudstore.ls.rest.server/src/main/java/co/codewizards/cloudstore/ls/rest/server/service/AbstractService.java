package co.codewizards.cloudstore.ls.rest.server.service;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.rest.server.InverseInvoker;

public abstract class AbstractService {
	@Context
	protected SecurityContext securityContext;

	protected Uid getClientId() {
		return new Uid(securityContext.getUserPrincipal().getName());
	}

	protected ObjectManager getObjectManager() {
		return ObjectManager.getInstance(getClientId());
	}

	protected InverseInvoker getInverseInvoker() {
		return InverseInvoker.getInverseInvoker(getObjectManager());
	}
}
