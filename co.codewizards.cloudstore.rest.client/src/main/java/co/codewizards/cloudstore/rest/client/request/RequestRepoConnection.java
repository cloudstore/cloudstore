package co.codewizards.cloudstore.rest.client.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import co.codewizards.cloudstore.core.dto.RepositoryDto;

public class RequestRepoConnection extends VoidRequest {
	private final String repositoryName;
	private final String pathPrefix;
	private final RepositoryDto clientRepositoryDto;

	public RequestRepoConnection(final String repositoryName, final String pathPrefix, final RepositoryDto clientRepositoryDto) {
		this.repositoryName = assertNotNull(repositoryName, "repositoryName");
		this.pathPrefix = pathPrefix;
		this.clientRepositoryDto = assertNotNull(clientRepositoryDto, "clientRepositoryDto");
		assertNotNull(clientRepositoryDto.getRepositoryId(), "clientRepositoryDto.repositoryId");
		assertNotNull(clientRepositoryDto.getPublicKey(), "clientRepositoryDto.publicKey");
	}

	@Override
	public Response _execute() {
		return createWebTarget("_requestRepoConnection", urlEncode(repositoryName), urlEncode(pathPrefix))
				.request().post(Entity.entity(clientRepositoryDto, MediaType.APPLICATION_XML));
	}

}
