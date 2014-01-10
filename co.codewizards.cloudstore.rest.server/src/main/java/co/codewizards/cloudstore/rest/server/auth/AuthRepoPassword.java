package co.codewizards.cloudstore.rest.server.auth;

import static co.codewizards.cloudstore.core.util.Util.*;
import co.codewizards.cloudstore.core.auth.AuthToken;
import co.codewizards.cloudstore.core.dto.EntityID;

public class AuthRepoPassword {

	private final EntityID serverRepositoryID;
	private final EntityID clientRepositoryID;
	private final AuthToken authToken;
	private final char[] password;

	protected AuthRepoPassword(EntityID serverRepositoryID, EntityID clientRepositoryID, AuthToken authToken) {
		this.serverRepositoryID = assertNotNull("serverRepositoryID", serverRepositoryID);
		this.clientRepositoryID = assertNotNull("clientRepositoryID", clientRepositoryID);
		this.authToken = assertNotNull("authToken", authToken);
		authToken.makeUnmodifiable();
		assertNotNull("authToken.expiryDateTime", authToken.getExpiryDateTime());
		assertNotNull("authToken.password", authToken.getPassword());
		this.password = authToken.getPassword().toCharArray();
	}

	public EntityID getServerRepositoryID() {
		return serverRepositoryID;
	}
	public EntityID getClientRepositoryID() {
		return clientRepositoryID;
	}
	public AuthToken getAuthToken() {
		return authToken;
	}
	public char[] getPassword() {
		return password;
	}
}
