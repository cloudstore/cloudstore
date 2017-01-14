package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.ConfigPropSetDto;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

@Path("_putParentConfigPropSetDto/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class PutParentConfigPropSetDtoService extends AbstractServiceWithRepoToRepoAuth {

	private static final Logger logger = LoggerFactory.getLogger(PutParentConfigPropSetDtoService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@PUT
	public void beginPutFile(final ConfigPropSetDto parentConfigPropSetDto) {
		assertNotNull(parentConfigPropSetDto, "parentConfigPropSetDto");
		final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();
		try {
			repoTransport.putParentConfigPropSetDto(parentConfigPropSetDto);
		} finally {
			repoTransport.close();
		}
	}
}
