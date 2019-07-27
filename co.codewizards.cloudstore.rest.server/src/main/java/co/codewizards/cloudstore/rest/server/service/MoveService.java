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

@Path("_move/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class MoveService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(MoveService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@POST
	public void move(@QueryParam("to") final String toPath)
	{
		move("", toPath);
	}

	@POST
	@Path("{path:.*}")
	public void move(@PathParam("path") String path, @QueryParam("to") final String toPath)
	{
		requireNonNull(path, "path");
		try (final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();) {
			path = repoTransport.unprefixPath(path);
			repoTransport.move(path, toPath);
		}
	}
}
