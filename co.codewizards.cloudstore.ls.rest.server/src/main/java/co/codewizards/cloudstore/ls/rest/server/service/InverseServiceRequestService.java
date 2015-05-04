package co.codewizards.cloudstore.ls.rest.server.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import co.codewizards.cloudstore.ls.core.dto.InverseServiceRequest;
import co.codewizards.cloudstore.ls.core.provider.MediaTypeConst;

@Path("InverseServiceRequest")
@Consumes(MediaTypeConst.APPLICATION_JAVA_NATIVE_WITH_OBJECT_REF)
@Produces(MediaTypeConst.APPLICATION_JAVA_NATIVE_WITH_OBJECT_REF)
public class InverseServiceRequestService extends AbstractService {

	@POST
	public InverseServiceRequest pollInverseServiceRequest() {
		return getInverseInvoker().pollInverseServiceRequest();
	}
}
