package co.codewizards.cloudstore.rest.server.auth;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class NotAuthorizedException extends WebApplicationException{

	public NotAuthorizedException() {
		super(Response.status(Status.UNAUTHORIZED)
				.header("WWW-Authenticate", "Basic realm=\"CloudStoreServer\"")
				.build());
	}
}
