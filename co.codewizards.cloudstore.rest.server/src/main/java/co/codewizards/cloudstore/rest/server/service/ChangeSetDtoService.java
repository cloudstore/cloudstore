package co.codewizards.cloudstore.rest.server.service;

import java.util.concurrent.Callable;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.concurrent.CallableProvider;
import co.codewizards.cloudstore.core.concurrent.DeferrableExecutor;
import co.codewizards.cloudstore.core.dto.ChangeSetDto;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

@Path("_ChangeSetDto/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class ChangeSetDtoService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(ChangeSetDtoService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@GET
	public ChangeSetDto getChangeSetDto(@QueryParam("localSync") final boolean localSync,
			@QueryParam("lastSyncToRemoteRepoLocalRepositoryRevisionSynced") final Long lastSyncToRemoteRepoLocalRepositoryRevisionSynced) {
		final RepoTransport[] repoTransport = new RepoTransport[] { authenticateAndCreateLocalRepoTransport() };
		try {
			final String callIdentifier = ChangeSetDtoService.class.getName() + ".getChangeSetDto|" + repositoryName + '|' + getAuth().getUserName() + '|' + localSync + '|' + lastSyncToRemoteRepoLocalRepositoryRevisionSynced;
			return DeferrableExecutor.getInstance().call(
					callIdentifier,
					new CallableProvider<ChangeSetDto>() {
						@Override
						public Callable<ChangeSetDto> getCallable() { // called synchronously during DeferrableExecutor.call(...) - if called at all
							final RepoTransport rt = repoTransport[0];
							repoTransport[0] = null;
							return new Callable<ChangeSetDto>() {
								@Override
								public ChangeSetDto call() throws Exception { // called *A*synchronously
									try {
										final ChangeSetDto changeSetDto = getChangeSetDto(rt, localSync, lastSyncToRemoteRepoLocalRepositoryRevisionSynced);
										return changeSetDto;
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

	protected ChangeSetDto getChangeSetDto(final RepoTransport repoTransport, final boolean localSync, final Long lastSyncToRemoteRepoLocalRepositoryRevisionSynced) {
		return repoTransport.getChangeSetDto(localSync, lastSyncToRemoteRepoLocalRepositoryRevisionSynced);
	}
}
