package co.codewizards.cloudstore.core.version;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static java.util.Objects.*;

import co.codewizards.cloudstore.core.dto.VersionInfoDto;

public class VersionCompatibilityValidator {

	protected VersionCompatibilityValidator() {
	}

	public static VersionCompatibilityValidator getInstance() {
		return createObject(VersionCompatibilityValidator.class);
	}

	public void validate(final VersionInfoDto clientVersionInfoDto, final VersionInfoDto serverVersionInfoDto)
	throws VersionCompatibilityException {
		requireNonNull(clientVersionInfoDto, "clientVersionInfoDto");
		requireNonNull(serverVersionInfoDto, "serverVersionInfoDto");

		final Version clientVersion = requireNonNull(clientVersionInfoDto.getLocalVersion(), "clientVersionInfoDto.localVersion");
		final Version minimumServerVersion = requireNonNull(clientVersionInfoDto.getMinimumRemoteVersion(), "clientVersionInfoDto.minimumRemoteVersion");

		final Version serverVersion = requireNonNull(serverVersionInfoDto.getLocalVersion(), "serverVersionInfoDto.localVersion");
		final Version minimumClientVersion = requireNonNull(serverVersionInfoDto.getMinimumRemoteVersion(), "serverVersionInfoDto.minimumRemoteVersion");

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
