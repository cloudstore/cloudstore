package co.codewizards.cloudstore.ls.core.invoke.filter;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class InvocationFilterRegistry {

	private List<Class<? extends InvocationFilter>> invocationFilterClasses;

	private static final class Holder {
		public static final InvocationFilterRegistry instance = new InvocationFilterRegistry();
	}

	private InvocationFilterRegistry() {
	}

	public static InvocationFilterRegistry getInstance() {
		return Holder.instance;
	}

	private List<InvocationFilter> loadInvocationFilters() {
		final List<InvocationFilter> result = new ArrayList<>();

		final Iterator<InvocationFilter> iterator = ServiceLoader.load(InvocationFilter.class).iterator();
		while (iterator.hasNext())
			result.add(iterator.next());

		Collections.sort(result, new Comparator<InvocationFilter>() {
			@Override
			public int compare(InvocationFilter o1, InvocationFilter o2) {
				int result = -1 * Integer.compare(o1.getPriority(), o2.getPriority());
				if (result != 0)
					return result;

				result = o1.getClass().getName().compareTo(o2.getClass().getName());
				return result;
			}
		});
		return result;
	}

	protected List<Class<? extends InvocationFilter>> getInvocationFilterClasses() {
		if (invocationFilterClasses == null) {
			final List<InvocationFilter> invocationFilters = loadInvocationFilters();
			final List<Class<? extends InvocationFilter>> l = new ArrayList<>(invocationFilters.size());
			for (final InvocationFilter invocationFilter : invocationFilters)
				l.add(invocationFilter.getClass());

			invocationFilterClasses = Collections.unmodifiableList(l);
		}
		return invocationFilterClasses;
	}

	protected List<InvocationFilter> getInvocationFilters() {
		final List<Class<? extends InvocationFilter>> invocationFilterClasses = getInvocationFilterClasses();

		final List<InvocationFilter> result = new ArrayList<InvocationFilter>(invocationFilterClasses.size());
		for (Class<? extends InvocationFilter> invocationFilterClass : invocationFilterClasses)
			result.add(newInstance(invocationFilterClass));

		return Collections.unmodifiableList(result);
	}

	private <T> T newInstance(Class<T> clazz) {
		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean canInvoke(final ExtMethodInvocationRequest extMethodInvocationRequest) {
		requireNonNull(extMethodInvocationRequest, "extMethodInvocationRequest");
		for (final InvocationFilter invocationFilter : getInvocationFilters()) {
			final Boolean result = invocationFilter.canInvoke(extMethodInvocationRequest);
			if (result != null)
				return result;
		}
		return false;
	}

	public void assertCanInvoke(final ExtMethodInvocationRequest extMethodInvocationRequest) {
		if (! canInvoke(extMethodInvocationRequest))
			throw new SecurityException("Invocation denied: " + extMethodInvocationRequest.getMethodInvocationRequest());
	}
}
