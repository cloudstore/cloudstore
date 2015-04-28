//package co.codewizards.cloudstore.ls.rest.server.service;
//
//import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
//import static co.codewizards.cloudstore.core.util.AssertUtil.*;
//
//import java.io.IOException;
//
//import co.codewizards.cloudstore.core.oio.File;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
//import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
//import co.codewizards.cloudstore.core.util.AssertUtil;
//import co.codewizards.cloudstore.core.util.IOUtil;
//
//public abstract class AbstractServiceWithExistingLocalRepo {
//
//	private String local;
//
//	/** Must be an empty String ("") or start with the '/' character. */
//	private String localPathPrefix;
//
//	/**
//	 * {@link File} referencing a directory inside the repository (or its root).
//	 */
//	private File localFile;
//
//	/**
//	 * The root directory of the repository.
//	 * <p>
//	 * This may be the same as {@link #localFile} or it may be
//	 * a direct or indirect parent-directory of {@code #localFile}.
//	 */
//	private File localRoot;
//
//	protected String getLocal() {
//		return local;
//	}
//
//	protected void setLocal(final String local) throws IOException {
//		assertNotNull("local", local);
//
//		String repositoryName;
//		final int slashIndex = local.indexOf('/');
//		if (slashIndex < 0) {
//			repositoryName = local;
//			localPathPrefix = "";
//		}
//		else {
//			repositoryName = local.substring(0, slashIndex);
//			localPathPrefix = local.substring(slashIndex);
//
//			if (!localPathPrefix.startsWith("/"))
//				throw new IllegalStateException("localPathPrefix does not start with '/': " + localPathPrefix);
//		}
//
//		if ("/".equals(localPathPrefix))
//			localPathPrefix = "";
//
//		localRoot = LocalRepoRegistry.getInstance().getLocalRootForRepositoryName(repositoryName);
//		if (localRoot != null)
//			localFile = localPathPrefix.isEmpty() ? localRoot : createFile(localRoot, localPathPrefix);
//		else {
//			localFile = createFile(local).getAbsoluteFile();
//			localRoot = LocalRepoHelper.getLocalRootContainingFile(localFile);
//			if (localRoot == null)
//				localRoot = localFile;
//
//			if (localRoot.equals(localFile))
//				localPathPrefix = "";
//			else
//				localPathPrefix = IOUtil.getRelativePath(localRoot, localFile).replace(FILE_SEPARATOR_CHAR, '/');
//		}
//		assertLocalRootNotNull();
//	}
//
//	protected void assertLocalRootNotNull() {
//		AssertUtil.assertNotNull("localRoot", localRoot);
//	}
//}
