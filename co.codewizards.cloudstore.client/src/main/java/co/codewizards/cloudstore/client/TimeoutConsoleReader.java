package co.codewizards.cloudstore.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TimeoutConsoleReader {

	private final String question;
	private final long timeoutMsec;
	private final String timeoutAnswer;

	public TimeoutConsoleReader(final String question, final long timeoutMsec, final String timeoutAnswer) {
		this.question = question;
		this.timeoutMsec = timeoutMsec;
		this.timeoutAnswer = timeoutAnswer;
	}

	public String readLine() throws InterruptedException {
		final ExecutorService ex = Executors.newSingleThreadExecutor();
		String answer = null;
		try {
			final Future<String> future = ex.submit(new CallableReader());
			try {
				System.out.println(question);
				answer = future.get(timeoutMsec, TimeUnit.MILLISECONDS);
			} catch (final ExecutionException e) {
				//unexpected, print to System.err
				e.getCause().printStackTrace();
			} catch (final TimeoutException e) {
				System.err.println(">>> The question was without reply, will cancel.");
				future.cancel(true);
				System.err.println(">>> Giving no answer will be interpreted as: " + timeoutAnswer);
				answer = timeoutAnswer;
			}
		} finally {
			ex.shutdownNow();
		}
		return answer;
	}

	private class CallableReader implements Callable<String> {
		@Override
		public String call() throws IOException {
			final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String line = "";
			while ("".equals(line)) {
				try {
					while (!br.ready()) {
						Thread.sleep(200);
					}
					line = br.readLine();
				} catch (final InterruptedException e) {
					System.err.println("CallableReader() interrupted!");
					return timeoutAnswer;
				}
			}
			System.out.println("Your answer was: '" + line + "'");
			return line;
		}
	}
}
