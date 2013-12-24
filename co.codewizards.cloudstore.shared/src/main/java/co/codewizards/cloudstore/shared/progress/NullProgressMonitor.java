package co.codewizards.cloudstore.shared.progress;

/**
 * @author Alexander Bieber <!-- alex [AT] nightlabs [DOT] de -->
 */
public class NullProgressMonitor implements ProgressMonitor {

	private volatile boolean canceled;

	public NullProgressMonitor() { }

	public void beginTask(String name, int totalWork) { }

	public void done() { }

	public void setTaskName(String name) { }

	public void subTask(String name) { }

	public void worked(int work) { }

	public boolean isCanceled() {
		return canceled;
	}

	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

	public void internalWorked(double worked) { }
}
