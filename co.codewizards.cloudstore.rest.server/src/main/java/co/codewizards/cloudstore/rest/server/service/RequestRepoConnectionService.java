package co.codewizards.cloudstore.rest.server.service;

import static java.util.Objects.*;

import java.net.URL;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistryImpl;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.core.util.UrlUtil;

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
	public void requestConnection(final RepositoryDto clientRepositoryDto)
	{
		requestConnection("", clientRepositoryDto);
	}

	@POST
	@Path("{pathPrefix:.*}")
	@Consumes(MediaType.APPLICATION_XML)
	public void requestConnection(@PathParam("pathPrefix") final String pathPrefix, final RepositoryDto clientRepositoryDto)
	{
		requireNonNull(pathPrefix, "pathPrefix");
		requireNonNull(clientRepositoryDto, "repositoryDto");

		URL localRootURL = LocalRepoRegistryImpl.getInstance().getLocalRootURLForRepositoryNameOrFail(repositoryName);
		localRootURL = UrlUtil.appendNonEncodedPath(localRootURL, pathPrefix);

		final RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(localRootURL);
		try (final RepoTransport repoTransport = repoTransportFactory.createRepoTransport(localRootURL, clientRepositoryDto.getRepositoryId());) {
			requestConnection(repoTransport, pathPrefix, clientRepositoryDto);
		}
	}

	protected void requestConnection(final RepoTransport repoTransport, final String pathPrefix, final RepositoryDto clientRepositoryDto) {
		repoTransport.requestRepoConnection(clientRepositoryDto.getPublicKey());
	}
}
