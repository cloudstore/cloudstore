package co.codewizards.cloudstore.client;

import java.io.File;

import org.kohsuke.args4j.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;

/**
 * {@link SubCommand} implementation for syncing a repository locally.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class LocalSyncSubCommand extends SubCommand
{
	private static final Logger logger = LoggerFactory.getLogger(LocalSyncSubCommand.class);

	// TODO support sub-dirs!
	@Argument(metaVar="<local>", required=false, usage="A path inside a repository in the local file system. This may be the local repository's root or any directory inside it. If it is not specified, it defaults to the current working directory. NOTE: Sub-dirs are NOT YET SUPPORTED!")
	private String local;

	private File localFile;

	@Override
	public String getSubCommandName() {
		return "localSync";
	}

	@Override
	public String getSubCommandDescription() {
		return "Synchronise a repository locally. This updates the repository's meta-data to reflect the current contents of the file system (directories and files).";
	}

	@Override
	public void prepare() throws Exception {
		super.prepare();

		if (local == null)
			localFile = new File("").getAbsoluteFile();
		else
			localFile = new File(local).getAbsoluteFile();

		local = localFile.getPath();
	}

	@Override
	public void run() throws Exception {
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(localFile);
		try {
			localRepoManager.localSync(new LoggerProgressMonitor(logger));
		} finally {
			localRepoManager.close();
		}
	}
}
