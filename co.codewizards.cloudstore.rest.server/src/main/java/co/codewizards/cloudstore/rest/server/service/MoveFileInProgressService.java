package co.codewizards.cloudstore.rest.server.service;

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
import co.codewizards.cloudstore.core.util.AssertUtil;

@Path("_moveFileInProgress/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class MoveFileInProgressService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(MoveFileInProgressService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@POST
	public void moveFileInProgress(@QueryParam("to") final String toPath)
	{
		moveFileInProgress("", toPath);
	}

	@POST
	@Path("{path:.*}")
	public void moveFileInProgress(@PathParam("path") String path, @QueryParam("to") final String toPath)
	{
		AssertUtil.assertNotNull("path", path);
		final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();
		try {
			path = repoTransport.unprefixPath(path);
			repoTransport.moveFileInProgressToRepo(path, toPath);
		} finally {
			repoTransport.close();
		}
	}
}
