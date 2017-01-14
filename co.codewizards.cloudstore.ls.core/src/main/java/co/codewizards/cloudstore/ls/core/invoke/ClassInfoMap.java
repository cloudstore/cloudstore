package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.HashMap;
import java.util.Map;

public class ClassInfoMap {

	// class-info on the proxies' side. the ClassManager is used on the real objects' side.
	private final Map<Integer, ClassInfo> classId2ClassInfo = new HashMap<>();

	public ClassInfoMap() {
	}

	public ClassInfo getClassInfoOrFail(int classId) {
		final ClassInfo classInfo = getClassInfo(classId);
		if (classInfo == null)
			throw new IllegalArgumentException("There is no ClassInfo for classId=" + classId);

		return classInfo;
	}

	public synchronized ClassInfo getClassInfo(final int classId) {
		return classId2ClassInfo.get(classId);
	}

	public synchronized ClassInfo putClassInfo(final ClassInfo classInfo) {
		assertNotNull(classInfo, "classInfo");
		return classId2ClassInfo.put(classInfo.getClassId(), classInfo);
	}
}
