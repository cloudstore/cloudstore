package co.codewizards.cloudstore.rest.client.command;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import co.codewizards.cloudstore.core.dto.RepositoryDTO;

public class RequestRepoConnection extends VoidCommand {
	private final String repositoryName;
	private final String pathPrefix;
	private final RepositoryDTO clientRepositoryDTO;

	public RequestRepoConnection(final String repositoryName, final String pathPrefix, final RepositoryDTO clientRepositoryDTO) {
		this.repositoryName = assertNotNull("repositoryName", repositoryName);
		this.pathPrefix = pathPrefix;
		this.clientRepositoryDTO = assertNotNull("clientRepositoryDTO", clientRepositoryDTO);
		assertNotNull("clientRepositoryDTO.repositoryId", clientRepositoryDTO.getRepositoryId());
		assertNotNull("clientRepositoryDTO.publicKey", clientRepositoryDTO.getPublicKey());
	}

	@Override
	public Response _execute() {
		return createWebTarget("_requestRepoConnection", urlEncode(repositoryName), pathPrefix)
				.request().post(Entity.entity(clientRepositoryDTO, MediaType.APPLICATION_XML));
	}

}
