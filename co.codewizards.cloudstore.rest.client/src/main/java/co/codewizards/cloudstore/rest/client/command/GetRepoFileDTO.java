package co.codewizards.cloudstore.rest.client.command;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import co.codewizards.cloudstore.core.dto.RepoFileDTO;

public class GetRepoFileDTO extends AbstractCommand<RepoFileDTO> {
	private final String repositoryName;
	private final String path;

	public GetRepoFileDTO(final String repositoryName, final String path) {
		this.repositoryName = assertNotNull("repositoryName", repositoryName);
		this.path = path;
	}

	@Override
	public RepoFileDTO execute() {
		final WebTarget webTarget = createWebTarget(getPath(RepoFileDTO.class), urlEncode(repositoryName), encodePath(path));
		final RepoFileDTO repoFileDTO = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(RepoFileDTO.class);
		return repoFileDTO;
	}

	@Override
	public boolean isResultNullable() {
		return true;
	}

}
