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
import co.codewizards.cloudstore.core.dto.FileChunkSetDTO;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

@Path("_FileChunkSetDTO/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class FileChunkSetDTOService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(FileChunkSetDTOService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@GET
	public FileChunkSetDTO getFileChunkSetDTO()
	{
		return getFileChunkSetDTO("");
	}

	@GET
	@Path("{path:.*}")
	public FileChunkSetDTO getFileChunkSetDTO(final @PathParam("path") String path)
	{
		assertNotNull("path", path);
		final RepoTransport[] repoTransport = new RepoTransport[] { authenticateAndCreateLocalRepoTransport() };
		try {
			String callIdentifier = FileChunkSetDTOService.class.getName() + ".getFileChunkSet|" + repositoryName + '|' + getAuth().getUserName() + '|' + path;
			return DeferrableExecutor.getInstance().call(
					callIdentifier,
					new CallableProvider<FileChunkSetDTO>() {
						@Override
						public Callable<FileChunkSetDTO> getCallable() { // called synchronously during DeferrableExecutor.call(...) - if called at all
							final RepoTransport rt = repoTransport[0];
							repoTransport[0] = null;
							final String unprefixedPath = rt.unprefixPath(path);
							return new Callable<FileChunkSetDTO>() {
								@Override
								public FileChunkSetDTO call() throws Exception { // called *A*synchronously
									try {
										FileChunkSetDTO fileChunkSetDTO = rt.getFileChunkSet(unprefixedPath);
										return fileChunkSetDTO;
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
