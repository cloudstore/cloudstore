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
import co.codewizards.cloudstore.core.dto.ChangeSetDTO;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

@Path("_ChangeSetDTO/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class ChangeSetDTOService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(ChangeSetDTOService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@GET
	public ChangeSetDTO getChangeSetDTO(final @QueryParam("localSync") boolean localSync) {
		final RepoTransport[] repoTransport = new RepoTransport[] { authenticateAndCreateLocalRepoTransport() };
		try {
			String callIdentifier = ChangeSetDTOService.class.getName() + ".getChangeSet|" + repositoryName + '|' + getAuth().getUserName() + '|' + localSync;
			return DeferrableExecutor.getInstance().call(
					callIdentifier,
					new CallableProvider<ChangeSetDTO>() {
						@Override
						public Callable<ChangeSetDTO> getCallable() { // called synchronously during DeferrableExecutor.call(...) - if called at all
							final RepoTransport rt = repoTransport[0];
							repoTransport[0] = null;
							return new Callable<ChangeSetDTO>() {
								@Override
								public ChangeSetDTO call() throws Exception { // called *A*synchronously
									try {
										ChangeSetDTO changeSetDTO = rt.getChangeSetDTO(localSync);
										return changeSetDTO;
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
