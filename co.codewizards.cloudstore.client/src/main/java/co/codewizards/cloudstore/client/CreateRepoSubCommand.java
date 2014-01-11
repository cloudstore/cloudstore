package co.codewizards.cloudstore.client;

import java.io.File;
import java.io.IOException;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;

/**
 * {@link SubCommand} implementation for creating a repository in the local file system.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class CreateRepoSubCommand extends SubCommand
{
	@Argument(metaVar="<localRoot>", required=false, usage="The path of the repository's root in the local file system. This must be an existing directory. If it does not exist and the '-createDir' option is set, it is automatically created. If it is not specified, the current working directory is used as local root.")
	private String localRoot;

	private File localRootFile;

	@Option(name="-createDir", required=false, usage="Whether to create the repository's root directory, if it does not yet exist. If specified, all parent-directories are created, too, if needed.")
	private boolean createDir;

	@Override
	public String getSubCommandName() {
		return "createRepo";
	}

	@Override
	public String getSubCommandDescription() {
		return "Create a new repository.";
	}

	@Override
	public void prepare() throws Exception {
		super.prepare();

		if (localRoot == null)
			localRootFile = new File("").getAbsoluteFile();
		else
			localRootFile = new File(localRoot).getAbsoluteFile();

		localRoot = localRootFile.getPath();
	}

	@Override
	public void run() throws Exception {
		if (!localRootFile.exists() && createDir) {
			localRootFile.mkdirs();

			if (!localRootFile.exists())
				throw new IOException("Could not create directory (permissions?): " + localRoot);
		}
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForNewRepository(localRootFile);
		localRepoManager.close(); // no try-finally needed, because we do nothing inbetween: only create and close ;-)

		new RepoInfoSubCommand(localRootFile).run();
	}
}
