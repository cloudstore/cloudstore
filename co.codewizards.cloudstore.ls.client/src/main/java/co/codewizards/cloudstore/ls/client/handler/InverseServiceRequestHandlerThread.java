package co.codewizards.cloudstore.ls.client.handler;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.ls.client.LocalServerClient;
import co.codewizards.cloudstore.ls.core.dto.ErrorResponse;
import co.codewizards.cloudstore.ls.core.dto.InverseServiceRequest;
import co.codewizards.cloudstore.ls.core.dto.InverseServiceResponse;
import co.codewizards.cloudstore.ls.core.dto.NullResponse;
import co.codewizards.cloudstore.ls.rest.client.LocalServerRestClient;
import co.codewizards.cloudstore.ls.rest.client.request.PollInverseServiceRequest;
import co.codewizards.cloudstore.ls.rest.client.request.PushInverseServiceResponse;

public class InverseServiceRequestHandlerThread extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(InverseServiceRequestHandlerThread.class);

	private static final AtomicInteger nextThreadId = new AtomicInteger();
	private volatile boolean interrupted;

	private final WeakReference<LocalServerClient> localServerClientRef;
	private final WeakReference<LocalServerRestClient> localServerRestClientRef;
	private final InverseServiceRequestHandlerManager inverseServiceRequestHandlerManager = InverseServiceRequestHandlerManager.getInstance();
	private final Executor executor = Executors.newCachedThreadPool();

	public InverseServiceRequestHandlerThread(final LocalServerClient localServerClient) {
		this.localServerClientRef = new WeakReference<LocalServerClient>(assertNotNull("localServerClient", localServerClient));
		this.localServerRestClientRef = new WeakReference<LocalServerRestClient>(assertNotNull("localServerRestClient", localServerClient.getLocalServerRestClient()));
		setName(getClass().getSimpleName() + '-' + nextThreadId.getAndIncrement());
		setDaemon(true);
	}

	@Override
	public void interrupt() {
		// We use our own field instead of isInterrupted() to make absolutely sure, we end the thread. The isInterrupted()
		// flag may be reset by an InterruptedException, while our flag cannot be reset.
		interrupted = true;
		super.interrupt();
	}

	@Override
	public boolean isInterrupted() {
		return interrupted || super.isInterrupted();
	}

	@Override
	public void run() {
		int consecutiveErrorCounter = 0;
		while (! isInterrupted()) {
			try {
				final InverseServiceRequest inverseServiceRequest = getLocalServerRestClientOrFail().execute(new PollInverseServiceRequest());
				if (inverseServiceRequest != null)
					executor.execute(new HandlerRunnable(inverseServiceRequest));

				consecutiveErrorCounter = 0;
			} catch (Exception x) {
				logger.error(x.toString(), x);

				// Wait a bit before retrying (increasingly longer) in order to prevent the log from filling up too quickly.
				// We wait 1 second longer after each consecutive error up to a maximum of 1 minute.
				consecutiveErrorCounter = Math.min(60, ++consecutiveErrorCounter);
				try { Thread.sleep(consecutiveErrorCounter * 1000L); } catch (Exception y) { doNothing(); }
			}
		}
	}

	private class HandlerRunnable implements Runnable {
		private final InverseServiceRequest inverseServiceRequest;

		public HandlerRunnable(final InverseServiceRequest inverseServiceRequest) {
			this.inverseServiceRequest = assertNotNull("inverseServiceRequest", inverseServiceRequest);
		}

		@Override
		public void run() {
			assertNotNull("inverseServiceRequest", inverseServiceRequest);
			final Uid requestId = inverseServiceRequest.getRequestId();
			assertNotNull("inverseServiceRequest.requestId", requestId);

			final LocalServerRestClient localServerRestClient = getLocalServerRestClientOrFail();

			InverseServiceResponse inverseServiceResponse = null;
			try {
				@SuppressWarnings("unchecked")
				final InverseServiceRequestHandler<InverseServiceRequest, InverseServiceResponse> handler = inverseServiceRequestHandlerManager.getInverseServiceRequestHandlerOrFail(inverseServiceRequest);

				final LocalServerClient localServerClient = getLocalServerClientOrFail();

				handler.setLocalServerClient(localServerClient);

				inverseServiceResponse = handler.handle(inverseServiceRequest);
				if (inverseServiceResponse == null)
					inverseServiceResponse = new NullResponse(requestId);

				if (!requestId.equals(inverseServiceResponse.getRequestId()))
					throw new IllegalStateException(String.format("Implementation error in %s: handle(...) returned a response with a requestId different from the request!", handler.getClass().getName()));

			} catch (final Exception x) {
				logger.warn("handleInverseServiceRequest: " + x, x);
				final ErrorResponse errorResponse = new ErrorResponse(requestId, new Error(x));
				localServerRestClient.execute(new PushInverseServiceResponse(errorResponse));
			}

			// Send this outside of the try-catch, because it might otherwise cause 2 responses for the same requestId to be delivered to the server.
			if (inverseServiceResponse != null)
				localServerRestClient.execute(new PushInverseServiceResponse(inverseServiceResponse));
		}
	}

	private LocalServerClient getLocalServerClientOrFail() {
		final LocalServerClient localServerClient = localServerClientRef.get();
		if (localServerClient == null)
			throw new IllegalStateException("LocalServerClient already garbage-collected!");

		return localServerClient;
	}

	private LocalServerRestClient getLocalServerRestClientOrFail() {
		final LocalServerRestClient localServerRestClient = localServerRestClientRef.get();
		if (localServerRestClient == null)
			throw new IllegalStateException("LocalServerRestClient already garbage-collected!");

		return localServerRestClient;
	}
}
