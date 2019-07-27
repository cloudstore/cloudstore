package co.codewizards.cloudstore.rest.client.request;

import static java.util.Objects.*;

import java.util.UUID;

import javax.ws.rs.core.MediaType;

import co.codewizards.cloudstore.core.auth.EncryptedSignedAuthToken;

public class GetEncryptedSignedAuthToken extends AbstractRequest<EncryptedSignedAuthToken> {

	private final String repositoryName;
	private final UUID clientRepositoryId;

	public GetEncryptedSignedAuthToken(final String repositoryName, final UUID clientRepositoryId) {
		this.repositoryName = requireNonNull(repositoryName, "repositoryName");
		this.clientRepositoryId = requireNonNull(clientRepositoryId, "clientRepositoryId");
	}

	@Override
	public EncryptedSignedAuthToken execute() {
		final EncryptedSignedAuthToken encryptedSignedAuthToken = createWebTarget(
				getPath(EncryptedSignedAuthToken.class), urlEncode(repositoryName), clientRepositoryId.toString())
				.request(MediaType.APPLICATION_XML).get(EncryptedSignedAuthToken.class);
		return encryptedSignedAuthToken;
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}
}
