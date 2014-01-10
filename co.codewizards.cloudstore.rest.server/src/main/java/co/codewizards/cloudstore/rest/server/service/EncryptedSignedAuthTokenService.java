package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;

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
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.persistence.RemoteRepository;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.rest.server.auth.AuthRepoPassword;
import co.codewizards.cloudstore.rest.server.auth.AuthRepoPasswordManager;

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
	@Path("{clientRepositoryID}")
	public EncryptedSignedAuthToken getEncryptedSignedAuthToken(@PathParam("clientRepositoryID") EntityID clientRepositoryID)
	{
		assertNotNull("repositoryName", repositoryName);
		assertNotNull("remoteRepositoryID", clientRepositoryID);
		File localRoot = LocalRepoRegistry.getInstance().getLocalRootForRepositoryNameOrFail(repositoryName);
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
			LocalRepoTransaction transaction = localRepoManager.beginTransaction();
			try {
				RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
				RemoteRepository clientRemoteRepository = remoteRepositoryDAO.getObjectByIdOrFail(clientRepositoryID);
				EncryptedSignedAuthToken result = getEncryptedSignedAuthToken(
						localRepoManager.getLocalRepositoryID(), clientRepositoryID,
						localRepoManager.getPrivateKey(), clientRemoteRepository.getPublicKey());
				transaction.commit();
				return result;
			} finally {
				transaction.rollbackIfActive();
			}
		} finally {
			localRepoManager.close();
		}
	}

	protected EncryptedSignedAuthToken getEncryptedSignedAuthToken(
			EntityID serverRepositoryID, EntityID clientRepositoryID, byte[] localRepoPrivateKey, byte[] remoteRepoPublicKey)
	{
		AuthRepoPassword authRepoPassword = AuthRepoPasswordManager.getInstance().getCurrentAuthRepoPassword(serverRepositoryID, clientRepositoryID);

		AuthToken authToken = authRepoPassword.getAuthToken();
		byte[] authTokenData = new AuthTokenIO().serialise(authToken);
		SignedAuthToken signedAuthToken = new AuthTokenSigner(localRepoPrivateKey).sign(authTokenData);

		byte[] signedAuthTokenData = new SignedAuthTokenIO().serialise(signedAuthToken);
		EncryptedSignedAuthToken encryptedSignedAuthToken =
				new SignedAuthTokenEncrypter(remoteRepoPublicKey).encrypt(signedAuthTokenData);

		return encryptedSignedAuthToken;
	}
}