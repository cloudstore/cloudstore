package co.codewizards.cloudstore.rest.server.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

@Path("_getClientRepositoryDto/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class GetClientRepositoryDtoService extends AbstractServiceWithRepoToRepoAuth {

	@GET
	public RepositoryDto getClientRepositoryDto()
	{
		try (final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();) {
			RepositoryDto repositoryDto = repoTransport.getClientRepositoryDto();
			return repositoryDto;
		}
	}

}
