package co.codewizards.cloudstore.ls.rest.client.request;

import javax.ws.rs.client.WebTarget;

import co.codewizards.cloudstore.ls.core.dto.InverseServiceRequest;
import co.codewizards.cloudstore.ls.core.provider.MediaTypeConst;

public class PollInverseServiceRequest extends AbstractRequest<InverseServiceRequest> {

	@Override
	public InverseServiceRequest execute() {
		final WebTarget webTarget = createWebTarget(getPath(InverseServiceRequest.class));
		final InverseServiceRequest inverseServiceRequest = assignCredentials(webTarget.request(MediaTypeConst.APPLICATION_JAVA_NATIVE_WITH_OBJECT_REF_TYPE))
				.post(null, InverseServiceRequest.class);
		return inverseServiceRequest;
	}

	@Override
	public boolean isResultNullable() {
		return true;
	}
}
