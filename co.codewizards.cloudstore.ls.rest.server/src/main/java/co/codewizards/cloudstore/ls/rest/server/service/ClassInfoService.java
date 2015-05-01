package co.codewizards.cloudstore.ls.rest.server.service;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import co.codewizards.cloudstore.ls.core.invoke.ClassInfo;
import co.codewizards.cloudstore.ls.core.invoke.ClassManager;
import co.codewizards.cloudstore.ls.core.provider.MediaTypeConst;

@Path("ClassInfo")
@Consumes(MediaTypeConst.APPLICATION_JAVA_NATIVE)
@Produces(MediaTypeConst.APPLICATION_JAVA_NATIVE)
public class ClassInfoService extends AbstractService {

	@GET
	@Path("{classId}")
	public ClassInfo getClassInfo(@PathParam("classId") int classId) {
		final ClassManager classManager = getObjectManager().getClassManager();
		final Class<?> clazz = classManager.getClass(classId);

		if (clazz == null)
			return null;

		final Set<String> interfaceNames = classManager.getInterfaceNames(clazz);
		return new ClassInfo(classId, clazz.getName(), interfaceNames);
	}

}
