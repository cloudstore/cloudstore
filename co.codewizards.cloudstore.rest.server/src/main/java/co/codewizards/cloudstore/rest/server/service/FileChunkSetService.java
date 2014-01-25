package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.FileChunkSet;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

@Path("_FileChunkSet/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class FileChunkSetService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(FileChunkSetService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@GET
	public FileChunkSet getFileChunkSet(@QueryParam("allowHollow") boolean allowHollow)
	{
		return getFileChunkSet("", allowHollow);
	}

	@GET
	@Path("{path:.*}")
	public FileChunkSet getFileChunkSet(@PathParam("path") String path, @QueryParam("allowHollow") boolean allowHollow)
	{
		assertNotNull("path", path);
		RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();
		try {
			path = repoTransport.unprefixPath(path);
			FileChunkSet response = repoTransport.getFileChunkSet(path, allowHollow);
			return response;
		} finally {
			repoTransport.close();
		}
	}
}
