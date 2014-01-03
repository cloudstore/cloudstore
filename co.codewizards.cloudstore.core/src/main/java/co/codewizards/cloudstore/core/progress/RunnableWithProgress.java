package co.codewizards.cloudstore.core.progress;

public interface RunnableWithProgress {
	void run(ProgressMonitor monitor) throws Exception;
}
