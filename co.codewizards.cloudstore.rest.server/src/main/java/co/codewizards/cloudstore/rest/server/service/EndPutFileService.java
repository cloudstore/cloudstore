package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.DateTime;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

@Path("_endPutFile/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class EndPutFileService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(EndPutFileService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@POST
	@Path("{path:.*}")
	public void endPutFile(
			@PathParam("path") String path,
			@QueryParam("lastModified") DateTime lastModified,
			@QueryParam("length") long length)
	{
		assertNotNull("path", path);
		assertNotNull("lastModified", lastModified);
		RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();
		try {
			path = repoTransport.unprefixPath(path);
			repoTransport.endPutFile(path, lastModified.toDate(), length);
		} finally {
			repoTransport.close();
		}
	}
}
