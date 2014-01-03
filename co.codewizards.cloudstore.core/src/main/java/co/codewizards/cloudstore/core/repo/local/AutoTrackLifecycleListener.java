package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Date;

import javax.jdo.listener.AttachLifecycleListener;
import javax.jdo.listener.DeleteLifecycleListener;
import javax.jdo.listener.DirtyLifecycleListener;
import javax.jdo.listener.InstanceLifecycleEvent;
import javax.jdo.listener.StoreLifecycleListener;

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

	private final LocalRepoTransaction transaction;

	public AutoTrackLifecycleListener(LocalRepoTransaction transaction) {
		this.transaction = assertNotNull("transaction", transaction);
	}

	public LocalRepoTransaction getTransaction() {
		return transaction;
	}

	@Override
	public void preStore(InstanceLifecycleEvent event) {
		onWrite(event.getPersistentInstance());
	}

	@Override
	public void postStore(InstanceLifecycleEvent event) { }

	@Override
	public void preDirty(InstanceLifecycleEvent event) { }

	@Override
	public void postDirty(InstanceLifecycleEvent event) {
		// We must use postDirty(...), because preDirty(...) causes a StackOverflowError. preDirty(...) seems to be
		// called again for the same object until it is dirty (IIUC). Thus, our onWrite(...) recursively calls preDirty(...)
		// and itself again.
		onWrite(event.getPersistentInstance());
	}

	@Override
	public void preAttach(InstanceLifecycleEvent event) { }

	@Override
	public void postAttach(InstanceLifecycleEvent event) {
		// We must write it after attaching, because the affected fields might not be detached.
		onWrite(event.getPersistentInstance());
	}

	@Override
	public void preDelete(InstanceLifecycleEvent event) {
		// We want to ensure that the revision is incremented, even if we do not have any remote repository connected
		// (and thus no DeleteModification being created).
		transaction.getLocalRevision();
	}

	@Override
	public void postDelete(InstanceLifecycleEvent event) { }

	private void onWrite(Object pc) {
		// We always obtain the localRevision - no matter, if the current write operation is on
		// an object implementing AutoTrackLocalRevision, because this causes incrementing of the
		// localRevision in the database.
		long localRevision = transaction.getLocalRevision();

		if (pc instanceof AutoTrackChanged) {
			AutoTrackChanged entity = (AutoTrackChanged) pc;
			entity.setChanged(new Date());
		}
		if (pc instanceof AutoTrackLocalRevision) {
			AutoTrackLocalRevision entity = (AutoTrackLocalRevision) pc;
			entity.setLocalRevision(localRevision);
		}
	}
}
