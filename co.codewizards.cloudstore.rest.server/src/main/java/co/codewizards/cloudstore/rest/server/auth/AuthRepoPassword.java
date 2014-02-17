package co.codewizards.cloudstore.rest.server.auth;

import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

import java.util.UUID;

import co.codewizards.cloudstore.core.auth.AuthToken;

public class AuthRepoPassword {

	private final UUID serverRepositoryId;
	private final UUID clientRepositoryId;
	private final AuthToken authToken;
	private final char[] password;

	protected AuthRepoPassword(UUID serverRepositoryId, UUID clientRepositoryId, AuthToken authToken) {
		this.serverRepositoryId = assertNotNull("serverRepositoryId", serverRepositoryId);
		this.clientRepositoryId = assertNotNull("clientRepositoryId", clientRepositoryId);
		this.authToken = assertNotNull("authToken", authToken);
		authToken.makeUnmodifiable();
		assertNotNull("authToken.expiryDateTime", authToken.getExpiryDateTime());
		assertNotNull("authToken.password", authToken.getPassword());
		this.password = authToken.getPassword().toCharArray();
	}

	public UUID getServerRepositoryId() {
		return serverRepositoryId;
	}
	public UUID getClientRepositoryId() {
		return clientRepositoryId;
	}
	public AuthToken getAuthToken() {
		return authToken;
	}
	public char[] getPassword() {
		return password;
	}
}
