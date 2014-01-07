package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;

@Path("{repositoryName:[^_/][^/]*}")
public class DeleteService {
	private static final Logger logger = LoggerFactory.getLogger(DeleteService.class);


	{
		logger.debug("<init>: created new instance");
	}

	private @PathParam("repositoryName") String repositoryName;

	@DELETE
	@Path("{path:.*}")
	@Produces(MediaType.WILDCARD)
	public Response delete(@Context UriInfo uriInfo, @PathParam("path") String path) {
		if (!assertNotNull("path", path).startsWith("/"))
			path = "/" + path;

		LocalRepoRegistry localRepoRegistry = LocalRepoRegistry.getInstance();
		File localRoot = localRepoRegistry.getLocalRootForRepositoryName(repositoryName);
		if (localRoot.exists()) {
			localRoot.delete();
		}
		Response response = Response.created(uriInfo.getAbsolutePath()).build();
		return response;
	}
}
