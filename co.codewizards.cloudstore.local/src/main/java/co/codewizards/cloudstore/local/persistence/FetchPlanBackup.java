package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.LinkedHashSet;

import javax.jdo.FetchPlan;
import javax.jdo.PersistenceManager;

public class FetchPlanBackup {

	private LinkedHashSet<String> groups;
	private int fetchSize;
	private int maxFetchDepth;

	protected FetchPlanBackup() {
	}

	public static FetchPlanBackup createFrom(final PersistenceManager pm) {
		return createFrom(assertNotNull(pm, "pm").getFetchPlan());
	}

	public static FetchPlanBackup createFrom(final FetchPlan fetchPlan) {
		assertNotNull(fetchPlan, "fetchPlan");
		final FetchPlanBackup fetchPlanBackup = createObject(FetchPlanBackup.class);
		fetchPlanBackup.init(fetchPlan);
		return fetchPlanBackup;
	}

	@SuppressWarnings("unchecked")
	public void init(final FetchPlan fetchPlan) {
		assertNotNull(fetchPlan, "fetchPlan");
		this.groups = new LinkedHashSet<String>(fetchPlan.getGroups());
		this.fetchSize = fetchPlan.getFetchSize();
		this.maxFetchDepth = fetchPlan.getMaxFetchDepth();
	}

	public void restore(final PersistenceManager pm) {
		restore(assertNotNull(pm, "pm").getFetchPlan());
	}

	public void restore(final FetchPlan fetchPlan) {
		assertNotNull(fetchPlan, "fetchPlan");
		if (groups == null)
			throw new IllegalStateException("init(...) was not called!");

		fetchPlan.setGroups(groups);
		fetchPlan.setFetchSize(fetchSize);
		fetchPlan.setMaxFetchDepth(maxFetchDepth);
	}
}
