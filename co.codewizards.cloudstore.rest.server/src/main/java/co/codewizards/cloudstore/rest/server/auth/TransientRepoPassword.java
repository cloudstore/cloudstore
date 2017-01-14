package co.codewizards.cloudstore.rest.server.auth;

import java.util.UUID;

import co.codewizards.cloudstore.core.auth.AuthToken;
import co.codewizards.cloudstore.core.util.AssertUtil;

public class TransientRepoPassword {

	private final UUID serverRepositoryId;
	private final UUID clientRepositoryId;
	private final AuthToken authToken;
	private final char[] password;

	protected TransientRepoPassword(final UUID serverRepositoryId, final UUID clientRepositoryId, final AuthToken authToken) {
		this.serverRepositoryId = AssertUtil.assertNotNull(serverRepositoryId, "serverRepositoryId");
		this.clientRepositoryId = AssertUtil.assertNotNull(clientRepositoryId, "clientRepositoryId");
		this.authToken = AssertUtil.assertNotNull(authToken, "authToken");
		authToken.makeUnmodifiable();
		AssertUtil.assertNotNull(authToken.getExpiryDateTime(), "authToken.expiryDateTime");
		AssertUtil.assertNotNull(authToken.getPassword(), "authToken.password");
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
