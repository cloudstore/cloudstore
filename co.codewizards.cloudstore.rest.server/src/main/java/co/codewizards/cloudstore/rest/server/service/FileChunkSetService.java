package co.codewizards.cloudstore.rest.server.service;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.concurrent.CallableProvider;
import co.codewizards.cloudstore.core.concurrent.DeferrableExecutor;
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
	public FileChunkSet getFileChunkSet(final @PathParam("path") String path, final @QueryParam("allowHollow") boolean allowHollow)
	{
		assertNotNull("path", path);
		final RepoTransport[] repoTransport = new RepoTransport[] { authenticateAndCreateLocalRepoTransport() };
		try {
			String callIdentifier = FileChunkSetService.class.getName() + ".getFileChunkSet|" + repositoryName + '|' + getAuth().getUserName() + '|' + path + '|' + allowHollow;
			return DeferrableExecutor.getInstance().call(
					callIdentifier,
					new CallableProvider<FileChunkSet>() {
						@Override
						public Callable<FileChunkSet> getCallable() { // called synchronously during DeferrableExecutor.call(...) - if called at all
							final RepoTransport rt = repoTransport[0];
							repoTransport[0] = null;
							final String unprefixedPath = rt.unprefixPath(path);
							return new Callable<FileChunkSet>() {
								@Override
								public FileChunkSet call() throws Exception { // called *A*synchronously
									try {
										FileChunkSet fileChunkSet = rt.getFileChunkSet(unprefixedPath, allowHollow);
										return fileChunkSet;
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
