package co.codewizards.cloudstore.core.repo.transport;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class RepoTransportFactoryRegistry {

	private static class RepoTransportFactoryRegistryHolder {
		public static final RepoTransportFactoryRegistry instance = new RepoTransportFactoryRegistry();
	}

	public static RepoTransportFactoryRegistry getInstance() {
		return RepoTransportFactoryRegistryHolder.instance;
	}

	protected RepoTransportFactoryRegistry() { }

	private List<RepoTransportFactory> repoTransportFactories;

	public RepoTransportFactory getRepoTransportFactoryOrFail(URL remoteRoot) {
		RepoTransportFactory repoTransportFactory = getRepoTransportFactory(remoteRoot);
		if (repoTransportFactory == null)
			throw new IllegalStateException("There is no RepoTransportFactory supporting this URL: " + remoteRoot);

		return repoTransportFactory;
	}

	public RepoTransportFactory getRepoTransportFactory(URL remoteRoot) {
		for (RepoTransportFactory factory : getRepoTransportFactories()) {
			if (factory.isSupported(remoteRoot))
				return factory;
		}
		return null;
	}

	public List<RepoTransportFactory> getRepoTransportFactories(URL remoteRoot) {
		List<RepoTransportFactory> result = new ArrayList<RepoTransportFactory>();
		for (RepoTransportFactory factory : getRepoTransportFactories()) {
			if (factory.isSupported(remoteRoot))
				result.add(factory);
		}
		return Collections.unmodifiableList(result);
	}

	public synchronized List<RepoTransportFactory> getRepoTransportFactories() {
		List<RepoTransportFactory> repoTransportFactories = this.repoTransportFactories;
		if (repoTransportFactories == null) {
			repoTransportFactories = loadRepoTransportFactoriesViaServiceLoader();
			sortRepoTransportFactories(repoTransportFactories);
			this.repoTransportFactories = repoTransportFactories = Collections.unmodifiableList(repoTransportFactories);
		}
		return repoTransportFactories;
	}

	private static List<RepoTransportFactory> loadRepoTransportFactoriesViaServiceLoader() {
		ArrayList<RepoTransportFactory> repoTransportFactories = new ArrayList<RepoTransportFactory>();
		ServiceLoader<RepoTransportFactory> sl = ServiceLoader.load(RepoTransportFactory.class);
		for (Iterator<RepoTransportFactory> it = sl.iterator(); it.hasNext(); ) {
			repoTransportFactories.add(it.next());
		}
		repoTransportFactories.trimToSize();
		return repoTransportFactories;
	}

	private static void sortRepoTransportFactories(List<RepoTransportFactory> repoTransportFactories) {
		Collections.sort(repoTransportFactories, new Comparator<RepoTransportFactory>() {
			@Override
			public int compare(RepoTransportFactory o1, RepoTransportFactory o2) {
				int result = -1 * Integer.compare(o1.getPriority(), o2.getPriority());
				if (result != 0)
					return result;

				String name1 = o1.getName() == null ? "" : o1.getName();
				String name2 = o2.getName() == null ? "" : o2.getName();
				result = name1.compareTo(name2);
				if (result != 0)
					return result;

				return o1.getClass().getName().compareTo(o2.getClass().getName());
			}
		});
	}
}
