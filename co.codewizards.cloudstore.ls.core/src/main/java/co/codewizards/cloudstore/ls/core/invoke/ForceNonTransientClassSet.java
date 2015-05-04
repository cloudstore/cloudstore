package co.codewizards.cloudstore.ls.core.invoke;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

public class ForceNonTransientClassSet {

	private final Set<Class<?>> forceNonTransientClasses;
	private final Map<Class<?>, Boolean> class2ForceNonTransient = new HashMap<>();

	private static class Holder {
		public static ForceNonTransientClassSet instance = new ForceNonTransientClassSet();
	}

	private ForceNonTransientClassSet() {
		Set<Class<?>> s = new HashSet<Class<?>>();

		final ArrayList<ForceNonTransientAdvisor> l = new ArrayList<>();
		for (Iterator<ForceNonTransientAdvisor> it = ServiceLoader.load(ForceNonTransientAdvisor.class).iterator(); it.hasNext(); ) {
			final ForceNonTransientAdvisor advisor = it.next();
			final Class<?>[] classes = advisor.getForceNonTransientClasses();
			if (classes == null)
				throw new IllegalStateException("Implementation error: advisor.getForceNonTransientClasses() returned null! " + advisor.getClass().getName());

			for (Class<?> clazz : classes)
				s.add(clazz);
		}

		forceNonTransientClasses = Collections.unmodifiableSet(s);
	}

	public synchronized boolean isForceNonTransientClass(Class<?> clazz) {
		Boolean result = class2ForceNonTransient.get(clazz);
		if (result == null) {
			for (final Class<?> forceNonTransientClass : forceNonTransientClasses) {
				if (forceNonTransientClass.isAssignableFrom(clazz)) {
					result = true;
					break;
				}
			}
			if (result == null)
				result = false;

			class2ForceNonTransient.put(clazz, result);
		}
		return result;
	}

	public static ForceNonTransientClassSet getInstance() {
		return Holder.instance;
	}
}
