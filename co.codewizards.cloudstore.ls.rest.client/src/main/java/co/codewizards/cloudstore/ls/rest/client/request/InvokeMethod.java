package co.codewizards.cloudstore.ls.rest.client.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import co.codewizards.cloudstore.ls.core.provider.MediaTypeConst;
import co.codewizards.cloudstore.ls.core.remoteobject.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.remoteobject.MethodInvocationResponse;

public class InvokeMethod extends AbstractRequest<MethodInvocationResponse> {

	private final MethodInvocationRequest methodInvocationRequest;

	public InvokeMethod(MethodInvocationRequest methodInvocationRequest) {
		this.methodInvocationRequest = assertNotNull("methodInvocationRequest", methodInvocationRequest);
	}

	@Override
	public MethodInvocationResponse execute() {
		final WebTarget webTarget = createWebTarget("InvokeMethod");
		final MethodInvocationResponse repoInfoResponseDto = assignCredentials(webTarget.request(MediaTypeConst.APPLICATION_JAVA_NATIVE_TYPE))
				.post(Entity.entity(methodInvocationRequest, MediaTypeConst.APPLICATION_JAVA_NATIVE_TYPE), MethodInvocationResponse.class);
		return repoInfoResponseDto;
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}
}
