package co.codewizards.cloudstore.local.dbupdate;

import java.util.Properties;

import co.codewizards.cloudstore.core.util.PropertiesUtil;

/**
 * Database-update-step rewriting the {@code xxx-persistence.properties}-files for the switch from
 * DataNucleus 4.x to DataNucleus 5.x.
 * 
 * @author mangu
 */
public class DbUpdateStep003 extends AbstractDbUpdateStep {

	@Override
	public int getVersion() {
		return 3;
	}

	@Override
	public void performUpdate() throws Exception {
		Properties persistenceProperties = PropertiesUtil.load(getPersistencePropertiesFile());
		
		persistenceProperties.setProperty("datanucleus.schema.autoCreateDatabase", "true");
		persistenceProperties.setProperty("datanucleus.schema.autoCreateTables", "true");
		persistenceProperties.setProperty("datanucleus.schema.autoCreateColumns", "true");
		persistenceProperties.setProperty("datanucleus.schema.autoCreateConstraints", "true");
		
		persistenceProperties.remove("datanucleus.autoCreateSchema");
		persistenceProperties.remove("datanucleus.autoCreateTables");
		persistenceProperties.remove("datanucleus.autoCreateColumns");
		persistenceProperties.remove("datanucleus.autoCreateConstraints");
		
		
		persistenceProperties.setProperty("datanucleus.schema.validateTables", "true");
		persistenceProperties.setProperty("datanucleus.schema.validateColumns", "true");
		persistenceProperties.setProperty("datanucleus.schema.validateConstraints", "true");
		
		persistenceProperties.remove("datanucleus.validateTables");
		persistenceProperties.remove("datanucleus.validateColumns");
		persistenceProperties.remove("datanucleus.validateConstraints");
		
		PropertiesUtil.store(getPersistencePropertiesFile(), persistenceProperties, null);
	}
}
