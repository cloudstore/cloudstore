package co.codewizards.cloudstore.core.version;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;

import co.codewizards.cloudstore.core.dto.VersionInfoDto;
import co.codewizards.cloudstore.core.updater.CloudStoreUpdaterCore;

public class VersionInfoProvider {

	protected VersionInfoProvider() {
	}

	public static VersionInfoProvider getInstance() {
		return createObject(VersionInfoProvider.class);
	}

	public VersionInfoDto getVersionInfoDto() {
		final VersionInfoDto versionInfoDto = new VersionInfoDto();
		versionInfoDto.setLocalVersion(getLocalVersion());
		versionInfoDto.setMinimumRemoteVersion(getMinimumRemoteVersion());
		return versionInfoDto;
	}

	protected Version getLocalVersion() {
		return new CloudStoreUpdaterCore().getLocalVersion();
	}

	protected Version getMinimumRemoteVersion() {
		return new Version("0.9.12");
	}
}
