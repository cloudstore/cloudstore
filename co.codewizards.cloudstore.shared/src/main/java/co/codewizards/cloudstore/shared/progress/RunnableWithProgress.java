package co.codewizards.cloudstore.shared.progress;

public interface RunnableWithProgress {
	void run(ProgressMonitor monitor) throws Exception;
}
