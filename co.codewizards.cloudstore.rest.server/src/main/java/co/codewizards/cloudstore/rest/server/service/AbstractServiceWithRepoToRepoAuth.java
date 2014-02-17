package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.internal.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.auth.AuthConstants;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.persistence.RemoteRepository;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.rest.server.auth.Auth;
import co.codewizards.cloudstore.rest.server.auth.AuthRepoPasswordManager;

public abstract class AbstractServiceWithRepoToRepoAuth {

	private static final Logger logger = LoggerFactory.getLogger(AbstractServiceWithRepoToRepoAuth.class);

	protected @Context HttpServletRequest request;

	protected @PathParam("repositoryName") String repositoryName;

	private Auth auth;

	/**
	 * Get the authentication information. This method does <b>not</b> verify, if the given authentication information
	 * is correct! It merely checks, if the client sent a 'Basic' authentication header. If it did not,
	 * this method throws a {@link WebApplicationException} with {@link Status#UNAUTHORIZED} or {@link Status#FORBIDDEN}.
	 * If it did, it extracts the information and puts it into an {@link Auth} instance.
	 * @return the {@link Auth} instance extracted from the client's headers. Never <code>null</code>.
	 * @throws WebApplicationException with {@link Status#UNAUTHORIZED}, if the client did not send an 'Authorization' header;
	 * with {@link Status#FORBIDDEN}, if there is an 'Authorization' header, but no 'Basic' authentication header (other authentication modes, like e.g. 'Digest'
	 * are not supported).
	 */
	protected Auth getAuth()
	throws WebApplicationException
	{
		if (auth == null) {
			String authorizationHeader = request.getHeader("Authorization");
			if (authorizationHeader == null || authorizationHeader.isEmpty()) {
				logger.debug("getAuth: There is no 'Authorization' header. Replying with a Status.UNAUTHORIZED response asking for 'Basic' authentication.");

				throw newUnauthorizedException();
			}

			logger.debug("getAuth: 'Authorization' header: {}", authorizationHeader);

			if (!authorizationHeader.startsWith("Basic"))
				throw new WebApplicationException(Response.status(Status.FORBIDDEN)
						.type(MediaType.APPLICATION_XML)
						.entity(new Error("Only 'Basic' authentication is supported!")).build());

			String basicAuthEncoded = authorizationHeader.substring("Basic".length()).trim();
			byte[] basicAuthDecodedBA = Base64.decode(basicAuthEncoded.getBytes(IOUtil.CHARSET_UTF_8));
			StringBuilder userNameSB = new StringBuilder();
			char[] password = null;

			ByteArrayInputStream in = new ByteArrayInputStream(basicAuthDecodedBA);
			CharBuffer cb = CharBuffer.allocate(basicAuthDecodedBA.length + 1);
			try {
				Reader r = new InputStreamReader(in, IOUtil.CHARSET_UTF_8);
				int charsReadTotal = 0;
				int charsRead;
				do {
					charsRead = r.read(cb);

					if (charsRead > 0)
						charsReadTotal += charsRead;
				} while (charsRead >= 0);
				cb.position(0);

				while (cb.position() < charsReadTotal) {
					char c = cb.get();
					if (c == ':')
						break;

					userNameSB.append(c);
				}

				if (cb.position() < charsReadTotal) {
					password = new char[charsReadTotal - cb.position()];
					int idx = 0;
					while (cb.position() < charsReadTotal)
						password[idx++] = cb.get();
				}
			} catch (Exception e) {
				throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_XML).entity(new Error(e)).build());
			} finally {
				// For extra safety: Overwrite all sensitive memory with 0.
				Arrays.fill(basicAuthDecodedBA, (byte)0);

				cb.position(0);
				for (int i = 0; i < cb.capacity(); ++i)
					cb.put((char)0);
			}

			Auth auth = new Auth();
			auth.setUserName(userNameSB.toString());
			auth.setPassword(password);
			this.auth = auth;
		}
		return auth;
	}

	/**
	 * Get the {@link Auth} information via {@link #getAuth()} and verify, if they are valid.
	 * @return the {@link Auth} information via {@link #getAuth()}; never <code>null</code>.
	 * @throws WebApplicationException with {@link Status#UNAUTHORIZED}, if the client did not send an 'Authorization' header
	 * or if user-name / password is wrong;
	 * with {@link Status#FORBIDDEN}, if there is an 'Authorization' header, but no 'Basic' authentication header (other authentication modes, like e.g. 'Digest'
	 * are not supported); with {@link Status#INTERNAL_SERVER_ERROR}, if there was an {@link IOException}.
	 */
	protected String authenticateAndReturnUserName()
	throws WebApplicationException
	{
		UUID serverRepositoryId = LocalRepoRegistry.getInstance().getRepositoryId(repositoryName);
		if (serverRepositoryId == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND)
					.type(MediaType.APPLICATION_XML)
					.entity(new Error(String.format("HTTP 404: repositoryName='%s' is neither an alias nor an ID of a known repository!", repositoryName))).build());
		}

		Auth auth = getAuth();
		try {
			UUID clientRepositoryId = getClientRepositoryIdFromUserName(auth.getUserName());
			if (clientRepositoryId != null) {
				if (AuthRepoPasswordManager.getInstance().isPasswordValid(serverRepositoryId, clientRepositoryId, auth.getPassword()))
					return auth.getUserName();
				else
					throw newUnauthorizedException();
			}
		} finally {
			auth.clear();
		}
		throw newUnauthorizedException();
	}

	protected UUID getClientRepositoryIdFromUserName(String userName) {
		if (assertNotNull("userName", userName).startsWith(AuthConstants.USER_NAME_REPOSITORY_ID_PREFIX)) {
			String repositoryIdString = userName.substring(AuthConstants.USER_NAME_REPOSITORY_ID_PREFIX.length());
			UUID clientRepositoryId = UUID.fromString(repositoryIdString);
			return clientRepositoryId;
		}
		return null;
	}

	protected UUID getClientRepositoryIdFromUserNameOrFail(String userName) {
		UUID clientRepositoryId = getClientRepositoryIdFromUserName(userName);
		if (clientRepositoryId == null)
			throw new IllegalArgumentException(String.format("userName='%s' is not a repository!", userName));

		return clientRepositoryId;
	}

	private WebApplicationException newUnauthorizedException() {
		return new WebApplicationException(Response.status(Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic realm=\"CloudStoreServer\"").build());
	}

	protected RepoTransport authenticateAndCreateLocalRepoTransport() {
		String userName = authenticateAndReturnUserName();
		UUID clientRepositoryId = getClientRepositoryIdFromUserNameOrFail(userName);
		URL localRootURL = getLocalRootURL(clientRepositoryId);
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(localRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(localRootURL, clientRepositoryId);
		return repoTransport;
	}

	protected URL authenticateAndGetLocalRootURL() {
		String userName = authenticateAndReturnUserName();
		UUID clientRepositoryId = getClientRepositoryIdFromUserNameOrFail(userName);
		return getLocalRootURL(clientRepositoryId);
	}

	protected URL getLocalRootURL(UUID clientRepositoryId) {
		assertNotNull("repositoryName", repositoryName);
		String localPathPrefix;
		File localRoot = LocalRepoRegistry.getInstance().getLocalRootForRepositoryNameOrFail(repositoryName);
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();
		try {
			RemoteRepository clientRemoteRepository = transaction.getDAO(RemoteRepositoryDAO.class).getRemoteRepositoryOrFail(clientRepositoryId);
			localPathPrefix = clientRemoteRepository.getLocalPathPrefix();
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
		URL localRootURL;
		try {
			localRootURL = localRoot.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		if (!localPathPrefix.isEmpty()) {
			String localRootURLString = localRootURL.toExternalForm();
			if (localRootURLString.endsWith("/"))
				localRootURLString = localRootURLString.substring(0, localRootURLString.length() - 1);

			// localPathPrefix is guaranteed to start with a '/'.
			localRootURLString += localPathPrefix;
			try {
				localRootURL = new URL(localRootURLString);
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		return localRootURL;
	}
}
