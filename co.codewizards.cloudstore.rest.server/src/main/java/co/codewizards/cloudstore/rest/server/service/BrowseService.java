package co.codewizards.cloudstore.rest.server.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("{repositoryID}/browse")
public class BrowseService {


	@GET
	@Produces(MediaType.WILDCARD)
	public Object getContents() {
		return getContents("/");
	}

	@GET
	@Path("{path}")
	@Produces(MediaType.WILDCARD)
	public Object getContents(@PathParam("path") String path) {

		return path;
	}

}
