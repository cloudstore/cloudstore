package co.codewizards.cloudstore.rest.server.service;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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
import co.codewizards.cloudstore.core.dto.ChangeSet;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

@Path("_ChangeSet/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class ChangeSetService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(ChangeSetService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@GET
	public ChangeSet getChangeSet(final @QueryParam("localSync") boolean localSync) {
		final RepoTransport[] repoTransport = new RepoTransport[] { authenticateAndCreateLocalRepoTransport() };
		try {
			String callIdentifier = ChangeSetService.class.getName() + ".getChangeSet|" + repositoryName + '|' + getAuth().getUserName() + '|' + localSync;
			return DeferrableExecutor.getInstance().call(
					callIdentifier,
					new CallableProvider<ChangeSet>() {
						@Override
						public Callable<ChangeSet> getCallable() { // called synchronously during DeferrableExecutor.call(...) - if called at all
							final RepoTransport rt = repoTransport[0];
							repoTransport[0] = null;
							return new Callable<ChangeSet>() {
								@Override
								public ChangeSet call() throws Exception { // called *A*synchronously
									try {
										ChangeSet changeSet = rt.getChangeSet(localSync);
										return changeSet;
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
