package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.HashMap;
import java.util.Map;

public class ClassInfoCache {

	private final Map<Integer, ClassInfo> classId2ClassInfo = new HashMap<>();

	public ClassInfoCache() {
	}

	public synchronized ClassInfo getClassInfo(final int classId) {
		return classId2ClassInfo.get(classId);
	}

	public synchronized ClassInfo putClassInfo(final ClassInfo classInfo) {
		assertNotNull("classInfo", classInfo);
		return classId2ClassInfo.put(classInfo.getClassId(), classInfo);
	}
}
