package co.codewizards.cloudstore.core.version;

import co.codewizards.cloudstore.core.dto.VersionInfoDto;

public class ClientTooOldException extends VersionCompatibilityException {
	private static final long serialVersionUID = 1L;

	public ClientTooOldException(VersionInfoDto clientVersionInfoDto, VersionInfoDto serverVersionInfoDto, String message) {
		super(clientVersionInfoDto, serverVersionInfoDto, message);
	}
}
