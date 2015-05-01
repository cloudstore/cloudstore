package co.codewizards.cloudstore.ls.rest.client.request;

import javax.ws.rs.client.WebTarget;

import co.codewizards.cloudstore.ls.core.invoke.ClassInfo;
import co.codewizards.cloudstore.ls.core.provider.MediaTypeConst;

public class GetClassInfo extends AbstractRequest<ClassInfo> {
	private final int classId;

	public GetClassInfo(final int classId) {
		this.classId = classId;
	}

	@Override
	public ClassInfo execute() {
		final WebTarget webTarget = createWebTarget(getPath(ClassInfo.class), Integer.toString(classId));
		final ClassInfo classInfo = assignCredentials(webTarget.request(MediaTypeConst.APPLICATION_JAVA_NATIVE_TYPE)).get(ClassInfo.class);
		return classInfo;
	}

	@Override
	public boolean isResultNullable() {
		return true;
	}
}
