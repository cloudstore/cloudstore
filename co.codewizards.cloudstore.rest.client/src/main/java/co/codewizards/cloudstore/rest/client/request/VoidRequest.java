package co.codewizards.cloudstore.rest.client.request;

import javax.ws.rs.core.Response;

public abstract class VoidRequest extends AbstractRequest<Void> {

	@Override
	public final Void execute() {
		final Response response = _execute();
		assertResponseIndicatesSuccess(response);
		return null;
	}

	protected abstract Response _execute();

	@Override
	public boolean isResultNullable() {
		return true;
	}

}
