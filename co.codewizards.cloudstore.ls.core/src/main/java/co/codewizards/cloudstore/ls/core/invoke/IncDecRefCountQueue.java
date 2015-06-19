package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.ls.NoObjectRef;

public class IncDecRefCountQueue {

	private static final Logger logger = LoggerFactory.getLogger(IncDecRefCountQueue.class);

	private static final long INC_DEC_REF_COUNT_PERIOD_MS = 5 * 1000L;

	private final List<ObjectRefWithRefId> incEntries = Collections.synchronizedList(new LinkedList<ObjectRefWithRefId>());
	private final List<ObjectRefWithRefId> decEntries = Collections.synchronizedList(new LinkedList<ObjectRefWithRefId>());

	private final Timer incDecRefCountTimer = new Timer("incDecRefCountTimer-" + Integer.toHexString(System.identityHashCode(this)), true);
	private final TimerTask incDecRefCountTimerTask = new TimerTask() {
		@Override
		public void run() {
			try {
				final ObjectRefWithRefId[] incEntries = popIncEntries();
				if (incEntries.length > 0)
					invoker.invokeStatic(ObjectRef.class, ObjectRef.VIRTUAL_METHOD_NAME_INC_REF_COUNT, (Class<?>[])null, new Object[] { incEntries });
			} catch (final Exception x) {
				logger.error("incDecRefCountTimerTask.run: " + x, x);
			}

			try {
				final ObjectRefWithRefId[] decEntries = popDecEntries();
				if (decEntries.length > 0)
					invoker.invokeStatic(ObjectRef.class, ObjectRef.VIRTUAL_METHOD_NAME_DEC_REF_COUNT, (Class<?>[])null, new Object[] { decEntries });
			} catch (final Exception x) {
				logger.error("incDecRefCountTimerTask.run: " + x, x);
			}

			// TODO cancel this task, if there's nothing to do and re-schedule when needed.
		}
	};

	private ObjectRefWithRefId[] popIncEntries() { // an array has the same effect as an ArrayList-subclass being annotated with @NoObjectRef - and is more efficient
		final ObjectRefWithRefId[] result;
		synchronized (incEntries) {
			result = incEntries.toArray(new ObjectRefWithRefId[incEntries.size()]);
			incEntries.clear();
		}
		return result;
	}

	private ObjectRefWithRefId[] popDecEntries() { // an array has the same effect as an ArrayList-subclass being annotated with @NoObjectRef - and is more efficient
		final ObjectRefWithRefId[] result;
		synchronized (decEntries) {
			result = decEntries.toArray(new ObjectRefWithRefId[decEntries.size()]);
			decEntries.clear();
		}
		return result;
	}

	private final Invoker invoker;

	public IncDecRefCountQueue(final Invoker invoker) {
		this.invoker = assertNotNull("invoker", invoker);
		incDecRefCountTimer.schedule(incDecRefCountTimerTask, INC_DEC_REF_COUNT_PERIOD_MS, INC_DEC_REF_COUNT_PERIOD_MS);
	}

	public void incRefCount(final ObjectRef objectRef, final Uid refId) {
		incEntries.add(new ObjectRefWithRefId(objectRef, refId));
	}

	public void decRefCount(final ObjectRef objectRef, final Uid refId) {
		decEntries.add(new ObjectRefWithRefId(objectRef, refId));
	}

	@NoObjectRef(inheritToObjectGraphChildren = false)
	public static class NoObjectRefArrayList<E> extends ArrayList<E> {
		private static final long serialVersionUID = 1L;

		public NoObjectRefArrayList() {
		}

		public NoObjectRefArrayList(Collection<? extends E> c) {
			super(c);
		}

		public NoObjectRefArrayList(int initialCapacity) {
			super(initialCapacity);
		}
	}
}
