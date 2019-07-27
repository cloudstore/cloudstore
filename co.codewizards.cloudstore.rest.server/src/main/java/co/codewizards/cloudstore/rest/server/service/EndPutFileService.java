package co.codewizards.cloudstore.rest.server.service;

import static java.util.Objects.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.DateTime;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

@Path("_endPutFile/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class EndPutFileService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(EndPutFileService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@PUT
	@Path("{path:.*}")
	public void endPutFile(
			@PathParam("path") String path,
			@QueryParam("lastModified") final DateTime lastModified,
			@QueryParam("length") final long length,
			@QueryParam("sha1") final String sha1)
	{
		requireNonNull(path, "path");
		requireNonNull(lastModified, "lastModified");
		final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();
		try {
			path = repoTransport.unprefixPath(path);
			repoTransport.endPutFile(path, lastModified.toDate(), length, sha1);
		} finally {
			repoTransport.close();
		}
	}
}
