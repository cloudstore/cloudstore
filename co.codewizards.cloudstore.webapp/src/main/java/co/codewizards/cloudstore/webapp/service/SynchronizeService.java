package co.codewizards.cloudstore.webapp.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("validate")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class SynchronizeService
{
	private static final Logger logger = LoggerFactory.getLogger(SynchronizeService.class);

	{
		logger.debug("<init>: created new instance");
	}

//	@Context
//	private RepositoryDAO licenceDAO;
//
//	@Context
//	private HttpServletRequest httpServletRequest;
//
//	@GET
//	public RepositoryValidationResponse test() throws IllegalAccessException, InvocationTargetException, IOException
//	{
//		return validate(new RepositoryValidationRequest("productID12345", "user@nightlabs.de", "licenceKey1234567890"));
//	}
//
//	@POST
//	public RepositoryValidationResponse validate(RepositoryValidationRequest request)
//			throws IllegalAccessException, InvocationTargetException, IOException
//	{
//		logger.debug("validate: entered: request={}", request);
//
//		if (request.getProductID() == null)
//			throw new IllegalArgumentException("request.productID == null");
//
//		if (request.getEmail() == null)
//			throw new IllegalArgumentException("request.email == null");
//
//		if (request.getRepositoryKey() == null)
//			throw new IllegalArgumentException("request.licenceKey == null");
//
//		Repository licence = licenceDAO.getRepository(request.getProductID(), request.getEmail(), request.getRepositoryKey());
//		if (licence == null) {
//			logger.debug("validate: Repository not found locally. Querying payproglobal.");
//
//			licence = validateAtPayProGlobal(request);
//		}
//
//		RepositoryValidationResponse result = new RepositoryValidationResponse();
//		if (licence != null) {
////			BeanUtils.copyProperties(result, licence);
////			org.nightlabs.licence.client.RemoteException<org.apache.commons.beanutils.ConversionException>: No value specified for 'Date'
////			at org.apache.commons.beanutils.converters.AbstractConverter.handleMissing(AbstractConverter.java:310)
////			at org.apache.commons.beanutils.converters.AbstractConverter.convert(AbstractConverter.java:136)
////			at org.apache.commons.beanutils.converters.ConverterFacade.convert(ConverterFacade.java:60)
////			at org.apache.commons.beanutils.BeanUtilsBean.convert(BeanUtilsBean.java:1078)
////			at org.apache.commons.beanutils.BeanUtilsBean.copyProperty(BeanUtilsBean.java:437)
////			at org.apache.commons.beanutils.BeanUtilsBean.copyProperties(BeanUtilsBean.java:286)
////			at org.apache.commons.beanutils.BeanUtils.copyProperties(BeanUtils.java:137)
////			at org.nightlabs.licence.webapp.service.ValidateService.validate(ValidateService.java:78)
////			...
//			result.setCustomerEmail(licence.getCustomerEmail());
//			result.setCustomerName(licence.getCustomerName());
//			result.setExpiryDate(licence.getExpiryDate());
//			result.setRepositoryKey(licence.getRepositoryKey());
//			result.setOrderReferenceID(licence.getOrderReferenceID());
//			result.setProductID(licence.getProductID());
//			result.setProductName(licence.getProductName());
//			result.setPurchaseDate(licence.getPurchaseDate());
//			result.setQuantityLeft(licence.getQuantityLeft());
//			result.setTotalQuantity(licence.getTotalQuantity());
//
//			if (licence.getExpiryDate() == null || licence.getExpiryDate().after(new Date()))
//				result.setValid(true);
//		}
//
//		return result;
//	}
//
//	private Repository validateAtPayProGlobal(RepositoryValidationRequest request)
//			throws IllegalAccessException, InvocationTargetException, IOException
//	{
//		int numericProductID;
//		try {
//			numericProductID = Integer.parseInt(request.getProductID());
//		} catch (NumberFormatException x) {
//			logger.warn("validate: request.productID='{}' is not a valid integer and therefore no valid payproglobal-productID.", request.getProductID());
//			return null;
//		}
//
//		LicenseInfo licenseInfo;
//
//		Passport passport = new Passport();
//		PassportSoap passportSoap = passport.getPassportSoap();
//		String licenceValidationResult = passportSoap.validateLicense(numericProductID, request.getEmail(), request.getRepositoryKey());
//
//		logger.trace("checkRepository_queryServer: PassportSoap.validateLicense() returned: {}", licenceValidationResult);
//
//		licenseInfo = new LicenseInfoIO().read(licenceValidationResult);
//
//		if (licenseInfo.getValid() == null)
//			throw new IOException("PassportSoap.validateLicense(...) returned illegal result: licenseInfo.getValid() == null");
//
//		boolean licenceValid = licenseInfo.getValid().booleanValue();
//
//		if (licenceValid) {
//			Repository licence = new Repository();
//			licence.setHttpRemoteAddress(httpServletRequest.getRemoteAddr());
//			licence.setCustomerEmail(licenseInfo.getCustomerEmail());
//			licence.setCustomerName(licenseInfo.getCustomerName());
//			licence.setRepositoryKey(licenseInfo.getKey());
//			licence.setOrderReferenceID(licenseInfo.getOrderReferenceNumber());
//			licence.setProductID(request.getProductID());
//			licence.setProductName(licenseInfo.getProductName());
////			licence.setPurchaseDate(licenseInfo.getPurchaseDate()); // TODO handle this!
//			licence.setTotalQuantity(licenseInfo.getTotalUsages());
//			licence.setQuantityLeft(licenseInfo.getUsagesLeft());
//			licence = licenceDAO.makePersistent(licence);
//			return licence;
//		}
//		return null;
//	}
}
