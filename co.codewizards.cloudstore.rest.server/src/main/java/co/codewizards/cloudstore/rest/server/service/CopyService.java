package co.codewizards.cloudstore.rest.server.service;

import static java.util.Objects.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

@Path("_copy/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class CopyService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(CopyService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@POST
	public void copy(@QueryParam("to") final String toPath)
	{
		copy("", toPath);
	}

	@POST
	@Path("{path:.*}")
	public void copy(@PathParam("path") String path, @QueryParam("to") final String toPath)
	{
		requireNonNull(path, "path");
		final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();
		try {
			path = repoTransport.unprefixPath(path);
			repoTransport.copy(path, toPath);
		} finally {
			repoTransport.close();
		}
	}
}
