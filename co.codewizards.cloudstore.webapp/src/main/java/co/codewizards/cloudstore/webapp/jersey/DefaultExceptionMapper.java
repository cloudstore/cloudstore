//package co.codewizards.cloudstore.webapp.jersey;
//
//import javax.ws.rs.core.Context;
//import javax.ws.rs.core.Response;
//import javax.ws.rs.ext.ExceptionMapper;
//import javax.ws.rs.ext.Provider;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import co.codewizards.cloudstore.webapp.CloudStoreREST;
//
///**
// * @author Chairat Kongarayawetchakun - ckongarayawetchakun at nightlabs dot de
// */
//@Provider
//public class DefaultExceptionMapper implements ExceptionMapper<Throwable>
//{
//	private static final Logger logger = LoggerFactory.getLogger(DefaultExceptionMapper.class);
//
//	private PersistenceManagerProvider persistenceManagerProvider;
//
//	public DefaultExceptionMapper(@Context CloudStoreREST cloudStoreREST) {
//		logger.debug("<init>: Instance created. licenceApp={}", licenceREST);
//
//		if (licenceREST == null)
//			throw new IllegalArgumentException("licenceApp == null");
//
//		this.persistenceManagerProvider = licenceREST.getPersistenceManagerProvider();
//
//		if (persistenceManagerProvider == null)
//			throw new IllegalArgumentException("licenceApp.getPersistenceManagerProvider() == null");
//	}
//
//	@Override
//	public Response toResponse(Throwable throwable)
//	{
//		// We need to log the exception here, because it otherwise doesn't occur in any log
//		// in a vanilla tomcat 7.0.25. Marco :-)
//		logger.error(throwable.toString(), throwable);
//
//		persistenceManagerProvider.rollback();
//
//		Error error = new Error(throwable);
//		Error e = error;
//
//		Throwable t = throwable;
//		while (t != null) {
//			for (StackTraceElement stackTraceElement : t.getStackTrace()) {
//				e.getStackTraceElements().add(new ErrorStackTraceElement(stackTraceElement));
//			}
//
//			t = t.getCause();
//			if (t != null) {
//				Error oldE = e;
//				e = new org.nightlabs.licence.shared.Error(t);
//				oldE.setCause(e);
//			}
//		}
//		return Response
//				.status(Response.Status.INTERNAL_SERVER_ERROR)
//				.type(MediaType.APPLICATION_XML)
//				.entity(error)
//				.build();
//	}
//}
