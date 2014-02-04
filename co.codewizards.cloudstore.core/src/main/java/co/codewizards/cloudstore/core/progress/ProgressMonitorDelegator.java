package co.codewizards.cloudstore.core.progress;

import java.security.InvalidParameterException;

/**
 * Abstract base class for wrapper implementations of {@link ProgressMonitor}s.
 * <p>
 * You can have a look at {@link SubProgressMonitor} as an example.
 *
 * @author Marius Heinzmann [marius<at>NightLabs<dot>de]
 */
public abstract class ProgressMonitorDelegator implements ProgressMonitor
{
	private final ProgressMonitor monitor;

	public ProgressMonitorDelegator(ProgressMonitor monitor) {
		if (monitor == null)
			throw new InvalidParameterException("The wrapped monitor must not be null!");

		this.monitor = monitor;
	}

	@Override
	public void beginTask(String name, int totalWork) {
		monitor.beginTask(name, totalWork);
	}

	@Override
	public void done() {
		monitor.done();
	}

	@Override
	public void internalWorked(double worked) {
		monitor.internalWorked(worked);
	}

	@Override
	public boolean isCanceled() {
		return monitor.isCanceled();
	}

	@Override
	public void setCanceled(boolean canceled) {
		monitor.setCanceled(canceled);
	}

	@Override
	public void setTaskName(String name) {
		monitor.setTaskName(name);
	}

	@Override
	public void subTask(String name) {
		monitor.subTask(name);
	}

	@Override
	public void worked(int work) {
		monitor.worked(work);
	}

}
