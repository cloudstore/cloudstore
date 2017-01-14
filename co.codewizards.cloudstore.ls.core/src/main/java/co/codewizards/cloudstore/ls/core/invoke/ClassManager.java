package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.Uid;

public class ClassManager {
	private static final Logger logger = LoggerFactory.getLogger(ClassManager.class);

	private final Uid clientId;

	// classes on the side of the real objects (not the proxies!)
	private Map<Integer, Class<?>> classId2Class = new HashMap<>();
	private Map<Class<?>, Integer> class2ClassId = new HashMap<>();
	private Set<Integer> classIdsKnownByRemoteSide = new HashSet<>();

	// class-info on the side of the real objects (not the proxies!)
	// the ClassInfoMap is used on the proxies' side.
	private Map<Integer, ClassInfo> classId2ClassInfo = new HashMap<Integer, ClassInfo>();

	private static final Map<String, Class<?>> primitiveClassName2Class;
	static {
		final Class<?>[] primitives = {
				byte.class,
				short.class,
				int.class,
				long.class,
				float.class,
				double.class,
				char.class,
				boolean.class
		};

		final Map<String, Class<?>> m = new HashMap<>(primitives.length);

		for (Class<?> clazz : primitives)
			m.put(clazz.getName(), clazz);

		primitiveClassName2Class = Collections.unmodifiableMap(m);
	}
//	private static Set<Class<?>> primitiveClasses = Collections.unmodifiableSet(new HashSet<Class<?>>(primitiveClassName2Class.values()));
//
//	public static Set<Class<?>> getPrimitiveClasses() {
//		return primitiveClasses;
//	}

	public ClassManager(final Uid clientId) {
		this.clientId = assertNotNull(clientId, "clientId");
		logger.debug("[{}].<init>: Created ClassManager.", clientId);
	}

	private int nextClassId;

	public synchronized int getClassIdOrFail(final Class<?> clazz) {
		final int classId = getClassId(clazz);
		if (classId < 0)
			throw new IllegalArgumentException(String.format("ClassManager[%s] does not have classId for this class: %s",
					clientId, clazz.getName()));

		return classId;
	}

	public synchronized int getClassId(final Class<?> clazz) {
		assertNotNull(clazz, "clazz");
		Integer classId = class2ClassId.get(clazz);
		if (classId == null)
			return -1;
		else
			return classId;
	}

	public synchronized int getClassIdOrCreate(final Class<?> clazz) {
		assertNotNull(clazz, "clazz");
		Integer classId = class2ClassId.get(clazz);
		if (classId == null) {
			classId = nextClassId();
			logger.debug("[{}].getClassIdOrCreate: Assigned classId={} to {}.", clientId, classId, clazz.getName());
			class2ClassId.put(clazz, classId);
			classId2Class.put(classId, clazz);
		}
		return classId;
	}

	public synchronized Class<?> getClassOrFail(final int classId) {
		final Class<?> clazz = getClass(classId);
		if (clazz == null)
			throw new IllegalArgumentException(String.format("ClassManager[%s] does not have class for this classId: %s",
					clientId, classId));

		return clazz;
	}

	public synchronized Class<?> getClass(final int classId) {
		final Class<?> clazz = classId2Class.get(classId);
		return clazz;
	}

	public synchronized boolean isClassIdKnownByRemoteSide(int classId) {
		final boolean result = classIdsKnownByRemoteSide.contains(classId);
		return result;
	}

	public synchronized void setClassIdKnownByRemoteSide(int classId) {
		if (classIdsKnownByRemoteSide.add(classId))
			logger.debug("[{}].setClassIdKnownByRemoteSide: classId={}", clientId, classId);
	}

	public synchronized ClassInfo getClassInfo(int classId) {
		ClassInfo classInfo = classId2ClassInfo.get(classId);
		if (classInfo == null) {
			final Class<?> clazz = getClass(classId);

			if (clazz == null)
				return null;

			final Set<String> interfaceNames = getInterfaceNames(clazz);
			classInfo = new ClassInfo(classId, clazz.getName(), interfaceNames, isEqualsOverridden(clazz));
			classId2ClassInfo.put(classId, classInfo);
		}
		return classInfo;
	}

	private boolean isEqualsOverridden(final Class<?> clazz) {
		Class<?> c = clazz;
		while (c != Object.class) {
			try {
				c.getDeclaredMethod("equals", Object.class);
				return true;
			} catch (NoSuchMethodException | SecurityException e) {
				doNothing();
			}
			c = c.getSuperclass();
		}
		return false;
	}

	protected synchronized int nextClassId() {
		return nextClassId++;
	}

	protected Set<String> getInterfaceNames(Class<?> clazz) {
		assertNotNull(clazz, "clazz");
		final Set<String> interfaceNames = new LinkedHashSet<>();
		populateInterfaceNames(interfaceNames, clazz);
		return interfaceNames;
	}

	private void populateInterfaceNames(Set<String> interfaceNames, Class<?> clazz) {
		if (clazz.isInterface())
			interfaceNames.add(clazz.getName());

		for (Class<?> iface : clazz.getInterfaces())
			populateInterfaceNames(interfaceNames, iface);

		final Class<?> superclass = clazz.getSuperclass();
		if (superclass != Object.class && superclass != null)
			populateInterfaceNames(interfaceNames, superclass);
	}

	public Class<?>[] getClassesOrFail(final String[] classNames) {
		assertNotNull(classNames, "classNames");
		final Class<?>[] classes = new Class<?>[classNames.length];

		for (int i = 0; i < classNames.length; i++)
			classes[i] = getClassOrFail(classNames[i]);

		return classes;
	}

	public Class<?> getClassOrFail(final String className) {
		assertNotNull(className, "className");

		Class<?> clazz = primitiveClassName2Class.get(className);
		if (clazz != null)
			return clazz;

		// TODO maybe use context-class-loader, too and other loaders (which?)?
		try {
			clazz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
		return clazz;
	}
}
