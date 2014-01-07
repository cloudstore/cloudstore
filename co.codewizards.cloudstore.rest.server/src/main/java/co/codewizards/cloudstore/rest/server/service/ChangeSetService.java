package co.codewizards.cloudstore.rest.server.service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.ChangeSet;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;

@Path("_ChangeSet/{repositoryID}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class ChangeSetService
{
	private static final Logger logger = LoggerFactory.getLogger(ChangeSetService.class);

	{
		logger.debug("<init>: created new instance");
	}

	private @PathParam("repositoryID") EntityID repositoryID;

	@GET
	@Path("{toRepositoryID}")
	public ChangeSet getChangeSet(@PathParam("toRepositoryID") EntityID toRepositoryID)
	{
		URL repoURL = getLocalRepositoryURL(repositoryID);
		RepoTransportFactoryRegistry repoTransportRegistry = RepoTransportFactoryRegistry.getInstance();
		RepoTransportFactory repoTransportFactory = repoTransportRegistry.getRepoTransportFactory(repoURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(repoURL);
		ChangeSet response = repoTransport.getChangeSet(toRepositoryID);
		return response;
	}

	private URL getLocalRepositoryURL(EntityID repositoryID) {
		LocalRepoRegistry registry = LocalRepoRegistry.getInstance();
		File localRoot = registry.getLocalRootOrFail(repositoryID);
		try {
			return localRoot.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
