package co.codewizards.cloudstore.core.version;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import co.codewizards.cloudstore.core.dto.VersionInfoDto;

public class VersionCompatibilityException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final VersionInfoDto clientVersionInfoDto;
	private final VersionInfoDto serverVersionInfoDto;

	public VersionCompatibilityException(final VersionInfoDto clientVersionInfoDto, final VersionInfoDto serverVersionInfoDto, String message) {
		super(message);
		this.clientVersionInfoDto = assertNotNull(clientVersionInfoDto, "clientVersionInfoDto");
		this.serverVersionInfoDto = assertNotNull(serverVersionInfoDto, "serverVersionInfoDto");

		assertNotNull(clientVersionInfoDto.getLocalVersion(), "clientVersionInfoDto.localVersion");
		assertNotNull(clientVersionInfoDto.getMinimumRemoteVersion(), "clientVersionInfoDto.minimumRemoteVersion");

		assertNotNull(serverVersionInfoDto.getLocalVersion(), "serverVersionInfoDto.localVersion");
		assertNotNull(serverVersionInfoDto.getMinimumRemoteVersion(), "serverVersionInfoDto.minimumRemoteVersion");
	}

	public VersionInfoDto getClientVersionInfoDto() {
		return clientVersionInfoDto;
	}
	public VersionInfoDto getServerVersionInfoDto() {
		return serverVersionInfoDto;
	}
}
