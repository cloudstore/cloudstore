package co.codewizards.cloudstore.client;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.util.IOUtil;

/**
 * {@link SubCommand} implementation for creating a repository in the local file system.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class CreateRepoSubCommand extends SubCommand
{
	@Argument(metaVar="<localRoot>", required=true, usage="The path of the repository's root in the local file system. This must be an existing directory. If it does not exist and the '-createDir' option is set, it is automatically created.")
	private String localRoot;

	private File localRootFile;

	@Option(name="-createDir", required=false, usage="Whether to create the repository's root directory, if it does not yet exist. If specified, all parent-directories are created, too, if needed.")
	private boolean createDir;

	@Option(name="-noAlias", required=false, usage="Whether to suppress the automatic creation of a repository-alias with the local root's name.")
	private boolean noAlias;

	@Option(name="-alias", metaVar="<alias>", required=false, usage="Specify a different alias. By default, the root directory's name is used. For example, if the repository's root is '/home/user/Documents', the alias 'Documents' is chosen. If this option is present, the specified <alias> is used instead.")
	private String alias;

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

		if (!noAlias && (alias == null || alias.isEmpty())) { // empty alias means the same as alias not specified.
			String simplified = IOUtil.simplifyPath(localRootFile);
			alias = new File(simplified).getName();
		}

		if (alias != null && alias.isEmpty())
			alias = null;
	}

	@Override
	public void run() throws Exception {
		if (alias != null && noAlias) {
			System.err.println("ERROR: You specified both '-alias' and '-noAlias'. These options exclude each other and cannot be combined!");
			System.exit(101);
		}

		if (!localRootFile.exists() && createDir) {
			localRootFile.mkdirs();

			if (!localRootFile.exists())
				throw new IOException("Could not create directory (permissions?): " + localRoot);
		}

		LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForNewRepository(localRootFile);
		try {
			if (!noAlias && alias != null) {
				LocalRepoRegistry localRepoRegistry = LocalRepoRegistry.getInstance();
				UUID oldRepositoryId = localRepoRegistry.getRepositoryId(alias);

				File oldLocalRoot = null;
				if (oldRepositoryId != null) {
					oldLocalRoot = localRepoRegistry.getLocalRoot(oldRepositoryId);
					if (oldLocalRoot == null || !oldLocalRoot.exists()) {
						// orphaned entry to be ignored (should be cleaned up after a while, anyway)
						oldRepositoryId = null;
						oldLocalRoot = null;
					}
				}

				if (oldRepositoryId != null)
					System.err.println(String.format("WARNING: There is already a repository registered with the alias '%s'! Skipping automatic alias registration. The existing repository's ID is '%s' and its local-root is '%s'.", alias, oldRepositoryId, oldLocalRoot));
				else
					localRepoManager.putRepositoryAlias(alias);
			}
		} finally {
			localRepoManager.close();
		}

		new RepoInfoSubCommand(localRootFile).run();
	}
}
