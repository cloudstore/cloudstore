package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.net.URL;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;

// TODO We should implement WebDAV: http://tools.ietf.org/html/rfc2518 + http://en.wikipedia.org/wiki/WebDAV
// TODO We should *additionally* provide browsing via HTML replies (=> @Produces(MediaType.HTML))
@Path("{repositoryName:[^_/][^/]*}")
public class WebDavService {
	private static final Logger logger = LoggerFactory.getLogger(WebDavService.class);

	{
		logger.debug("<init>: created new instance");
	}

	private @PathParam("repositoryName") String repositoryName;

	@GET
	@Produces(MediaType.WILDCARD)
	public Object getContents() {
		return getContents("");
	}

	@GET
	@Path("{path:.*}")
	@Produces(MediaType.WILDCARD)
	public Object getContents(@PathParam("path") String path) {
		if (!assertNotNull("path", path).startsWith("/"))
			path = "/" + path;

		return String.format("RepositoryName: '%s'<br/>Path: '%s'", repositoryName, path);
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

}
