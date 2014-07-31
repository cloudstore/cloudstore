package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;

@Path("_requestRepoConnection/{repositoryName}")
public class RequestRepoConnectionService
{
	private static final Logger logger = LoggerFactory.getLogger(RequestRepoConnectionService.class);

	{
		logger.debug("<init>: created new instance");
	}

	private @PathParam("repositoryName") String repositoryName;

	@POST
	@Consumes(MediaType.APPLICATION_XML)
	public void requestConnection(RepositoryDto clientRepositoryDto)
	{
		requestConnection("", clientRepositoryDto);
	}

	@POST
	@Path("{pathPrefix:.*}")
	@Consumes(MediaType.APPLICATION_XML)
	public void requestConnection(@PathParam("pathPrefix") String pathPrefix, RepositoryDto clientRepositoryDto)
	{
		assertNotNull("pathPrefix", pathPrefix);
		assertNotNull("repositoryDto", clientRepositoryDto);

		URL localRootURL = LocalRepoRegistry.getInstance().getLocalRootURLForRepositoryNameOrFail(repositoryName);

		if (!"".equals(pathPrefix)) {
			String localRootURLString = localRootURL.toExternalForm();
			if (localRootURLString.endsWith("/"))
				localRootURLString = localRootURLString.substring(0, localRootURLString.length() - 1);

			if (!pathPrefix.startsWith("/"))
				pathPrefix = '/' + pathPrefix;

			localRootURLString = localRootURLString + pathPrefix;
			try {
				localRootURL = new URL(localRootURLString);
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}

		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(localRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(localRootURL, clientRepositoryDto.getRepositoryId());
		try {
			repoTransport.requestRepoConnection(clientRepositoryDto.getPublicKey());
		} finally {
			repoTransport.close();
		}
	}
}
