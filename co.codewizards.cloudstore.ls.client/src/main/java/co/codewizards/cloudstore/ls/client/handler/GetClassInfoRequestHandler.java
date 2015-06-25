package co.codewizards.cloudstore.ls.client.handler;

import co.codewizards.cloudstore.ls.core.invoke.GetClassInfoRequest;
import co.codewizards.cloudstore.ls.core.invoke.GetClassInfoResponse;

public class GetClassInfoRequestHandler extends AbstractInverseServiceRequestHandler<GetClassInfoRequest, GetClassInfoResponse> {

	@Override
	public GetClassInfoResponse handle(final GetClassInfoRequest request) throws Exception {
//		final ClassManager classManager = getLocalServerClient().getObjectManager().getClassManager();
//		final int classId = request.getClassId();
//		final ClassInfo classInfo = classManager.getClassInfo(classId);
//		return new GetClassInfoResponse(request, classInfo);
		throw new UnsupportedOperationException();
	}
}
