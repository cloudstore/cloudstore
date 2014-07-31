package co.codewizards.cloudstore.rest.client.command;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import co.codewizards.cloudstore.core.dto.RepoFileDto;

public class GetRepoFileDto extends AbstractCommand<RepoFileDto> {
	private final String repositoryName;
	private final String path;

	public GetRepoFileDto(final String repositoryName, final String path) {
		this.repositoryName = assertNotNull("repositoryName", repositoryName);
		this.path = path;
	}

	@Override
	public RepoFileDto execute() {
		final WebTarget webTarget = createWebTarget(getPath(RepoFileDto.class), urlEncode(repositoryName), encodePath(path));
		final RepoFileDto repoFileDto = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(RepoFileDto.class);
		return repoFileDto;
	}

	@Override
	public boolean isResultNullable() {
		return true;
	}

}
