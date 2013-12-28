package co.codewizards.cloudstore.shared.repo;

import static co.codewizards.cloudstore.shared.util.Util.*;

import java.util.Date;

import javax.jdo.listener.InstanceLifecycleEvent;
import javax.jdo.listener.StoreLifecycleListener;

import co.codewizards.cloudstore.shared.persistence.AutoTrackChanged;
import co.codewizards.cloudstore.shared.persistence.AutoTrackLocalRevision;

/**
 * JDO lifecycle-listener updating the {@link AutoTrackChanged#getChanged() changed} and the
 * {@link AutoTrackLocalRevision#getLocalRevision() localRevision} properties of persistence-capable
 * objects.
 * <p>
 * Whenever an object is written to the datastore, said properties are updated, if the appropriate
 * interfaces are implemented by the persistence-capable object.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class AutoTrackLifecycleListener implements StoreLifecycleListener {

	private RepositoryTransaction transaction;

	public AutoTrackLifecycleListener(RepositoryTransaction transaction) {
		this.transaction = assertNotNull("transaction", transaction);
	}

	public RepositoryTransaction getTransaction() {
		return transaction;
	}

	@Override
	public void preStore(InstanceLifecycleEvent event) {
		Object pc = event.getPersistentInstance();
		if (pc instanceof AutoTrackChanged) {
			AutoTrackChanged entity = (AutoTrackChanged) pc;
			entity.setChanged(new Date());
		}
		if (pc instanceof AutoTrackLocalRevision) {
			AutoTrackLocalRevision entity = (AutoTrackLocalRevision) pc;
			entity.setLocalRevision(transaction.getLocalRevision());
		}
	}

	@Override
	public void postStore(InstanceLifecycleEvent event) { }

}
