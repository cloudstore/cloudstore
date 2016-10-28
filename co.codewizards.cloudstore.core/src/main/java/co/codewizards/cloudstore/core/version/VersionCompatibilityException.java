package co.codewizards.cloudstore.core.version;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import co.codewizards.cloudstore.core.dto.VersionInfoDto;

public class VersionCompatibilityException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final VersionInfoDto clientVersionInfoDto;
	private final VersionInfoDto serverVersionInfoDto;

	public VersionCompatibilityException(final VersionInfoDto clientVersionInfoDto, final VersionInfoDto serverVersionInfoDto, String message) {
		super(message);
		this.clientVersionInfoDto = assertNotNull("clientVersionInfoDto", clientVersionInfoDto);
		this.serverVersionInfoDto = assertNotNull("serverVersionInfoDto", serverVersionInfoDto);

		assertNotNull("clientVersionInfoDto.localVersion", clientVersionInfoDto.getLocalVersion());
		assertNotNull("clientVersionInfoDto.minimumRemoteVersion", clientVersionInfoDto.getMinimumRemoteVersion());

		assertNotNull("serverVersionInfoDto.localVersion", serverVersionInfoDto.getLocalVersion());
		assertNotNull("serverVersionInfoDto.minimumRemoteVersion", serverVersionInfoDto.getMinimumRemoteVersion());
	}

	public VersionInfoDto getClientVersionInfoDto() {
		return clientVersionInfoDto;
	}
	public VersionInfoDto getServerVersionInfoDto() {
		return serverVersionInfoDto;
	}
}
