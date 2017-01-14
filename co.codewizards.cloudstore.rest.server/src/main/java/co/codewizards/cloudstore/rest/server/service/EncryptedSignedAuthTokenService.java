package co.codewizards.cloudstore.rest.server.service;

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
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistryImpl;
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
	public EncryptedSignedAuthToken getEncryptedSignedAuthToken(@PathParam("clientRepositoryId") final UUID clientRepositoryId)
	{
		AssertUtil.assertNotNull(repositoryName, "repositoryName");
		AssertUtil.assertNotNull(clientRepositoryId, "clientRepositoryId");
		final File localRoot = LocalRepoRegistryImpl.getInstance().getLocalRootForRepositoryNameOrFail(repositoryName);
		final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
			final EncryptedSignedAuthToken result = getEncryptedSignedAuthToken(
					localRepoManager.getRepositoryId(), clientRepositoryId,
					localRepoManager.getPrivateKey(), localRepoManager.getRemoteRepositoryPublicKeyOrFail(clientRepositoryId));
			return result;
		} finally {
			localRepoManager.close();
		}
	}

	protected EncryptedSignedAuthToken getEncryptedSignedAuthToken(
			final UUID serverRepositoryId, final UUID clientRepositoryId, final byte[] localRepoPrivateKey, final byte[] remoteRepoPublicKey)
	{
		final TransientRepoPassword transientRepoPassword = TransientRepoPasswordManager.getInstance().getCurrentAuthRepoPassword(serverRepositoryId, clientRepositoryId);

		final AuthToken authToken = transientRepoPassword.getAuthToken();
		final byte[] authTokenData = new AuthTokenIO().serialise(authToken);
		final SignedAuthToken signedAuthToken = new AuthTokenSigner(localRepoPrivateKey).sign(authTokenData);

		final byte[] signedAuthTokenData = new SignedAuthTokenIO().serialise(signedAuthToken);
		final EncryptedSignedAuthToken encryptedSignedAuthToken =
				new SignedAuthTokenEncrypter(remoteRepoPublicKey).encrypt(signedAuthTokenData);

		return encryptedSignedAuthToken;
	}
}