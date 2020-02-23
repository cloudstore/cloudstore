package co.codewizards.cloudstore.local.dbupdate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;
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
		File persistencePropertiesFile = getPersistencePropertiesFile();

		// Before we change anything, we copy the original persisitence-properties-file into a backup-file.
		String dateString = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

		File persistencePropertiesBackup = persistencePropertiesFile.getParentFile().createFile(
				persistencePropertiesFile.getName() + "." + dateString + ".bak");

		IOUtil.copyFile(persistencePropertiesFile, persistencePropertiesBackup);

		Properties persistenceProperties = PropertiesUtil.load(persistencePropertiesFile);

		String connectionUrl = persistenceProperties.getProperty("javax.jdo.option.ConnectionURL");
		if (connectionUrl != null && connectionUrl.contains(":derby:")) {
			// At the moment, we officially support only derby. Hence, it should always copy the newer
			// template-file. This is much nicer than reading+writing a Properties object, which causes
			// all comments to be lost and which chaotically reorders all the settings.
			IOUtil.copyResource(this.getClass(), "/cloudstore-persistence.derby.properties",
					getPersistencePropertiesFile());
		}
		else {
			// In case, downstream-projects use sth. other than derby, we migrate the existing file,
			// instead of copying the template (which we don't know here). This is not beautiful,
			// but it should work (and I tested this code -- it really does work).
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
			
			PropertiesUtil.store(persistencePropertiesFile, persistenceProperties, null);
		}
	}
}
