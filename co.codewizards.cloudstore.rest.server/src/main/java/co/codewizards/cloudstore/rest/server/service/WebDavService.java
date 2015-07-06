package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.util.AssertUtil;
import co.codewizards.cloudstore.rest.server.webdav.COPY;
import co.codewizards.cloudstore.rest.server.webdav.MKCOL;
import co.codewizards.cloudstore.rest.server.webdav.MOVE;
import co.codewizards.cloudstore.rest.server.webdav.PROPFIND;

// TODO We should implement WebDAV: http://tools.ietf.org/html/rfc2518 + http://en.wikipedia.org/wiki/WebDAV
// TODO We should *additionally* provide browsing via HTML replies (=> @Produces(MediaType.HTML))
@Path("{repositoryName:[^_/][^/]*}")
public class WebDavService extends AbstractServiceWithRepoToRepoAuth {
	private static final Logger logger = LoggerFactory.getLogger(WebDavService.class);

	{
		logger.debug("<init>: created new instance");
	}

//	@GET
//	@Produces(MediaType.WILDCARD)
//	public Object getContents() {
//		return getContents("");
//	}

	@GET
	@Path("{path:.*}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public byte[] getFileData(
			@PathParam("path") String path,
			@QueryParam("offset") final long offset,
			@QueryParam("length") @DefaultValue("-1") final int length)
	{
		AssertUtil.assertNotNull("path", path);
		try (final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport()) {
			path = repoTransport.unprefixPath(path);
			return repoTransport.getFileData(path, offset, length);
		}
	}

	@MKCOL
	@Path("{path:.*}")
	public void mkcol(@PathParam("path") final String path, @QueryParam("lastModified") final DateTime lastModified) {
		throw new UnsupportedOperationException("NYI");
	}

	@DELETE
	@Path("{path:.*}")
	public void delete(@PathParam("path") String path) {
		AssertUtil.assertNotNull("path", path);
		try (final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();) {
			path = repoTransport.unprefixPath(path);
			repoTransport.delete(path);
		}
	}

	@PUT
	@Path("{path:.*}")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public void putFileData(@PathParam("path") String path, @QueryParam("offset") final long offset, final byte[] fileData) {
		assertNotNull("path", path);
		try (final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();) {
			path = repoTransport.unprefixPath(path);
			repoTransport.putFileData(path, offset, fileData);
		}
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String browse() {
		return browse("");
	}

	@GET
	@Path("{path:.*}")
	@Produces(MediaType.TEXT_HTML)
	public String browse(@PathParam("path") String path){
		assertNotNull("path", path);
		try (final RepoTransport repoTransport = authenticateWithLdap()) {
			path = repoTransport.unprefixPath(path);
			RepoFileDto dto = repoTransport.getRepoFileDto(path);
			return "<html><body>" + dto.toString() + "</body></html>";
		}
	}


	@COPY
	@Path("{path:.*}")
	public void copy(@PathParam("path") final String path, @HeaderParam("DESTINATION") final String destination) {
		throw new UnsupportedOperationException("NYI");
	}

	@MOVE
	@Path("{path:.*}")
	public void move(@PathParam("path") final String path, @HeaderParam("DESTINATION") final String destination) {
		throw new UnsupportedOperationException("NYI");
	}

	@PROPFIND
	@Path("{path:.*}")
	public void propfind(@HeaderParam("CONTENT_LENGTH") final long contentLength) {
		throw new UnsupportedOperationException("NYI");
	}
}
