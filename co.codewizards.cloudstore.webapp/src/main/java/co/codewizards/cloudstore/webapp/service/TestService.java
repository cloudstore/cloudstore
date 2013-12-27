package co.codewizards.cloudstore.webapp.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("test")
@Consumes(MediaType.WILDCARD)
public class TestService
{
	private static final Logger logger = LoggerFactory.getLogger(TestService.class);

	{
		logger.debug("<init>: Instance created.");
	}

//	private static SecureRandom random = new SecureRandom();

//	@Context
//	private LicenceDAO licenceDAO;
//
//	@Context
//	private UserDAO userDAO;

	private @QueryParam("exception") boolean exception;

	@POST
	public String testPOST()
	{
		return test();
	}

	@GET
	public String test()
	{
//		logger.info("test: entered. exception={} pm={}", exception, pm);
//		logger.info("test: pm={} pm.currentTransaction.active={}", pm, pm.currentTransaction().isActive());
//		logger.info("test: licenceDAO={}", licenceDAO);
//		logger.info("test: userDAO={}", userDAO);
//
//		if (licenceDAO == null)
//			throw new IllegalStateException("Injection of licenceDAO failed!");
//
//		if (userDAO == null)
//			throw new IllegalStateException("Injection of userDAO failed!");
//
//		licenceDAO.getLicence("a", "b", "c");
////		Licence licence = new Licence();
////		licence.setProductID("product" + random.nextInt(10));
////		licence.setCustomerEmail("customer" + random.nextInt(1000) + "@domain.tld");
////
////		byte[] licenceKey = new byte[16];
////		random.nextBytes(licenceKey);
////		licence.setLicenceKey(Util.encodeHexStr(licenceKey));
////
////		pm.makePersistent(licence);
//
//		if (exception)
//			throw new RuntimeException("Test");

		return this.getClass().getName();
	}

}
