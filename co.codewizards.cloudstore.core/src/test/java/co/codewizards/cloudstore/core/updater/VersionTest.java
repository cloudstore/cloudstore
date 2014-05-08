package co.codewizards.cloudstore.core.updater;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class VersionTest {

	@Test
	public void parseReleaseVersionWithoutPatchLevel() {
		final Version version = new Version("2.3.4");
		assertThat(version.getMajor()).isEqualTo(2);
		assertThat(version.getMinor()).isEqualTo(3);
		assertThat(version.getRelease()).isEqualTo(4);
		assertThat(version.getPatchLevel()).isEqualTo(0);
		assertThat(version.getSuffix()).isNull();
	}

	@Test
	public void parseSnapshotVersionWithoutPatchLevel() {
		final Version version = new Version("2.3.4-SNAPSHOT");
		assertThat(version.getMajor()).isEqualTo(2);
		assertThat(version.getMinor()).isEqualTo(3);
		assertThat(version.getRelease()).isEqualTo(4);
		assertThat(version.getPatchLevel()).isEqualTo(0);
		assertThat(version.getSuffix()).isEqualTo("SNAPSHOT");
	}

	@Test
	public void toStringReleaseVersionWithoutPatchLevel() {
		final String versionStr = "2.3.4";
		final Version version = new Version(versionStr);
		assertThat(version.toString()).isEqualTo(versionStr);
	}

	@Test
	public void toStringSnapshotVersionWithoutPatchLevel() {
		final String versionStr = "2.3.4-SNAPSHOT";
		final Version version = new Version(versionStr);
		assertThat(version.toString()).isEqualTo(versionStr);
	}

	@Test
	public void compareReleaseVersionsWithoutPatchLevel() {
		assertThat(new Version("2.3.4").compareTo(new Version("2.3.4"))).isZero();
		assertThat(new Version("2.3.4").compareTo(new Version("2.3.5"))).isNegative();
		assertThat(new Version("2.3.5").compareTo(new Version("2.3.4"))).isPositive();
	}

	@Test
	public void compareReleaseAndSnapshotVersionsWithoutPatchLevel() {
		assertThat(new Version("2.3.4-SNAPSHOT").compareTo(new Version("2.3.4-SNAPSHOT"))).isZero();
		assertThat(new Version("2.3.4-SNAPSHOT").compareTo(new Version("2.3.4"))).isNegative();
		assertThat(new Version("2.3.4").compareTo(new Version("2.3.4-SNAPSHOT"))).isPositive();
		assertThat(new Version("2.3.3").compareTo(new Version("2.3.4-SNAPSHOT"))).isNegative();
		assertThat(new Version("2.3.4").compareTo(new Version("2.3.5-SNAPSHOT"))).isNegative();
	}

	@Test
	public void compareVersionsWithIgnoredSuffixWithoutPatchLevel() {
		// We currently only take the "SNAPSHOT" suffix into account - all other suffixes are currently ignored.
		// Important: This might change later! If this ever changes, this test will break.
		assertThat(new Version("2.3.4-shouldbeignored").compareTo(new Version("2.3.4"))).isZero();
		assertThat(new Version("2.3.4").compareTo(new Version("2.3.4-shouldbeignored"))).isZero();
		assertThat(new Version("2.3.4-shouldbeignored").compareTo(new Version("2.3.4-doesnotmatter"))).isZero();
	}
}
