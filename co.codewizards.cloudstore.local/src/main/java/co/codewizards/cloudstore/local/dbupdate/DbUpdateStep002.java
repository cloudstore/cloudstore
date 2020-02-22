package co.codewizards.cloudstore.local.dbupdate;

public class DbUpdateStep002 extends AbstractDbUpdateStep {

	@Override
	public int getVersion() {
		return 2;
	}

	@Override
	public void performUpdate() throws Exception {
		// Nothing to be done as we do not support upgrading from 1 to 2.
		// Repo-version 1 was an inofficial, never released cloudstore-version.
		throw new UnsupportedOperationException("Upgrading from 1 to 2 is not supported!");
	}
}
