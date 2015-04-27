package co.codewizards.cloudstore.ls.rest.server.auth;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.internal.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.ls.core.LocalServerPropertiesManager;

public class AuthFilter implements ContainerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

	protected @Context HttpServletRequest request;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
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
		final byte[] basicAuthDecodedBA = getBasicAuthEncodedBA(basicAuthEncoded);
		final StringBuilder userNameSB = new StringBuilder();
		char[] password = null;

		final ByteArrayInputStream in = new ByteArrayInputStream(basicAuthDecodedBA);
		CharArrayWriter caw = new CharArrayWriter(basicAuthDecodedBA.length + 1);
		CharArrayReader car = null;
		try {
			final Reader r = new InputStreamReader(in, IOUtil.CHARSET_NAME_UTF_8);
			int charsReadTotal = 0;
			int charsRead;
			do {
				final char[] c = new char[10];
				charsRead = r.read(c);
				caw.write(c);

				if (charsRead > 0)
					charsReadTotal += charsRead;
			} while (charsRead >= 0);

			charsRead = 0;

			car = new CharArrayReader(caw.toCharArray());
			int charsReadTotalCheck = 0;

			while (charsRead >= 0 && charsRead < charsReadTotal) {
				final char[] cbuf = new char[1];
				charsRead = car.read(cbuf);
				if (charsRead > 0)
					charsReadTotalCheck += charsRead;

				if (cbuf[0] == ':')
					break;

				userNameSB.append(cbuf[0]);
			}

			if (charsRead >= 0 && charsRead < charsReadTotal) {
				password = new char[charsReadTotal - charsReadTotalCheck];
				final int passwordSize = car.read(password);
				if (passwordSize + charsReadTotalCheck != charsReadTotal)
					throw new IllegalStateException("passwordSize and charsRead must match charsReadTotal!"
							+ " passwordSize=" + passwordSize
							+ ", charsRead=" + charsRead
							+ ", charsReadTotal=" + charsReadTotal);//TODO for testing
			}
		} catch (final Exception e) {
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_XML).entity(new Error(e)).build());
		} finally {
			// For extra safety: Overwrite all sensitive memory with 0.
			Arrays.fill(basicAuthDecodedBA, (byte)0);

			final char[] zeroArray = new char[] {0};
			// overwrite caw & car:
			if (caw != null) {
				final int oldCawSize = caw.size();
				caw.reset();
				try {
					if (car != null) {
						car.reset();
					}
					for (int i = 0; i < oldCawSize; ++i)
						caw.write(zeroArray);
					car = new CharArrayReader(caw.toCharArray());
					car.close();
					caw.reset();
					caw = null;
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		final String userName = userNameSB.toString();
		if (LocalServerPropertiesManager.USER_NAME.equals(userName)) {
			final String pw = new String(password);
			if (AuthManager.getInstance().isPasswordValid(pw))
				return;
		}

		throw newUnauthorizedException();
	}

	private WebApplicationException newUnauthorizedException() {
		return new NotAuthorizedException("Basic realm=\"CloudStoreServer.Local\"");
	}

	private byte[] getBasicAuthEncodedBA(final String basicAuthEncoded) {
		byte[] basicAuthDecodedBA;
		try {
			basicAuthDecodedBA = Base64.decode(basicAuthEncoded.getBytes(IOUtil.CHARSET_NAME_UTF_8));
		} catch (final UnsupportedEncodingException e1) {
			throw new RuntimeException(e1);
		}
		return basicAuthDecodedBA;
	}

}
