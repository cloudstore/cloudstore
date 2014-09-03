package co.codewizards.cloudstore.core.concurrent;

import static java.lang.System.*;
import static org.assertj.core.api.Assertions.*;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallerBlocksPolicyTest {
	private static Logger logger = LoggerFactory.getLogger(CallerBlocksPolicyTest.class);

	private static Random random = new SecureRandom();

	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 3,
			60, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(2));
	{
		logger.debug("[{}]<init>", Integer.toHexString(identityHashCode(this)));
		executor.setRejectedExecutionHandler(new CallerBlocksPolicy());
	}

	@Test
	public void enqueueManyCallables() throws InterruptedException, ExecutionException {
		logger.debug("[{}]enqueueManyCallables: entered.", Integer.toHexString(identityHashCode(this)));
		final Set<Integer> indexesToDo = new HashSet<Integer>();
		final Set<Integer> indexesDone = new HashSet<Integer>();

		final List<Future<Void>> futures = new LinkedList<Future<Void>>();
		for (int i = 0; i < 10; ++i) {
			final int index = i;
			indexesToDo.add(index);

			logger.info("Submitting Callable[{}]...", index);
			final Future<Void> future = executor.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					logger.info("[{}].begin", index);
					Thread.sleep(1000 + random.nextInt(2000));
					logger.info("[{}].end", index);
					if (!indexesDone.add(index))
						throw new IllegalStateException("index already added before: " + index);

					return null;
				}
			});
			logger.info("Submitted Callable[{}].", index);
			futures.add(future);
		}
		logger.info("Waiting for all callables to finish...");
		for (final Future<Void> future : futures) {
			future.get();
		}
		logger.info("ALL DONE!");
		assertThat(indexesDone).isEqualTo(indexesToDo);
	}

}
