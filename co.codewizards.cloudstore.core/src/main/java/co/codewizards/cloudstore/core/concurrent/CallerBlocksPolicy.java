package co.codewizards.cloudstore.core.concurrent;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * A handler for rejected tasks that blocks until the rejected task
 * can be added to the queue.
 * <p>
 * The behaviour of an application using this policy is similar to one using the
 * {@link ThreadPoolExecutor.CallerRunsPolicy},
 * but instead of running a certain {@code Callable} on the main thread, which may block this thread
 * extremely long - much longer than the other {@code Callable}s run, the main thread already continues
 * as soon as any of the currently running tasks finished.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class CallerBlocksPolicy implements RejectedExecutionHandler {
    @Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        try {
			e.getQueue().put(r);
		} catch (InterruptedException x) {
			throw new RejectedExecutionException(x);
		}
    }
}
