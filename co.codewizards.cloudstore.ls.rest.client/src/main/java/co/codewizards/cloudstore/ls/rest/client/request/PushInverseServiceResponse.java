package co.codewizards.cloudstore.ls.rest.client.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import co.codewizards.cloudstore.ls.core.dto.InverseServiceResponse;
import co.codewizards.cloudstore.ls.core.provider.MediaTypeConst;

public class PushInverseServiceResponse extends VoidRequest {

	private final InverseServiceResponse response;

	public PushInverseServiceResponse(final InverseServiceResponse response) {
		this.response = assertNotNull("response", response);
	}

	@Override
	protected Response _execute() {
		final WebTarget webTarget = createWebTarget(getPath(InverseServiceResponse.class));
		final Response r = assignCredentials(webTarget.request(MediaTypeConst.APPLICATION_JAVA_NATIVE_TYPE))
				.post(Entity.entity(response, MediaTypeConst.APPLICATION_JAVA_NATIVE_TYPE));
		return r;
	}
}
