package co.codewizards.cloudstore.core.version;

import static java.util.Objects.*;

import co.codewizards.cloudstore.core.dto.VersionInfoDto;

public class VersionCompatibilityException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final VersionInfoDto clientVersionInfoDto;
	private final VersionInfoDto serverVersionInfoDto;

	public VersionCompatibilityException(final VersionInfoDto clientVersionInfoDto, final VersionInfoDto serverVersionInfoDto, String message) {
		super(message);
		this.clientVersionInfoDto = requireNonNull(clientVersionInfoDto, "clientVersionInfoDto");
		this.serverVersionInfoDto = requireNonNull(serverVersionInfoDto, "serverVersionInfoDto");

		requireNonNull(clientVersionInfoDto.getLocalVersion(), "clientVersionInfoDto.localVersion");
		requireNonNull(clientVersionInfoDto.getMinimumRemoteVersion(), "clientVersionInfoDto.minimumRemoteVersion");

		requireNonNull(serverVersionInfoDto.getLocalVersion(), "serverVersionInfoDto.localVersion");
		requireNonNull(serverVersionInfoDto.getMinimumRemoteVersion(), "serverVersionInfoDto.minimumRemoteVersion");
	}

	public VersionInfoDto getClientVersionInfoDto() {
		return clientVersionInfoDto;
	}
	public VersionInfoDto getServerVersionInfoDto() {
		return serverVersionInfoDto;
	}
}
