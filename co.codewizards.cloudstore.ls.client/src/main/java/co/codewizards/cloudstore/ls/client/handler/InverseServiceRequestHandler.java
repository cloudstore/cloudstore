package co.codewizards.cloudstore.ls.client.handler;

import co.codewizards.cloudstore.ls.client.LocalServerClient;
import co.codewizards.cloudstore.ls.core.dto.InverseServiceRequest;
import co.codewizards.cloudstore.ls.core.dto.InverseServiceResponse;


public interface InverseServiceRequestHandler<Q extends InverseServiceRequest, A extends InverseServiceResponse> {

	/**
	 * Gets the priority of this handler.
	 * <p>
	 * The greatest number wins, if there are multiple handlers for the same class.
	 * @return the priority of this handler.
	 */
	int getPriority();

	/**
	 * Gets the class or interface to be handled. Sub-classes are handled, too! If undesired, use the {@link #getPriority() priority}.
	 * @return the class or interface to be handled. Must not be <code>null</code>.
	 */
	Class<? super Q> getInverseServiceRequestType();

	LocalServerClient getLocalServerClient();

	void setLocalServerClient(LocalServerClient localServerClient);

	A handle(Q request);
}
