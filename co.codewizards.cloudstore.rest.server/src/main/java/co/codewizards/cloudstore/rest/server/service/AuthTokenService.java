package co.codewizards.cloudstore.rest.server.service;

import java.io.File;
import java.security.SecureRandom;
import java.util.Date;

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
import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.persistence.RemoteRepository;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

@Path("_getAuthToken/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class AuthTokenService
{
	private static final Logger logger = LoggerFactory.getLogger(AuthTokenService.class);

	{
		logger.debug("<init>: created new instance");
	}

	private @PathParam("repositoryName") String repositoryName;

	@GET
	@Path("{remoteRepositoryID}")
	public EncryptedSignedAuthToken getAuthToken(@PathParam("remoteRepositoryID") EntityID remoteRepositoryID)
	{
		File localRoot = LocalRepoRegistry.getInstance().getLocalRootForRepositoryNameOrFail(repositoryName);
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
			LocalRepoTransaction transaction = localRepoManager.beginTransaction();
			try {
				RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
				RemoteRepository remoteRepository = remoteRepositoryDAO.getObjectByIdOrFail(remoteRepositoryID);
				EncryptedSignedAuthToken response = getAuthToken(localRepoManager.getPrivateKey(), remoteRepository.getPublicKey());
				transaction.commit();
				return response;
			} finally {
				transaction.rollbackIfActive();
			}
		} finally {
			localRepoManager.close();
		}
	}

	protected EncryptedSignedAuthToken getAuthToken(byte[] localRepoPrivateKey, byte[] remoteRepoPublicKey) {
		AuthToken authToken = new AuthToken();
		Date expiryDate = new Date(System.currentTimeMillis() + (60 * 1000 * 5)); // TODO configurable!
		authToken.setExpiryDateTime(new DateTime(expiryDate));
		authToken.setPassword(randomString(40));

		byte[] authTokenData = new AuthTokenIO().serialise(authToken);
		SignedAuthToken signedAuthToken = new AuthTokenSigner(localRepoPrivateKey).sign(authTokenData);

		EncryptedSignedAuthToken encryptedSignedAuthToken =
				new SignedAuthTokenEncrypter(remoteRepoPublicKey).encrypt(signedAuthToken.getAuthTokenData());

		return encryptedSignedAuthToken;
	}

	private static SecureRandom random = new SecureRandom();

	private String randomString(final int length) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < length; i++) {
			char c = (char)(random.nextInt((Character.MAX_VALUE))); // TODO only characters typeable on an english keyboard
			sb.append(c);
		}
		return sb.toString();
	}
}
