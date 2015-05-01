package co.codewizards.cloudstore.ls.client.handler;

import java.lang.reflect.ParameterizedType;

import co.codewizards.cloudstore.ls.client.LocalServerClient;
import co.codewizards.cloudstore.ls.core.dto.InverseServiceRequest;
import co.codewizards.cloudstore.ls.core.dto.InverseServiceResponse;

public abstract class AbstractInverseServiceRequestHandler<Q extends InverseServiceRequest, A extends InverseServiceResponse> implements InverseServiceRequestHandler<Q, A> {

	private final Class<Q> requestClass;
	{
		// TODO implement proper resolution as the actual type arguments can be different ones through a large inheritance hierarchy!
		// ...put this code then into the ReflectionUtil class.
		@SuppressWarnings("unchecked")
		Class<Q> c = (Class<Q>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		requestClass = c;
	}

	private LocalServerClient localServerClient;

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public Class<? super Q> getInverseServiceRequestType() {
		return requestClass;
	}

	@Override
	public LocalServerClient getLocalServerClient() {
		return localServerClient;
	}
	@Override
	public void setLocalServerClient(LocalServerClient localServerClient) {
		this.localServerClient = localServerClient;
	}
}
