package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.net.URL;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.FileChunkSet;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;

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
	public FileChunkSet getFileChunkSet()
	{
		return getFileChunkSet("");
	}

	@GET
	@Path("{path:.*}")
	public FileChunkSet getFileChunkSet(@PathParam("path") String path)
	{
		assertNotNull("path", path);
		authenticateAndReturnUserName();

		URL localRootURL = LocalRepoRegistry.getInstance().getLocalRootURLForRepositoryNameOrFail(repositoryName);
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(localRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(localRootURL);
		try {
			FileChunkSet response = repoTransport.getFileChunkSet(path);
			return response;
		} finally {
			repoTransport.close();
		}
	}
}