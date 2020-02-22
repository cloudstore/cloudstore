package co.codewizards.cloudstore.local.dbupdate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.TreeMap;

public class DbUpdateStepRegistry {

	private List<DbUpdateStep> dbUpdateSteps;

	public List<DbUpdateStep> getDbUpdateSteps() {
		if (dbUpdateSteps == null) {
			ArrayList<DbUpdateStep> list = new ArrayList<DbUpdateStep>();
			for (DbUpdateStep dbUpdateStep : ServiceLoader.load(DbUpdateStep.class)) {
				int version = dbUpdateStep.getVersion();
				if (version < 1)
					throw new IllegalStateException(String.format("Implementation-error in %s: getVersion() == %d < 1",
							dbUpdateStep.getClass().getName(), version));

				list.add(dbUpdateStep);
			}

			if (list.isEmpty())
				throw new IllegalStateException("No DbUpdateStep found!");

			Collections.sort(list, new Comparator<DbUpdateStep>() {
				public int compare(DbUpdateStep o1, DbUpdateStep o2) {
					int res = Integer.compare(o1.getVersion(), o2.getVersion());
					if (res != 0)
						return res;
					
					res = Integer.compare(o1.getOrderHint(), o2.getOrderHint());
					if (res != 0)
						return res;
					
					res = o1.getClass().getName().compareTo(o2.getClass().getName());
					return res;
				}
			});
			
			list.trimToSize();
			dbUpdateSteps = Collections.unmodifiableList(list);
		}
		return dbUpdateSteps;
	}

	public SortedMap<Integer, List<DbUpdateStep>> getDbUpdateStepsAfter(int version) {
		SortedMap<Integer, List<DbUpdateStep>> result = new TreeMap<>();
		for (DbUpdateStep dbUpdateStep : getDbUpdateSteps()) {
			int v = dbUpdateStep.getVersion();
			if (v > version) {
				List<DbUpdateStep> list = result.get(v);
				if (list == null) {
					list = new ArrayList<DbUpdateStep>(2);
					result.put(v, list);
				}
				list.add(dbUpdateStep);
			}
		}
		return result;
	}

	public int getCurrentVersion() {
		List<DbUpdateStep> dbUpdateSteps = getDbUpdateSteps();
		DbUpdateStep lastDbUpdateStep = dbUpdateSteps.get(dbUpdateSteps.size() - 1);
		return lastDbUpdateStep.getVersion();
	}
}
