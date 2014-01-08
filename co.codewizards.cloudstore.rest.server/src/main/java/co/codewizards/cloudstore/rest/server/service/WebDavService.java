package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

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

import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.rest.server.webdav.COPY;
import co.codewizards.cloudstore.rest.server.webdav.MKCOL;
import co.codewizards.cloudstore.rest.server.webdav.MOVE;
import co.codewizards.cloudstore.rest.server.webdav.PROPFIND;

// TODO We should implement WebDAV: http://tools.ietf.org/html/rfc2518 + http://en.wikipedia.org/wiki/WebDAV
// TODO We should *additionally* provide browsing via HTML replies (=> @Produces(MediaType.HTML))
@Path("{repositoryName:[^_/][^/]*}")
public class WebDavService {
	private static final Logger logger = LoggerFactory.getLogger(WebDavService.class);

	{
		logger.debug("<init>: created new instance");
	}

	private @PathParam("repositoryName") String repositoryName;

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
			@QueryParam("offset") long offset,
			@QueryParam("length") @DefaultValue("-1") int length)
	{
		if (!assertNotNull("path", path).startsWith("/"))
			path = "/" + path;

		URL localRootURL = LocalRepoRegistry.getInstance().getLocalRootURLForRepositoryNameOrFail(repositoryName);
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(localRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(localRootURL);
		try {
			return repoTransport.getFileData(path, offset, length);
		} finally {
			repoTransport.close();
		}
	}

	@DELETE
	@Path("{path:.*}")
	public void delete(@PathParam("path") String path) {
		assertNotNull("path", path);

		URL localRootURL = LocalRepoRegistry.getInstance().getLocalRootURLForRepositoryNameOrFail(repositoryName);
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(localRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(localRootURL);
		try {
			repoTransport.delete(path);
		} finally {
			repoTransport.close();
		}
	}

	@PUT
	@Path("{path:.*}")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public void putFileData(@PathParam("path") String path, @QueryParam("offset") long offset, byte[] fileData) {
		assertNotNull("path", path);

		URL localRootURL = LocalRepoRegistry.getInstance().getLocalRootURLForRepositoryNameOrFail(repositoryName);
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(localRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(localRootURL);
		try {
			repoTransport.putFileData(path, offset, fileData);
		} finally {
			repoTransport.close();
		}
	}

	@GET
	@Path("_test/{path:.*}")
	@Produces(MediaType.TEXT_HTML)
	public Collection<File> browseFile(@PathParam("path") String path) {
		if (!assertNotNull("path", path).startsWith("/"))
			path = "/" + path;

		//TODO return the list of files
		return Collections.EMPTY_SET;
	}

	@COPY
	@Path("{path:.*}")
	public void copy(@PathParam("path") String path, @HeaderParam("DESTINATION") final String destination) {
		assertNotNull("path", path);

		URL localRootURL = LocalRepoRegistry.getInstance().getLocalRootURLForRepositoryNameOrFail(repositoryName);
//		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(localRootURL);
//		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(localRootURL);
//		try {
//			repoTransport.delete(path);
//		} finally {
//			repoTransport.close();
//		}
	}

	@MOVE
	@Path("{path:.*}")
	public void move(@PathParam("path") String path, @HeaderParam("DESTINATION") final String destination) {
		assertNotNull("path", path);

		URL localRootURL = LocalRepoRegistry.getInstance().getLocalRootURLForRepositoryNameOrFail(repositoryName);
//		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(localRootURL);
//		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(localRootURL);
//		try {
//			repoTransport.delete(path);
//		} finally {
//			repoTransport.close();
//		}
	}

	@MKCOL
	@Path("{path:.*}")
	public void mkcol(@PathParam("path") String path, @HeaderParam("CONTENT_LENGTH") final long contentLength) {
//		assertNotNull("path", path);
//
//		URL localRootURL = LocalRepoRegistry.getInstance().getLocalRootURLForRepositoryNameOrFail(repositoryName);
//		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(localRootURL);
//		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(localRootURL);
//		try {
//			repoTransport.delete(path);
//		} finally {
//			repoTransport.close();
//		}
	}

	@PROPFIND
	@Path("{path:.*}")
	public void propfind(@HeaderParam("CONTENT_LENGTH") final long contentLength) {
//		assertNotNull("path", path);
//
//		URL localRootURL = LocalRepoRegistry.getInstance().getLocalRootURLForRepositoryNameOrFail(repositoryName);
//		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(localRootURL);
//		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(localRootURL);
//		try {
//			repoTransport.delete(path);
//		} finally {
//			repoTransport.close();
//		}
	}
}
