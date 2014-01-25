package co.codewizards.cloudstore.rest.server.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

@Path("_endSyncFromRepository/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class EndSyncFromRepositoryService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(EndSyncFromRepositoryService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@POST
	public void endSyncFromRepository()
	{
		RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();
		try {
			repoTransport.endSyncFromRepository();
		} finally {
			repoTransport.close();
		}
	}
}
