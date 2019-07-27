package co.codewizards.cloudstore.rest.server.service;

import static java.util.Objects.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

@Path("_makeDirectory/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class MakeDirectoryService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(MakeDirectoryService.class);

	{
		logger.debug("<init>: created new instance");
	}

	private @QueryParam("lastModified") DateTime lastModified;

	@POST
	public void makeDirectory()
	{
		makeDirectory("");
	}

	@POST
	@Path("{path:.*}")
	public void makeDirectory(@PathParam("path") String path)
	{
		requireNonNull(path, "path");
		try (final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();) {
			path = repoTransport.unprefixPath(path);
			repoTransport.makeDirectory(path, lastModified == null ? null : lastModified.toDate());
		}
	}
}
