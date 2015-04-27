package co.codewizards.cloudstore.ls.rest.client.request;

import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.core.Response;

/**
 * Abstract base class for REST requests never returning a response.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public abstract class VoidRequest extends AbstractRequest<Void> {

	@Override
	public final Void execute() {
		final Response response = _execute();
		assertResponseIndicatesSuccess(response);
		return null;
	}

	/**
	 * REST requests without response should implement this delegate method instead of {@link #execute()}.
	 * @return the response object returned from {@link SyncInvoker#put(javax.ws.rs.client.Entity)} or a
	 * similar method.
	 */
	protected abstract Response _execute();

	@Override
	public boolean isResultNullable() {
		return true;
	}

}
