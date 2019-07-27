package co.codewizards.cloudstore.ls.core.invoke;

import static java.util.Objects.*;

import java.io.Serializable;
import java.util.Set;

import co.codewizards.cloudstore.core.ls.NoObjectRef;

@NoObjectRef
public class ClassInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private final int classId;

	private final String className;

	private final Set<String> interfaceNames;

	private final boolean equalsOverridden;

	public ClassInfo(final int classId, final String className, final Set<String> interfaceNames, final boolean equalsOverridden) {
		this.classId = classId;
		this.className = requireNonNull(className, "className");
		this.interfaceNames = requireNonNull(interfaceNames, "interfaceNames");
		this.equalsOverridden = equalsOverridden;
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

	/**
	 * Is the {@link #equals(Object) equals(...)} method overridden?
	 * <p>
	 * <b>Important:</b> {@link #hashCode()} must always be overridden, if {@code equals(...)} is overridden and vice-versa!
	 * In other words, either both methods or none of them must be overridden. The information provided by this method is
	 * thus used for both methods: {@code equals(...)} <b>and</b> {@code hashCode()}!
	 * <p>
	 * If {@code false}, it is assumed that {@code equals(...)} means object-identity. This can - and will - be checked
	 * locally by the proxy itself. Invocations of {@code equals(...)} and {@code hashCode()} are thus <i>not</i> delegated
	 * to the real object, providing significant performance benefit.
	 * <p>
	 * If {@code true}, it is assumed that invocations of {@code equals(...)} and {@code hashCode()} must be delegated to the
	 * real object.
	 * @return whether the {@link #equals(Object) equals(...)} method is overridden.
	 */
	public boolean isEqualsOverridden() {
		return equalsOverridden;
	}
}
