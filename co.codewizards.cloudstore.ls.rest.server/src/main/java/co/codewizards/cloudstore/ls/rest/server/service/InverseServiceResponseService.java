package co.codewizards.cloudstore.ls.rest.server.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import co.codewizards.cloudstore.ls.core.dto.InverseServiceResponse;
import co.codewizards.cloudstore.ls.core.provider.MediaTypeConst;

@Path("InverseServiceResponse")
@Consumes(MediaTypeConst.APPLICATION_JAVA_NATIVE)
@Produces(MediaTypeConst.APPLICATION_JAVA_NATIVE)
public class InverseServiceResponseService extends AbstractService {

	@POST
	public void pushInverseServiceResponse(InverseServiceResponse response) {
		getInverseInvoker().pushInverseServiceResponse(response);
	}
}
