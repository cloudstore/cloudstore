package co.codewizards.cloudstore.core.version;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import co.codewizards.cloudstore.core.dto.VersionInfoDto;

public class VersionCompatibilityValidator {

	protected VersionCompatibilityValidator() {
	}

	public static VersionCompatibilityValidator getInstance() {
		return createObject(VersionCompatibilityValidator.class);
	}

	public void validate(final VersionInfoDto clientVersionInfoDto, final VersionInfoDto serverVersionInfoDto)
	throws VersionCompatibilityException {
		assertNotNull(clientVersionInfoDto, "clientVersionInfoDto");
		assertNotNull(serverVersionInfoDto, "serverVersionInfoDto");

		final Version clientVersion = assertNotNull(clientVersionInfoDto.getLocalVersion(), "clientVersionInfoDto.localVersion");
		final Version minimumServerVersion = assertNotNull(clientVersionInfoDto.getMinimumRemoteVersion(), "clientVersionInfoDto.minimumRemoteVersion");

		final Version serverVersion = assertNotNull(serverVersionInfoDto.getLocalVersion(), "serverVersionInfoDto.localVersion");
		final Version minimumClientVersion = assertNotNull(serverVersionInfoDto.getMinimumRemoteVersion(), "serverVersionInfoDto.minimumRemoteVersion");

		if (serverVersion.compareTo(minimumServerVersion) < 0)
			throw new ServerTooOldException(clientVersionInfoDto, serverVersionInfoDto,
					String.format("The server version is %s, but the client requires at least server version %s!",
							serverVersion, minimumServerVersion));

		if (clientVersion.compareTo(minimumClientVersion) < 0)
			throw new ClientTooOldException(clientVersionInfoDto, serverVersionInfoDto,
					String.format("The client version is %s, but the server requires at least client version %s!",
							clientVersion, minimumClientVersion));
	}
}
