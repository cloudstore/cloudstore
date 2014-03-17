package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.listener.AttachLifecycleListener;
import javax.jdo.listener.DeleteLifecycleListener;
import javax.jdo.listener.DirtyLifecycleListener;
import javax.jdo.listener.InstanceLifecycleEvent;
import javax.jdo.listener.StoreLifecycleListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.persistence.AutoTrackChanged;
import co.codewizards.cloudstore.core.persistence.AutoTrackLocalRevision;

/**
 * JDO lifecycle-listener updating the {@link AutoTrackChanged#getChanged() changed} and the
 * {@link AutoTrackLocalRevision#getLocalRevision() localRevision} properties of persistence-capable
 * objects.
 * <p>
 * Whenever an object is written to the datastore, said properties are updated, if the appropriate
 * interfaces are implemented by the persistence-capable object.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class AutoTrackLifecycleListener implements AttachLifecycleListener, StoreLifecycleListener, DirtyLifecycleListener, DeleteLifecycleListener {
	private static final Logger logger = LoggerFactory.getLogger(AutoTrackLifecycleListener.class);

	private final LocalRepoTransaction transaction;

	private final Map<Object, Date> oid2LastChanged = new HashMap<>();
	private boolean defer;

	public AutoTrackLifecycleListener(LocalRepoTransaction transaction) {
		this.transaction = assertNotNull("transaction", transaction);
	}

	public LocalRepoTransaction getTransaction() {
		return transaction;
	}

	@Override
	public void preStore(final InstanceLifecycleEvent event) {
		onWrite(event.getPersistentInstance());
	}

	@Override
	public void postStore(final InstanceLifecycleEvent event) { }

	@Override
	public void preDirty(final InstanceLifecycleEvent event) { }

	@Override
	public void postDirty(final InstanceLifecycleEvent event) {
		// We must use postDirty(...), because preDirty(...) causes a StackOverflowError. preDirty(...) seems to be
		// called again for the same object until it is dirty (IIUC). Thus, our onWrite(...) recursively calls preDirty(...)
		// and itself again. We could - of course - work around this by tracking the recursion using a ThreadLocal, but
		// using postDirty(...) instead is far easier - and there's no difference for our use case, anyway.
		onWrite(event.getPersistentInstance());
	}

	@Override
	public void preAttach(final InstanceLifecycleEvent event) { }

	@Override
	public void postAttach(final InstanceLifecycleEvent event) {
		// We must write it after attaching, because the affected fields might not be detached.
		onWrite(event.getPersistentInstance());
	}

	@Override
	public void preDelete(final InstanceLifecycleEvent event) {
		// We want to ensure that the revision is incremented, even if we do not have any remote repository connected
		// (and thus no DeleteModification being created).
		transaction.getLocalRevision();

		final Object oid = JDOHelper.getObjectId(event.getPersistentInstance());
		oid2LastChanged.remove(oid);
	}

	@Override
	public void postDelete(final InstanceLifecycleEvent event) { }

	private void onWrite(final Object pc) {
		// We always obtain the localRevision - no matter, if the current write operation is on
		// an object implementing AutoTrackLocalRevision, because this causes incrementing of the
		// localRevision in the database (once per transaction).
		final long localRevision = transaction.getLocalRevision();

		final Date changed = new Date();
		final Object oid = JDOHelper.getObjectId(pc);
		if (!defer && oid != null) { // in preStore(...), there is no OID, yet.
			final Date oldLastChanged = oid2LastChanged.get(oid);
			oid2LastChanged.put(oid, changed); // always keep the newest changed-timestamp.

			if (oldLastChanged != null) {
				logger.debug("onWrite: skipping (already processed in this transaction): {}", pc);
				return; // already processed in this transaction.
			}
		}

		if (pc instanceof AutoTrackChanged) {
			logger.debug("onWrite: setChanged({}) for {}", changed, pc);
			final AutoTrackChanged entity = (AutoTrackChanged) pc;
			entity.setChanged(changed);
		}
		if (pc instanceof AutoTrackLocalRevision) {
			logger.debug("onWrite: setLocalRevision({}) for {}", localRevision, pc);
			final AutoTrackLocalRevision entity = (AutoTrackLocalRevision) pc;
			entity.setLocalRevision(localRevision);
		}
	}

	public void onBegin() {
		defer = true;
	}

	public void onCommit() {
		defer = false;
		final long start = System.currentTimeMillis();
		final PersistenceManager pm = transaction.getPersistenceManager();
		for (final Map.Entry<Object, Date> me : oid2LastChanged.entrySet()) {
			try {
				final Object pc = pm.getObjectById(me.getKey());
				if (pc instanceof AutoTrackChanged) {
					final Date changed = me.getValue();
					logger.debug("flush: setChanged({}) for {}", changed, pc);
					final AutoTrackChanged entity = (AutoTrackChanged) pc;
					entity.setChanged(changed);
				}
			} catch (JDOObjectNotFoundException x) {
				logger.warn("close: " + x, x);
			}
		}
		final int oid2LastChangedSize = oid2LastChanged.size();
		oid2LastChanged.clear();
		logger.info("close: Deferred operations took {} ms for {} entities.", System.currentTimeMillis() - start, oid2LastChangedSize);
	}

	public void onRollback() {
		defer = false;
		oid2LastChanged.clear();
	}
}
