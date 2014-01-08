package co.codewizards.cloudstore.rest.server.service;

import java.net.URL;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;

@Path("_endSyncFromRepository/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class EndSyncFromRepositoryService
{
	private static final Logger logger = LoggerFactory.getLogger(EndSyncFromRepositoryService.class);

	{
		logger.debug("<init>: created new instance");
	}

	private @PathParam("repositoryName") String repositoryName;

	@PUT
	@Path("{path:.*}")
	public void endSyncFromRepository(@PathParam("fromRepositoryID") EntityID fromRepositoryID)
	{
		URL localRootURL = LocalRepoRegistry.getInstance().getLocalRootURLForRepositoryNameOrFail(repositoryName);
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(localRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(localRootURL);
		try {
			repoTransport.endSyncFromRepository(fromRepositoryID);
		} finally {
			repoTransport.close();
		}
	}
}
