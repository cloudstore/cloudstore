package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

@Path("_beginPutFile/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class BeginPutFileService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(BeginPutFileService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@PUT
	@Path("{path:.*}")
	public void beginPutFile(@PathParam("path") String path)
	{
		assertNotNull(path, "path");
		final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();
		try {
			path = repoTransport.unprefixPath(path);
			repoTransport.beginPutFile(path);
		} finally {
			repoTransport.close();
		}
	}
}
