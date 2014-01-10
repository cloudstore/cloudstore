package co.codewizards.cloudstore.rest.server.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.internal.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.auth.AuthConstants;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.rest.server.auth.Auth;
import co.codewizards.cloudstore.rest.server.auth.AuthRepoPasswordManager;

public abstract class AbstractServiceWithRepoToRepoAuth {

	private static final Logger logger = LoggerFactory.getLogger(AbstractServiceWithRepoToRepoAuth.class);

	protected @Context HttpServletRequest request;

	protected @PathParam("repositoryName") String repositoryName;

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
		String authorizationHeader = request.getHeader("Authorization");
		if (authorizationHeader == null || authorizationHeader.isEmpty()) {
			logger.debug("getAuth: There is no 'Authorization' header. Replying with a Status.UNAUTHORIZED response asking for 'Basic' authentication.");

			throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic realm=\"Cumulus4jKeyServer\"").build());
		}

		logger.debug("getAuth: 'Authorization' header: {}", authorizationHeader);

		if (!authorizationHeader.startsWith("Basic"))
			throw new WebApplicationException(Response.status(Status.FORBIDDEN).entity(new Error("Only 'Basic' authentication is supported!")).build());

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
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Error(e)).build());
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
		EntityID serverRepositoryID = LocalRepoRegistry.getInstance().getRepositoryID(repositoryName);
		if (serverRepositoryID == null) {
			serverRepositoryID = new EntityID(repositoryName);
		}

		Auth auth = getAuth();
		try {
			if (auth.getUserName().startsWith(AuthConstants.USER_NAME_REPOSITORY_ID_PREFIX)) {
				String repositoryIDString = auth.getUserName().substring(AuthConstants.USER_NAME_REPOSITORY_ID_PREFIX.length());
				EntityID clientRepositoryID = new EntityID(repositoryIDString);

				if (AuthRepoPasswordManager.getInstance().isPasswordValid(serverRepositoryID, clientRepositoryID, auth.getPassword()))
					return auth.getUserName();
				else
					throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).build());
			}
		} finally {
			auth.clear();
		}
		throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).build());
	}
}
