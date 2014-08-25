package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.ByteArrayInputStream;
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
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.core.util.UrlUtil;
import co.codewizards.cloudstore.oio.api.File;
import co.codewizards.cloudstore.rest.server.auth.Auth;
import co.codewizards.cloudstore.rest.server.auth.TransientRepoPasswordManager;

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
			final String authorizationHeader = request.getHeader("Authorization");
			if (authorizationHeader == null || authorizationHeader.isEmpty()) {
				logger.debug("getAuth: There is no 'Authorization' header. Replying with a Status.UNAUTHORIZED response asking for 'Basic' authentication.");

				throw newUnauthorizedException();
			}

			logger.debug("getAuth: 'Authorization' header: {}", authorizationHeader);

			if (!authorizationHeader.startsWith("Basic"))
				throw new WebApplicationException(Response.status(Status.FORBIDDEN)
						.type(MediaType.APPLICATION_XML)
						.entity(new Error("Only 'Basic' authentication is supported!")).build());

			final String basicAuthEncoded = authorizationHeader.substring("Basic".length()).trim();
			final byte[] basicAuthDecodedBA = Base64.decode(basicAuthEncoded.getBytes(IOUtil.CHARSET_UTF_8));
			final StringBuilder userNameSB = new StringBuilder();
			char[] password = null;

			final ByteArrayInputStream in = new ByteArrayInputStream(basicAuthDecodedBA);
			final CharBuffer cb = CharBuffer.allocate(basicAuthDecodedBA.length + 1);
			try {
				final Reader r = new InputStreamReader(in, IOUtil.CHARSET_UTF_8);
				int charsReadTotal = 0;
				int charsRead;
				do {
					charsRead = r.read(cb);

					if (charsRead > 0)
						charsReadTotal += charsRead;
				} while (charsRead >= 0);
				cb.position(0);

				while (cb.position() < charsReadTotal) {
					final char c = cb.get();
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
			} catch (final Exception e) {
				throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_XML).entity(new Error(e)).build());
			} finally {
				// For extra safety: Overwrite all sensitive memory with 0.
				Arrays.fill(basicAuthDecodedBA, (byte)0);

				cb.position(0);
				for (int i = 0; i < cb.capacity(); ++i)
					cb.put((char)0);
			}

			final Auth auth = new Auth();
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
		final UUID serverRepositoryId = LocalRepoRegistry.getInstance().getRepositoryId(repositoryName);
		if (serverRepositoryId == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND)
					.type(MediaType.APPLICATION_XML)
					.entity(new Error(String.format("HTTP 404: repositoryName='%s' is neither an alias nor an ID of a known repository!", repositoryName))).build());
		}

		final Auth auth = getAuth();
		try {
			final UUID clientRepositoryId = getClientRepositoryIdFromUserName(auth.getUserName());
			if (clientRepositoryId != null) {
				if (TransientRepoPasswordManager.getInstance().isPasswordValid(serverRepositoryId, clientRepositoryId, auth.getPassword()))
					return auth.getUserName();
				else
					throw newUnauthorizedException();
			}
		} finally {
			// We clear auth, even though it is kept in this instance, because we need the password only for
			// authentication. We authenticate only once and don't need it later, again. Every service invocation
			// has its own new REST service object instance. Hence, this is clearing should be really no problem.
			auth.clear();
		}
		throw newUnauthorizedException();
	}

	protected UUID getClientRepositoryIdFromUserName(final String userName) {
		if (assertNotNull("userName", userName).startsWith(AuthConstants.USER_NAME_REPOSITORY_ID_PREFIX)) {
			final String repositoryIdString = userName.substring(AuthConstants.USER_NAME_REPOSITORY_ID_PREFIX.length());
			final UUID clientRepositoryId = UUID.fromString(repositoryIdString);
			return clientRepositoryId;
		}
		return null;
	}

	protected UUID getClientRepositoryIdFromUserNameOrFail(final String userName) {
		final UUID clientRepositoryId = getClientRepositoryIdFromUserName(userName);
		if (clientRepositoryId == null)
			throw new IllegalArgumentException(String.format("userName='%s' is not a repository!", userName));

		return clientRepositoryId;
	}

	private WebApplicationException newUnauthorizedException() {
		return new WebApplicationException(Response.status(Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic realm=\"CloudStoreServer\"").build());
	}

	protected RepoTransport authenticateAndCreateLocalRepoTransport() {
		final String userName = authenticateAndReturnUserName();
		final UUID clientRepositoryId = getClientRepositoryIdFromUserNameOrFail(userName);
		final URL localRootURL = getLocalRootURL(clientRepositoryId);
		final RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(localRootURL);
		final RepoTransport repoTransport = repoTransportFactory.createRepoTransport(localRootURL, clientRepositoryId);
		return repoTransport;
	}

	protected URL authenticateAndGetLocalRootURL() {
		final String userName = authenticateAndReturnUserName();
		final UUID clientRepositoryId = getClientRepositoryIdFromUserNameOrFail(userName);
		return getLocalRootURL(clientRepositoryId);
	}

	protected URL getLocalRootURL(final UUID clientRepositoryId) {
		assertNotNull("repositoryName", repositoryName);
		final File localRoot = LocalRepoRegistry.getInstance().getLocalRootForRepositoryNameOrFail(repositoryName);
		final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
			final String localPathPrefix = localRepoManager.getLocalPathPrefixOrFail(clientRepositoryId);
			URL localRootURL;
			try {
				localRootURL = localRoot.toURI().toURL();
			} catch (final MalformedURLException e) {
				throw new RuntimeException(e);
			}

			localRootURL = UrlUtil.appendNonEncodedPath(localRootURL, localPathPrefix);

			return localRootURL;
		} finally {
			localRepoManager.close();
		}
	}
}
