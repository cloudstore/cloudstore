//package co.codewizards.cloudstore.webapp;
//
//import java.io.File;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.Set;
//
//import javax.jdo.PersistenceManager;
//import javax.jdo.PersistenceManagerFactory;
//import javax.ws.rs.ApplicationPath;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import co.codewizards.cloudstore.webapp.jersey.DefaultExceptionMapper;
//import co.codewizards.cloudstore.webapp.service.TestService;
//
//import com.sun.jersey.api.core.DefaultResourceConfig;
//
///**
// * @author Chairat Kongarayawetchakun - ckongarayawetchakun at nightlabs dot de
// */
//@ApplicationPath("CloudStoreREST")
//public class CloudStoreREST
////extends Application
//extends DefaultResourceConfig
//{
//	private static final Logger logger = LoggerFactory.getLogger(CloudStoreREST.class);
//	private static final String PERSISTENCE_PROPERTIES_FILE_NAME = "cloudStore-persistence.properties";
//
//	public static final String SYSTEM_PROPERTY_DATA_DIRECTORY = "org.nightlabs.cloudStore.webapp.CloudStoreREST.dataDirectory";
//	public static final boolean TEST_MODE = Boolean.parseBoolean(System.getProperty("org.nightlabs.cloudStore.webapp.CloudStoreREST.TEST_MODE"));
//
////	static {
////		logger.debug("<static_init>: Class loaded. TEST_MODE={}", TEST_MODE);
////	}
////
////	{
////		logger.debug("<init>: Instance created.");
////		getProperties().put("com.sun.jersey.spi.container.ContainerRequestFilters", JDOTransactionRequestFilter.class.getName());
////		getProperties().put("com.sun.jersey.spi.container.ContainerResponseFilters", JDOTransactionResponseFilter.class.getName());
//////		getProperties().put("com.sun.jersey.spi.container.ResourceFilters", value)
////	}
//
//	private static final Class<?>[] serviceClassesArray = {
//		// BEGIN services
//		TestService.class,
//		// END services
//
//		// BEGIN providers
//		// providers are not services (they are infrastructure), but they are registered the same way.
////		JAXBContextResolver.class
//		DefaultExceptionMapper.class
//		// END providers
//	};
//
//	private static final Set<Class<?>> serviceClassesSet;
//	static {
//		Set<Class<?>> s = new HashSet<Class<?>>(serviceClassesArray.length);
//		for (Class<?> c : serviceClassesArray)
//			s.add(c);
//
//		serviceClassesSet = Collections.unmodifiableSet(s);
//	}
//
//	@Override
//	public Set<Class<?>> getClasses() {
//		return serviceClassesSet;
//	}
//
//	private Set<Object> singletons;
//
//	private File dataDirectory;
//	private PersistenceManagerFactory persistenceManagerFactory;
////	private PersistenceManagerProvider persistenceManagerProvider;
//
//	@Override
//	public Set<Object> getSingletons()
//	{
////		if (singletons == null) {
////			initDataDirectory();
////			initUserSpecificCloudStorePersistencePropertiesFile();
////			persistenceManagerFactory = JDOHelper.getPersistenceManagerFactory(PropertiesHelper.loadProperties(PERSISTENCE_PROPERTIES_FILE_NAME, dataDirectory));
////			PersistenceManagerFactoryProvider persistenceManagerFactoryProvider = new PersistenceManagerFactoryProvider(persistenceManagerFactory);
////			persistenceManagerProvider = new PersistenceManagerProvider(persistenceManagerFactoryProvider);
////
////			Set<Object> s = new HashSet<Object>();
////			s.add(new CloudStoreRESTProvider(this));
////			s.add(persistenceManagerFactoryProvider);
////			s.add(persistenceManagerProvider);
////			s.add(new DAOProvider(this));
////
////			initDatastore();
////
////			SignatureKeyManager signatureKeyManager = initSignatureKeyManager();
////			s.add(new SignatureKeyManagerProvider(signatureKeyManager));
////			singletons = Collections.unmodifiableSet(s);
////		}
//
//		return singletons;
//	}
//
////	public PersistenceManagerProvider getPersistenceManagerProvider()
////	{
////		return persistenceManagerProvider;
////	}
//
//	private void initDataDirectory()
//	{
////		String dataDirectoryString = System.getProperty(SYSTEM_PROPERTY_DATA_DIRECTORY, "").trim();
////		if (dataDirectoryString.isEmpty()) {
////			dataDirectory = new File(IOUtil.getUserHome(), ".org.nightlabs.cloudStore");
////			logger.debug(
////					"initDataDirectory: System property \"{}\" is not specified or empty. Using default data directory: {}",
////					SYSTEM_PROPERTY_DATA_DIRECTORY, dataDirectory.getAbsolutePath()
////			);
////			System.setProperty(SYSTEM_PROPERTY_DATA_DIRECTORY, dataDirectory.getAbsolutePath());
////		}
////		else {
////			String dataDirectoryStringResolved = IOUtil.replaceTemplateVariables(dataDirectoryString, System.getProperties());
////			dataDirectory = new File(dataDirectoryStringResolved);
////			logger.debug(
////					"initDataDirectory: System property \"{}\" was specified. Using data directory: {}",
////					SYSTEM_PROPERTY_DATA_DIRECTORY, dataDirectory.getAbsolutePath()
////			);
////		}
////
////		if (!dataDirectory.isDirectory()) {
////			dataDirectory.mkdirs();
////
////			if (!dataDirectory.isDirectory())
////				throw new IllegalStateException("Directory does not exist and could not be created: " + dataDirectory.getAbsolutePath());
////		}
//	}
//
//	private void initUserSpecificCloudStorePersistencePropertiesFile()
//	{
////		try {
////			File userSpecificCloudStorePersistencePropertiesFile = new File(dataDirectory, PERSISTENCE_PROPERTIES_FILE_NAME);
////			if (!userSpecificCloudStorePersistencePropertiesFile.exists()) {
////				IOUtil.writeTextFile(
////						userSpecificCloudStorePersistencePropertiesFile,
////						""
////								+ "## This '" + PERSISTENCE_PROPERTIES_FILE_NAME + "' file can be modified to override the\n"
////								+ "## default settings. It is merged into the configuration.\n"
////								+ "##\n"
////								+ "## For example, let's assume the defaults contain this property:\n"
////								+ "##\n"
////								+ "## some.property = aa\n"
////								+ "##\n"
////								+ "## To override the value 'aa' with 'bb', you simply put the following line here:\n"
////								+ "##\n"
////								+ "## some.property = bb\n"
////								+ "##\n"
////								+ "## To override the value with null (just as if the property would not exist),\n"
////								+ "## you can *not* write the following:\n"
////								+ "##\n"
////								+ "## some.property =\n"
////								+ "##\n"
////								+ "## Simply leaving the value empty like this will override the property's value\n"
////								+ "## with an empty string instead of null (which is equivalent to removing it from\n"
////								+ "## the map). For a null value, you add the following meta-property (2nd line):\n"
////								+ "##\n"
////								+ "## some.property = cc\n"
////								+ "## some.property.null = true\n"
////								+ "##\n"
////								+ "## The first line is not necessary - it's just there to illustrate that the\n"
////								+ "## *.null-meta-property has priority and the 'cc' is ignored.\n"
////								+ "\n"
////				);
////			}
////		} catch (IOException x) {
////			throw new RuntimeException(x);
////		}
//	}
//
//	private void initDatastore()
//	{
////		logger.trace("initDatastore: entered.");
////
////		PersistenceManager pm = persistenceManagerFactory.getPersistenceManager();
////		try {
////			pm.currentTransaction().begin();
////
////			pm.getExtent(CloudStore.class);
////			pm.getExtent(PayProCloudStore.class);
////
////			if (TEST_MODE)
////				createTestData(pm);
////
////			pm.currentTransaction().commit();
////		} finally {
////			if (pm.currentTransaction().isActive())
////				pm.currentTransaction().rollback();
////
////			pm.close();
////		}
//	}
//
//	private void createTestData(PersistenceManager pm)
//	{
////		logger.trace("createTestData: entered.");
////
////		{
////			final String testProductID = "productID12345";
////			final String testCustomerEmail = "user@nightlabs.de";
////			final String testCloudStoreKey = "cloudStoreKey1234567890";
////			CloudStoreDAO cloudStoreDAO = new CloudStoreDAO();
////			cloudStoreDAO.setPersistenceManager(pm);
////
////			CloudStore cloudStore = cloudStoreDAO.getCloudStore(testProductID, testCustomerEmail, testCloudStoreKey);
////			if (cloudStore != null) {
////				logger.trace("createTestData: Test data already exists. Leaving without any action.");
////				return;
////			}
////
////			cloudStore = new CloudStore();
////			cloudStore.setProductID(testProductID);
////			cloudStore.setCustomerEmail(testCustomerEmail);
////			cloudStore.setCloudStoreKey(testCloudStoreKey);
////			cloudStore.setCustomerName("Emil Müller");
////			cloudStore.setOrderReferenceID("orderRef12345");
////			cloudStore.setProductName("Product Gaga");
////			cloudStore.setPurchaseDate(new Date());
////			cloudStore.setQuantityLeft(2);
////			cloudStore.setTotalQuantity(3);
////			pm.makePersistent(cloudStore);
////		}
////
////		SecureRandom random = new SecureRandom();
////		int testCloudStoreQty = 1000 + random.nextInt(1000);
////		for (int i = 0; i < testCloudStoreQty; ++i) {
////			CloudStore cloudStore = new CloudStore();
////			cloudStore.setProductID("product" + random.nextInt(10));
////			cloudStore.setCustomerEmail("customer" + random.nextInt(1000) + "@domain.tld");
////
////			byte[] cloudStoreKey = new byte[16];
////			random.nextBytes(cloudStoreKey);
////			cloudStore.setCloudStoreKey(Util.encodeHexStr(cloudStoreKey));
////
////			cloudStore.setCustomerName("Customer " + random.nextInt(1000));
////			cloudStore.setOrderReferenceID("orderRef12345");
////			cloudStore.setProductName("Product Gaga");
////			cloudStore.setPurchaseDate(new Date());
////			cloudStore.setTotalQuantity(random.nextInt(100));
////			cloudStore.setQuantityLeft(Math.min(random.nextInt(100), cloudStore.getTotalQuantity()));
////
//////			pm.makePersistent(cloudStore);
////
////			PayProCloudStore payProCloudStore = new PayProCloudStore();
////			payProCloudStore.setCloudStore(cloudStore);
////			payProCloudStore.setCustomerEmail(cloudStore.getCustomerEmail());
////			payProCloudStore.setCustomerName(cloudStore.getCustomerName());
////			pm.makePersistent(payProCloudStore);
////		}
////
////		logger.trace("createTestData: Test data created.");
//	}
//}
