package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;
import java.util.Set;

public class ClassInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private final int classId;

	private final String className;

	private final Set<String> interfaceNames;

	public ClassInfo(final int classId, final String className, final Set<String> interfaceNames) {
		this.classId = classId;
		this.className = assertNotNull("className", className);
		this.interfaceNames = assertNotNull("interfaceNames", interfaceNames);
	}

	public int getClassId() {
		return classId;
	}

	public String getClassName() {
		return className;
	}
	public Set<String> getInterfaceNames() {
		return interfaceNames;
	}
}
