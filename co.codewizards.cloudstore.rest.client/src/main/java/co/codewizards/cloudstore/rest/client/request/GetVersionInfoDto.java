package co.codewizards.cloudstore.rest.client.request;

import co.codewizards.cloudstore.core.dto.VersionInfoDto;

public class GetVersionInfoDto extends AbstractRequest<VersionInfoDto> {

	public GetVersionInfoDto() {
	}

	@Override
	public VersionInfoDto execute() {
		final VersionInfoDto versionInfoDto = createWebTarget(getPath(VersionInfoDto.class))
				.request().get(VersionInfoDto.class);
		return versionInfoDto;
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}

}
