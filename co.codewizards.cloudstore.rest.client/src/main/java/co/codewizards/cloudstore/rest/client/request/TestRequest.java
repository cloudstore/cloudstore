package co.codewizards.cloudstore.rest.client.request;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class TestRequest extends AbstractRequest<Void> {

	private final boolean testException;

	public TestRequest(final boolean testException) {
		this.testException = testException;
	}

	@Override
	public boolean isResultNullable() {
		return true;
	}

	@Override
	public Void execute() {
		if (testException) {
			final Response response = createWebTarget("_test").queryParam("exception", true).request().get();
			assertResponseIndicatesSuccess(response);
			throw new IllegalStateException("Server sent response instead of exception: " + response);
		}
		else {
			final String response = createWebTarget("_test").request(MediaType.TEXT_PLAIN).get(String.class);
			if (!"SUCCESS".equals(response)) {
				throw new IllegalStateException("Server response invalid: " + response);
			}
		}
		return null;
	}

}
