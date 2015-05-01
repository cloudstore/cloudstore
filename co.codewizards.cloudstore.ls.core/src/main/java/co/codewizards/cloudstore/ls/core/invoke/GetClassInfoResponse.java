package co.codewizards.cloudstore.ls.core.invoke;

import java.util.Set;

import co.codewizards.cloudstore.ls.core.dto.AbstractInverseServiceResponse;

/**
 * Contains the class info for a certain {@code classId}.
 * @author mn
 */
public class GetClassInfoResponse extends AbstractInverseServiceResponse {
	private static final long serialVersionUID = 1L;

	private final ClassInfo classInfo;

	public GetClassInfoResponse(final GetClassInfoRequest request, final int classId, final String className, final Set<String> interfaceNames) {
		super(request);
		this.classInfo = new ClassInfo(classId, className, interfaceNames);
	}

	public ClassInfo getClassInfo() {
		return classInfo;
	}
}
