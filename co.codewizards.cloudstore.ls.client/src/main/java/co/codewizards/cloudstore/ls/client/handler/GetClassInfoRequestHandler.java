package co.codewizards.cloudstore.ls.client.handler;

import java.util.Set;

import co.codewizards.cloudstore.ls.core.invoke.ClassManager;
import co.codewizards.cloudstore.ls.core.invoke.GetClassInfoRequest;
import co.codewizards.cloudstore.ls.core.invoke.GetClassInfoResponse;

public class GetClassInfoRequestHandler extends AbstractInverseServiceRequestHandler<GetClassInfoRequest, GetClassInfoResponse> {

	@Override
	public GetClassInfoResponse handle(final GetClassInfoRequest request) {
		final ClassManager classManager = getLocalServerClient().getObjectManager().getClassManager();
		final int classId = request.getClassId();
		final Class<?> clazz = classManager.getClass(classId);

		if (clazz == null)
			return null;

		final Set<String> interfaceNames = classManager.getInterfaceNames(clazz);
		return new GetClassInfoResponse(request, classId, clazz.getName(), interfaceNames);
	}
}
