package co.codewizards.cloudstore.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.version.Version;

@XmlRootElement
public class VersionInfoDto {

	private Version localVersion;

	private Version minimumRemoteVersion;

	/**
	 * Gets the version of the system which created this {@code VersionInfoDto}.
	 * <p>
	 * Thus, if the server is asked for its version, this property represents the
	 * server's version.
	 * @return the version of the system being asked for this instance. Should
	 * never be <code>null</code>.
	 */
	public Version getLocalVersion() {
		return localVersion;
	}
	public void setLocalVersion(Version localVersion) {
		this.localVersion = localVersion;
	}

	/**
	 * Gets the minimum version expected from its remote peer by the system which created this {@code VersionInfoDto}.
	 * <p>
	 * Thus, <b>if the server is asked</b> for this instance, this property represents <b>the
	 * client's minimum version</b> required.
	 * <p>
	 * If this {@code VersionInfoDto} was created by the client, this property represents the
	 * server's minimum version expected by the client.
	 * @return the minimum version expected by the asked system from its remote peer. Should
	 * never be <code>null</code>.
	 */
	public Version getMinimumRemoteVersion() {
		return minimumRemoteVersion;
	}
	public void setMinimumRemoteVersion(Version minimumRemoteVersion) {
		this.minimumRemoteVersion = minimumRemoteVersion;
	}

	@Override
	public String toString() {
		return VersionInfoDto.class.getSimpleName()
				+ "[localVersion=" + localVersion
				+ ", minimumRemoteVersion=" + minimumRemoteVersion
				+ ']';
	}
}
