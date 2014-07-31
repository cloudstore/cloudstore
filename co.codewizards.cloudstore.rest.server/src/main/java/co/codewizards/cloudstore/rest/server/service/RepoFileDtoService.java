package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.concurrent.Callable;

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
import co.codewizards.cloudstore.core.dto.RepoFileDto;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

@Path("_RepoFileDto/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class RepoFileDtoService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(RepoFileDtoService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@GET
	public RepoFileDto getRepoFileDto()
	{
		return getRepoFileDto("");
	}

	@GET
	@Path("{path:.*}")
	public RepoFileDto getRepoFileDto(final @PathParam("path") String path)
	{
		assertNotNull("path", path);
		final RepoTransport[] repoTransport = new RepoTransport[] { authenticateAndCreateLocalRepoTransport() };
		try {
			String callIdentifier = RepoFileDtoService.class.getName() + ".getRepoFileDto|" + repositoryName + '|' + getAuth().getUserName() + '|' + path;
			return DeferrableExecutor.getInstance().call(
					callIdentifier,
					new CallableProvider<RepoFileDto>() {
						@Override
						public Callable<RepoFileDto> getCallable() { // called synchronously during DeferrableExecutor.call(...) - if called at all
							final RepoTransport rt = repoTransport[0];
							repoTransport[0] = null;
							final String unprefixedPath = rt.unprefixPath(path);
							return new Callable<RepoFileDto>() {
								@Override
								public RepoFileDto call() throws Exception { // called *A*synchronously
									try {
										RepoFileDto repoFileDto = rt.getRepoFileDto(unprefixedPath);
										return repoFileDto;
									} finally {
										rt.close();
									}
								}
							};
						}
					});
		} finally {
			if (repoTransport[0] != null)
				repoTransport[0].close();
		}
	}
}
