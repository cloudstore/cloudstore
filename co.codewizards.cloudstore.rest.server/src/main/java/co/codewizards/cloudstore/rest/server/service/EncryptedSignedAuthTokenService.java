package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;





import co.codewizards.cloudstore.core.auth.AuthToken;
import co.codewizards.cloudstore.core.auth.AuthTokenIO;
import co.codewizards.cloudstore.core.auth.AuthTokenSigner;
import co.codewizards.cloudstore.core.auth.EncryptedSignedAuthToken;
import co.codewizards.cloudstore.core.auth.SignedAuthToken;
import co.codewizards.cloudstore.core.auth.SignedAuthTokenEncrypter;
import co.codewizards.cloudstore.core.auth.SignedAuthTokenIO;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.util.AssertUtil;
import co.codewizards.cloudstore.rest.server.auth.TransientRepoPassword;
import co.codewizards.cloudstore.rest.server.auth.TransientRepoPasswordManager;

@Path("_EncryptedSignedAuthToken/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class EncryptedSignedAuthTokenService
{
	private static final Logger logger = LoggerFactory.getLogger(EncryptedSignedAuthTokenService.class);

	{
		logger.debug("<init>: created new instance");
	}

	private @PathParam("repositoryName") String repositoryName;

	@GET
	@Path("{clientRepositoryId}")
	public EncryptedSignedAuthToken getEncryptedSignedAuthToken(@PathParam("clientRepositoryId") UUID clientRepositoryId)
	{
		AssertUtil.assertNotNull("repositoryName", repositoryName);
		AssertUtil.assertNotNull("clientRepositoryId", clientRepositoryId);
		File localRoot = LocalRepoRegistry.getInstance().getLocalRootForRepositoryNameOrFail(repositoryName);
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
			EncryptedSignedAuthToken result = getEncryptedSignedAuthToken(
					localRepoManager.getRepositoryId(), clientRepositoryId,
					localRepoManager.getPrivateKey(), localRepoManager.getRemoteRepositoryPublicKeyOrFail(clientRepositoryId));
			return result;
		} finally {
			localRepoManager.close();
		}
	}

	protected EncryptedSignedAuthToken getEncryptedSignedAuthToken(
			UUID serverRepositoryId, UUID clientRepositoryId, byte[] localRepoPrivateKey, byte[] remoteRepoPublicKey)
	{
		TransientRepoPassword transientRepoPassword = TransientRepoPasswordManager.getInstance().getCurrentAuthRepoPassword(serverRepositoryId, clientRepositoryId);

		AuthToken authToken = transientRepoPassword.getAuthToken();
		byte[] authTokenData = new AuthTokenIO().serialise(authToken);
		SignedAuthToken signedAuthToken = new AuthTokenSigner(localRepoPrivateKey).sign(authTokenData);

		byte[] signedAuthTokenData = new SignedAuthTokenIO().serialise(signedAuthToken);
		EncryptedSignedAuthToken encryptedSignedAuthToken =
				new SignedAuthTokenEncrypter(remoteRepoPublicKey).encrypt(signedAuthTokenData);

		return encryptedSignedAuthToken;
	}
}