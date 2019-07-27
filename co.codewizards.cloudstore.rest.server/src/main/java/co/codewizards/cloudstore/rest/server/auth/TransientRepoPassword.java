package co.codewizards.cloudstore.rest.server.auth;

import static java.util.Objects.*;

import java.util.UUID;

import co.codewizards.cloudstore.core.auth.AuthToken;

public class TransientRepoPassword {

	private final UUID serverRepositoryId;
	private final UUID clientRepositoryId;
	private final AuthToken authToken;
	private final char[] password;

	protected TransientRepoPassword(final UUID serverRepositoryId, final UUID clientRepositoryId, final AuthToken authToken) {
		this.serverRepositoryId = requireNonNull(serverRepositoryId, "serverRepositoryId");
		this.clientRepositoryId = requireNonNull(clientRepositoryId, "clientRepositoryId");
		this.authToken = requireNonNull(authToken, "authToken");
		authToken.makeUnmodifiable();
		requireNonNull(authToken.getExpiryDateTime(), "authToken.expiryDateTime");
		requireNonNull(authToken.getPassword(), "authToken.password");
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
