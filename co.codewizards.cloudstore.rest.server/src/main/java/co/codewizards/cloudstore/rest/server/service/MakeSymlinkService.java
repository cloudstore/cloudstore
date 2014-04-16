package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

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

@Path("_makeSymlink/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class MakeSymlinkService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(MakeSymlinkService.class);

	{
		logger.debug("<init>: created new instance");
	}

	private @QueryParam("target") String target;

	private @QueryParam("lastModified") DateTime lastModified;

	@POST
	public void makeSymlink()
	{
		makeSymlink("");
	}

	@POST
	@Path("{path:.*}")
	public void makeSymlink(@PathParam("path") String path)
	{
		assertNotNull("path", path);
		assertNotNull("target", target);
		RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();
		try {
			path = repoTransport.unprefixPath(path);
			repoTransport.makeSymlink(path, target, lastModified == null ? null : lastModified.toDate());
		} finally {
			repoTransport.close();
		}
	}
}
