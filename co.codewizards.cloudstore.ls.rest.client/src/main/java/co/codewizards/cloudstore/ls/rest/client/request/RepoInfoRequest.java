package co.codewizards.cloudstore.ls.rest.client.request;

import static java.util.Objects.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import co.codewizards.cloudstore.ls.core.dto.RepoInfoRequestDto;
import co.codewizards.cloudstore.ls.core.dto.RepoInfoResponseDto;

public class RepoInfoRequest extends AbstractRequest<RepoInfoResponseDto> {

	private final RepoInfoRequestDto repoInfoRequestDto;

	public RepoInfoRequest(final RepoInfoRequestDto repoInfoRequestDto) {
		this.repoInfoRequestDto = requireNonNull(repoInfoRequestDto, "repoInfoRequestDto");
	}

	@Override
	public RepoInfoResponseDto execute() {
		final WebTarget webTarget = createWebTarget("RepoInfo");
		final RepoInfoResponseDto repoInfoResponseDto = assignCredentials(webTarget.request(MediaType.APPLICATION_XML_TYPE)).post(Entity.entity(repoInfoRequestDto, MediaType.APPLICATION_XML_TYPE), RepoInfoResponseDto.class);
		return repoInfoResponseDto;
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}

}
