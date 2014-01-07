package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("{repositoryName:[^_/][^/]*}")
public class BrowseService {
	private static final Logger logger = LoggerFactory.getLogger(BrowseService.class);

	{
		logger.debug("<init>: created new instance");
	}

	private @PathParam("repositoryName") String repositoryName;

	@GET
	@Produces(MediaType.WILDCARD)
	public Object getContents() {
		return getContents("");
	}

	@GET
	@Path("{path:.*}")
	@Produces(MediaType.WILDCARD)
	public Object getContents(@PathParam("path") String path) {
		if (!assertNotNull("path", path).startsWith("/"))
			path = "/" + path;

		return String.format("RepositoryName: '%s'<br/>Path: '%s'", repositoryName, path);
	}

}
