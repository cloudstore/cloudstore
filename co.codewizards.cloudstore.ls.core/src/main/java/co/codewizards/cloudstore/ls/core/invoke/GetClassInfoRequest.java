package co.codewizards.cloudstore.ls.core.invoke;

import co.codewizards.cloudstore.ls.core.dto.AbstractInverseServiceRequest;

/**
 * Gets the class info for a classId.
 * @author mn
 */
public class GetClassInfoRequest extends AbstractInverseServiceRequest {
	private static final long serialVersionUID = 1L;

	private final int classId;

	public GetClassInfoRequest(final int classId) {
		this.classId = classId;
	}

	public int getClassId() {
		return classId;
	}

	@Override
	public boolean isTimeoutDeadly() {
		return true;
	}
}
