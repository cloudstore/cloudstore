package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.concurrent.CallableProvider;
import co.codewizards.cloudstore.core.concurrent.DeferrableExecutor;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

@Path("_RepoFileDTO/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class RepoFileDTOService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(RepoFileDTOService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@GET
	public RepoFileDTO getRepoFileDTO()
	{
		return getRepoFileDTO("");
	}

	@GET
	@Path("{path:.*}")
	public RepoFileDTO getRepoFileDTO(final @PathParam("path") String path)
	{
		assertNotNull("path", path);
		final RepoTransport[] repoTransport = new RepoTransport[] { authenticateAndCreateLocalRepoTransport() };
		try {
			String callIdentifier = RepoFileDTOService.class.getName() + ".getRepoFileDTO|" + repositoryName + '|' + getAuth().getUserName() + '|' + path;
			return DeferrableExecutor.getInstance().call(
					callIdentifier,
					new CallableProvider<RepoFileDTO>() {
						@Override
						public Callable<RepoFileDTO> getCallable() { // called synchronously during DeferrableExecutor.call(...) - if called at all
							final RepoTransport rt = repoTransport[0];
							repoTransport[0] = null;
							final String unprefixedPath = rt.unprefixPath(path);
							return new Callable<RepoFileDTO>() {
								@Override
								public RepoFileDTO call() throws Exception { // called *A*synchronously
									try {
										RepoFileDTO repoFileDTO = rt.getRepoFileDTO(unprefixedPath);
										return repoFileDTO;
									} finally {
										rt.close();
									}
								}
							};
						}
					}, 60, TimeUnit.SECONDS); // TODO make configurable! or maybe from query-param?! or both?!
		} finally {
			if (repoTransport[0] != null)
				repoTransport[0].close();
		}
	}
}
