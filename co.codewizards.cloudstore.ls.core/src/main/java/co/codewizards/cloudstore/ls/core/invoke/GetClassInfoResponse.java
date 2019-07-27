package co.codewizards.cloudstore.ls.core.invoke;

import static java.util.Objects.*;

import co.codewizards.cloudstore.ls.core.dto.AbstractInverseServiceResponse;

/**
 * Contains the class info for a certain {@code classId}.
 * @author mn
 */
public class GetClassInfoResponse extends AbstractInverseServiceResponse {
	private static final long serialVersionUID = 1L;

	private final ClassInfo classInfo;

	public GetClassInfoResponse(final GetClassInfoRequest request, final ClassInfo classInfo) {
		super(request);
		this.classInfo = requireNonNull(classInfo, "classInfo");
	}

	public ClassInfo getClassInfo() {
		return classInfo;
	}
}
