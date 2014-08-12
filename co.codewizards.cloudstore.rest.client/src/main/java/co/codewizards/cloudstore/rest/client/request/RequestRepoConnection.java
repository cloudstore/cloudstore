package co.codewizards.cloudstore.rest.client.request;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import co.codewizards.cloudstore.core.dto.RepositoryDto;

public class RequestRepoConnection extends VoidRequest {
	private final String repositoryName;
	private final String pathPrefix;
	private final RepositoryDto clientRepositoryDto;

	public RequestRepoConnection(final String repositoryName, final String pathPrefix, final RepositoryDto clientRepositoryDto) {
		this.repositoryName = assertNotNull("repositoryName", repositoryName);
		this.pathPrefix = pathPrefix;
		this.clientRepositoryDto = assertNotNull("clientRepositoryDto", clientRepositoryDto);
		assertNotNull("clientRepositoryDto.repositoryId", clientRepositoryDto.getRepositoryId());
		assertNotNull("clientRepositoryDto.publicKey", clientRepositoryDto.getPublicKey());
	}

	@Override
	public Response _execute() {
		return createWebTarget("_requestRepoConnection", urlEncode(repositoryName), urlEncode(pathPrefix))
				.request().post(Entity.entity(clientRepositoryDto, MediaType.APPLICATION_XML));
	}

}
