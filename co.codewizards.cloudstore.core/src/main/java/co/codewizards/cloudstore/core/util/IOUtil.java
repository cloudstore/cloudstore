package co.codewizards.cloudstore.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.progress.ProgressMonitor;

public final class IOUtil {
	/**
	 * UTF-8 caracter set name.
	 */
	public static final String CHARSET_NAME_UTF_8 = "UTF-8";

	/**
	 * UTF-8 caracter set.
	 */
	public static final Charset CHARSET_UTF_8 = Charset.forName(CHARSET_NAME_UTF_8);

	/**
	 * 1 GB in bytes.
	 * This holds the result of the calculation 1 * 1024 * 1024 * 1024
	 */
	public static final long GIGABYTE = 1 * 1024 * 1024 * 1024;

	private static File tempDir = null;

	private static final Logger logger = LoggerFactory.getLogger(IOUtil.class);

	private IOUtil() { }

	/**
	 * This method finds - if possible - a relative path for addressing
	 * the given <code>file</code> from the given <code>baseDir</code>.
	 * <p>
	 * If it is not possible to denote <code>file</code> relative to <code>baseDir</code>,
	 * the absolute path of <code>file</code> will be returned.
	 * </p>
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>
	 *   <code>baseDir="/home/marco"</code><br/>
	 *   <code>file="temp/jfire/jboss/bin/run.sh"</code><br/>
	 *     or<br/>
	 *   <code>file="/home/marco/temp/jfire/jboss/bin/run.sh"</code><br/>
	 *   <code>result="temp/jfire/jboss/bin/run.sh"</code>
	 * </li>
	 * <li>
	 *   <code>baseDir="/home/marco/workspace.jfire/JFireBase"</code><br/>
	 *   <code>file="/home/marco/temp/jfire/jboss/bin/run.sh"</code><br/>
	 *     or<br/>
	 *   <code>file="../../temp/jfire/jboss/bin/run.sh"</code><br/>
	 *   <code>result="../../temp/jfire/jboss/bin/run.sh"</code>
	 * </li>
	 * <li>
	 *   <code>baseDir="/tmp/workspace.jfire/JFireBase"</code><br/>
	 *   <code>file="/home/marco/temp/jfire/jboss/bin/run.sh"</code><br/>
	 *   <code>result="/home/marco/temp/jfire/jboss/bin/run.sh"</code> (absolute, because relative is not possible)
	 * </li>
	 * </ul>
	 * </p>
	 *
	 * @param baseDir This directory is used as start for the relative path. It can be seen as the working directory
	 *		from which to point to <code>file</code>.
	 * @param file The file to point to.
	 * @return the path to <code>file</code> relative to <code>baseDir</code> or the absolute path,
	 *		if a relative path cannot be formulated (i.e. they have no directory in common).
	 * @throws IOException In case of an error
	 */
	public static String getRelativePath(File baseDir, String file)
	throws IOException
	{
		// When passing the current working dir by "new File(".").getAbsoluteFile()" to this method, it
		// generates 2 "../" where there should be only one "../", because it counts the single "." at the end as subdirectory.
		// Therefore, we now call once simplifyPath before we start.
		// Maybe we don't need to call simplifyPath in _getRelativePath anymore. Need to check later. Marco.
		// Additionally, this method did not work without baseDir being an absolute path. Therefore, I now check and make it absolute, if it is not yet.
		if (baseDir.isAbsolute())
			baseDir = new File(simplifyPath(baseDir));
		else
			baseDir = new File(simplifyPath(baseDir.getAbsoluteFile()));


		File absFile;
		File tmpF = new File(file);
		if (tmpF.isAbsolute())
			absFile = tmpF;
		else
			absFile = new File(baseDir, file);

		File dest = absFile;
		File b = baseDir;
		String up = "";
		while (b.getParentFile() != null) {
			String res = _getRelativePath(b, dest.getAbsolutePath());
			if (res != null)
				return up + res;

//			up = "../" + up;
			up = ".." + File.separatorChar + up;
			b = b.getParentFile();
		}

		return absFile.getAbsolutePath();
	}

	/**
	 * This method finds - if possible - a relative path for addressing
	 * the given <code>file</code> from the given <code>baseDir</code>.
	 * <p>
	 * If it is not possible to denote <code>file</code> relative to <code>baseDir</code>,
	 * the absolute path of <code>file</code> will be returned.
	 * </p>
	 * See {@link getRelativePath} for examples.
	 *
	 * @param baseDir This directory is used as start for the relative path. It can be seen as the working directory
	 *		from which to point to <code>file</code>.
	 * @param file The file to point to.
	 * @return the path to <code>file</code> relative to <code>baseDir</code> or the absolute path,
	 *		if a relative path cannot be formulated (i.e. they have no directory in common).
	 * @throws IOException In case of an error
	 */
	public static String getRelativePath(File baseDir, File file)
	throws IOException
	{
		return getRelativePath(baseDir, file.getPath());
	}

	/**
	 * This method finds - if possible - a relative path for addressing
	 * the given <code>file</code> from the given <code>baseDir</code>.
	 * <p>
	 * If it is not possible to denote <code>file</code> relative to <code>baseDir</code>,
	 * the absolute path of <code>file</code> will be returned.
	 * </p>
	 * See {@link getRelativePath} for examples.
	 *
	 * @param baseDir This directory is used as start for the relative path. It can be seen as the working directory
	 *		from which to point to <code>file</code>.
	 * @param file The file to point to.
	 * @return the path to <code>file</code> relative to <code>baseDir</code> or the absolute path,
	 *		if a relative path cannot be formulated (i.e. they have no directory in common).
	 * @throws IOException In case of an error
	 */
	public static String getRelativePath(String baseDir, String file)
	throws IOException
	{
		return getRelativePath(new File(baseDir), file);
	}

	/**
	 * This method is a helper method used by {@link #getRelativePath(File, String) }. It will
	 * only return a relative path, if <code>file</code> is a child node of <code>baseDir</code>.
	 * Otherwise it returns <code>null</code>.
	 *
	 * @param baseDir The directory to which the resulting path will be relative.
	 * @param file The file to which the resulting path will point.
	 */
	private static String _getRelativePath(File baseDir, String file)
	throws IOException
	{
		// we make baseDir now absolute in getRelativePath(...)
//		if (!baseDir.isAbsolute())
//			throw new IllegalArgumentException("baseDir \""+baseDir.getPath()+"\" is not absolute!");

		File absFile;
		File tmpF = new File(file);
		if (tmpF.isAbsolute())
			absFile = tmpF;
		else
			absFile = new File(baseDir, file);

		String absFileStr = null;
		String baseDirStr = null;
		for (int mode_base = 0; mode_base < 2; ++mode_base) {
			switch (mode_base) {
				case 0:
					baseDirStr = simplifyPath(baseDir);
				break;
				case 1:
					baseDirStr = baseDir.getCanonicalPath();
				break;
				default:
					throw new IllegalStateException("this should never happen!");
			}

			for (int mode_abs = 0; mode_abs < 2; ++mode_abs) {
				baseDirStr = addFinalSlash(baseDirStr);

				switch (mode_abs) {
					case 0:
						absFileStr = simplifyPath(absFile);
					break;
					case 1:
						absFileStr = absFile.getCanonicalPath();
					break;
					default:
						throw new IllegalStateException("this should never happen!");
				}

				if (!absFileStr.startsWith(baseDirStr)) {
					if (mode_base >= 1 && mode_abs >= 1)
						return null;
						// throw new IllegalArgumentException("file \""+file+"\" is not a child of baseDir \""+baseDir.getCanonicalPath()+"\"!");
				}
				else
					break;
			} // for (int mode_abs = 0; mode_abs < 2; ++mode_abs) {
		} // for (int mode = 0; mode < 2; ++mode) {

		if (baseDirStr == null)
			throw new NullPointerException("baseDirStr == null");

		if (absFileStr == null)
			throw new NullPointerException("absFileStr == null");

		return absFileStr.substring(baseDirStr.length(), absFileStr.length());
	}

	/**
	 * This method removes double slashes, single-dot-directories and double-dot-directories
	 * from a path. This means, it does nearly the same as <code>File.getCanonicalPath</code>, but
	 * it does not resolve symlinks. This is essential for the method <code>getRelativePath</code>,
	 * because this method first tries it with a simplified path before using the canonical path
	 * (prevents problems with iteration through directories, where there are symlinks).
	 * <p>
	 * Please note that this method makes the given path absolute!
	 *
	 * @param path A path to simplify, e.g. "/../opt/java/jboss/../jboss/./bin/././../server/default/lib/."
	 * @return the simplified string (absolute path), e.g. "/opt/java/jboss/server/default/lib"
	 */
	public static String simplifyPath(File path)
	{
		LinkedList<String> dirs = new LinkedList<String>();

		String pathStr = path.getAbsolutePath();
		boolean startWithSeparator = pathStr.startsWith(File.separator);

		StringTokenizer tk = new StringTokenizer(pathStr, File.separator, false);
		while (tk.hasMoreTokens()) {
			String dir = tk.nextToken();
			if (".".equals(dir))
				;// nothing
			else if ("..".equals(dir)) {
				if (!dirs.isEmpty())
					dirs.removeLast();
			}
			else
				dirs.addLast(dir);
		}

		StringBuffer sb = new StringBuffer();
		for (String dir : dirs) {
			if (startWithSeparator || sb.length() > 0)
				sb.append(File.separator);
			sb.append(dir);
		}

		return sb.toString();
	}

	/**
	 * Transfer data between streams.
	 * @param in The input stream
	 * @param out The output stream
	 * @param inputOffset How many bytes to skip before transferring
	 * @param inputLen How many bytes to transfer. -1 = all
	 * @return The number of bytes transferred
	 * @throws IOException if an error occurs.
	 */
	public static long transferStreamData(InputStream in, OutputStream out, long inputOffset, long inputLen)
	throws java.io.IOException
	{
		return transferStreamData(in, out, inputOffset, inputLen, null);
	}

	public static long transferStreamData(InputStream in, OutputStream out, long inputOffset, long inputLen, ProgressMonitor monitor)
	throws java.io.IOException
	{
		byte[] buf = new byte[4096];

		if (monitor!= null) {
			if (inputLen < 0)
				monitor.beginTask("Copying data", 100);
			else
				monitor.beginTask("Copying data", (int)(inputLen / buf.length) + 1);
		}
		try {
			int bytesRead;
			int transferred = 0;

			//skip offset
			if(inputOffset > 0)
				if(in.skip(inputOffset) != inputOffset)
					throw new IOException("Input skip failed (offset "+inputOffset+")");

			while (true) {
				if(inputLen >= 0)
					bytesRead = in.read(buf, 0, (int)Math.min(buf.length, inputLen-transferred));
				else
					bytesRead = in.read(buf);

				if (bytesRead <= 0)
					break;

				out.write(buf, 0, bytesRead);

				if (monitor != null && inputLen >= 0)
					monitor.worked(1);

				transferred += bytesRead;

				if(inputLen >= 0 && transferred >= inputLen)
					break;
			}
			out.flush();

			return transferred;
		} finally {
			if (monitor!= null) {
				if (inputLen < 0)
					monitor.worked(100);

				monitor.done();
			}
		}
	}

	/**
	 * Transfer all available data from an {@link InputStream} to an {@link OutputStream}.
	 * <p>
	 * This is a convenience method for <code>transferStreamData(in, out, 0, -1)</code>
	 * @param in The stream to read from
	 * @param out The stream to write to
	 * @return The number of bytes transferred
	 * @throws IOException In case of an error
	 */
	public static long transferStreamData(InputStream in, OutputStream out)
	throws java.io.IOException
	{
		return transferStreamData(in, out, 0, -1);
	}

	public static enum CreateSymlinkResult {
		/**
		 * The symlink was successfully created.
		 */
		symlinked,

		/**
		 * The data was copied instead of being symlinked.
		 */
		copied,

		/**
		 * If the destination already exists.
		 */
		alreadyExists,

		/**
		 * While waiting for the symlink to be created, we catched an {@link InterruptedException}.
		 */
		interrupted,

		/**
		 * The symlink was not created and <code>copyIfSymlinksNotSupported</code> was set to <code>false</code>.
		 */
		unsupported,

		/**
		 * Specifies that the existing File object neither denotes a Directory nor a File. This can only be returned
		 * when creation of the symlink was not possible.
		 */
		unknownFileType
	}

	/**
	 * This method creates a symlink if supported by the operating system. Even though windows
	 * does support them, this method supports them only on GNU/Linux, so far. It seems, anyway,
	 * that symlinks on windows can cause very strange behaviour and data loss! see this for more
	 * details: http://shell-shocked.org/article.php?id=284
	 *
	 * @param existing The existing file or directory. If this is relative, the symlink will be relative, too.
	 *		If the data needs to be copied, it will be copied to <code>new File(link, existing.getPath())</code>.
	 * @param link The link that shall be created.
	 * @param copyIfSymlinksNotSupported If this is <code>true</code> and the OS does not support symlinks, the method
	 *		will delegate to {@link #copyDirectory(File, File)} or {@link #copyFile(File, File)}. If it is <code>false</code>,
	 *		either the symlink can be created or nothing will be done.
	 * @return an instance of {@link CreateSymlinkResult} - never <code>null</code>.
	 * @throws IOException if IO fails in an underlying call.
	 */
	public static CreateSymlinkResult createSymlink(File existing, File link, boolean copyIfSymlinksNotSupported)
	throws IOException
	{
		Logger logger = LoggerFactory.getLogger(IOUtil.class);

		if (logger.isDebugEnabled())
			logger.debug("createSymlink: begin: existing=\"" + existing.getPath() + "\" link=\"" + link.getPath() + "\"");

		if (link.exists())
			return CreateSymlinkResult.alreadyExists;

		File linkParent = link.getParentFile();
		if (linkParent != null && !linkParent.exists() && !linkParent.mkdirs())
			logger.warn("createSymlink: creating the link's parent directories failed!");

		int processResult;
		Process process = Runtime.getRuntime().exec(new String[] { "/bin/ln", "-s", existing.getPath(), link.getAbsolutePath() });
		try {
			processResult = process.waitFor();
		} catch (InterruptedException e) {
			logger.warn("createSymlink: interrupted! will return immediately!", e);
			return CreateSymlinkResult.interrupted;
		}

		if (processResult != 0)
			logger.warn("createSymlink: processResult != 0, but: " + processResult);

		if (link.exists())
			return CreateSymlinkResult.symlinked;

		if (!copyIfSymlinksNotSupported)
			return CreateSymlinkResult.unsupported;

		logger.debug("createSymlink: symlink could not be created - will copy instead: existing=\"" + existing.getPath() + "\" link=\"" + link.getPath() + "\"");

		// A symlink references from the ${link} to the ${existing}. Therefore, we must modify ${existing}, if it is not absolute.
		if (!existing.isAbsolute())
			existing = new File(link, existing.getPath());

		if (existing.isDirectory())
			copyDirectory(existing, link);
		else if (existing.isFile())
			copyFile(existing, link);
		else {
			logger.debug("createSymlink: ${existing} is neither a directory, nor a file! cannot create link: existing=\"" + existing.getPath() + "\" link=\"" + link.getPath() + "\"");
			return CreateSymlinkResult.unknownFileType;
		}

		return CreateSymlinkResult.copied;
	}

	/**
	 * This method deletes the given directory recursively. If the given parameter
	 * specifies a file and no directory, it will be deleted anyway. If one or more
	 * files or subdirectories cannot be deleted, the method still continues and tries
	 * to delete as many files/subdirectories as possible.
	 * <p>
	 * Before diving into a subdirectory, it first tries to delete <code>dir</code>. If this
	 * succeeds, it does not try to dive into it. This way, this implementation is symlink-safe.
	 * Hence, if <code>dir</code> denotes a symlink to another directory, this method does
	 * normally not delete the real contents of the directory.
	 * </p>
	 * <p>
	 * <b>Warning!</b> It sometimes still deletes the contents! This happens, if the user has no permissions
	 * onto a symlinked directory (thus, the deletion before dive-in fails), but its contents. In this case,
	 * the method will dive into the directory (because the deletion failed and it therefore assumes it to be a real
	 * directory) and delete the contents.
	 * </p>
	 * <p>
	 * We might later extend this method to do further checks (maybe call an OS program like we do in
	 * {@link #createSymlink(File, File, boolean)}).
	 * </p>
	 *
	 * @param dir The directory or file to delete
	 * @return <code>true</code> if the file or directory does not exist anymore.
	 * 		This means it either was not existing already before or it has been
	 * 		successfully deleted. <code>false</code> if the directory could not be
	 * 		deleted.
	 */
	public static boolean deleteDirectoryRecursively(File dir)
	{
		if (!dir.exists())
			return true;

		// If we're running this on linux (that's what I just tested ;) and dir denotes a symlink,
		// we must not dive into it and delete its contents! We can instead directly delete dir.
		// There is no way in Java (except for calling system tools) to find out whether it is a symlink,
		// but we can simply delete it. If the deletion succeeds, it was a symlink, otherwise it's a real directory.
		// This way, we don't delete the contents in symlinks and thus prevent data loss!
		try {
			if (dir.delete())
				return true;
		} catch(SecurityException e) {
			// ignore according to docs.
			return false; // or should we really ignore this security exception and delete the contents?!?!?! To return false instead is definitely safer.
		}

		if (dir.isDirectory()) {
			File[] content = dir.listFiles();
			for (File f : content) {
				if (f.isDirectory())
					deleteDirectoryRecursively(f);
				else
					try {
						f.delete();
					} catch(SecurityException e) {
						// ignore according to docs.
					}
			}
		}

		try {
			return dir.delete();
		} catch(SecurityException e) {
			return false;
		}
	}

	/**
	 * This method deletes the given directory recursively. If the given parameter
	 * specifies a file and no directory, it will be deleted anyway. If one or more
	 * files or subdirectories cannot be deleted, the method still continues and tries
	 * to delete as many files/subdirectories as possible.
	 *
	 * @param dir The directory or file to delete
	 * @return True, if the file or directory does not exist anymore. This means it either
	 * was not existing already before or it has been successfully deleted. False, if the
	 * directory could not be deleted.
	 */
	public static boolean deleteDirectoryRecursively(String dir)
	{
		File dirF = new File(dir);
		return deleteDirectoryRecursively(dirF);
	}

	/**
	 * Tries to find a unique, not existing folder name under the given root folder
	 * suffixed with a number. When found, the directory will be created.
	 * It will start with 0 and make Integer.MAX_VALUE number
	 * of iterations maximum. The first not existing foldername will be returned.
	 * If no foldername could be found after the maximum iterations a {@link IOException}
	 * will be thrown.
	 * <p>
	 * Note that the creation of the directory is not completely safe. This method is
	 * synchronized, but other processes could "steal" the unique filename.
	 *
	 * @param rootFolder The rootFolder to find a unique subfolder for
	 * @param prefix A prefix for the folder that has to be found.
	 * @return A File pointing to an unique (not existing) Folder under the given rootFolder and with the given prefix
	 * @throws IOException in case of an error
	 */
	public static synchronized File createUniqueIncrementalFolder(File rootFolder, final String prefix) throws IOException
	{
		for(int n=0; n<Integer.MAX_VALUE; n++) {
			File f = new File(rootFolder, String.format("%s%x", prefix, n));
			if(!f.exists()) {
				if(!f.mkdirs())
					throw new IOException("The directory "+f.getAbsolutePath()+" could not be created");
				return f;
			}
		}
		throw new IOException("Iterated to Integer.MAX_VALUE and could not find a unique folder!");
	}

	/**
	 * Tries to find a unique, not existing folder name under the given root folder with a random
	 * number (in hex format) added to the given prefix. When found, the directory will be created.
	 * <p>
	 * The method will try to find a name for <code>maxIterations</code> number
	 * of itarations and use random numbers from 0 to <code>uniqueOutOf</code>.
	 * <p>
	 * Note that the creation of the directory is not completely safe. This method is
	 * synchronized, but other processes could "steal" the unique filename.
	 *
	 * @param rootFolder The rootFolder to find a unique subfolder for
	 * @param prefix A prefix for the folder that has to be found.
	 * @param maxIterations The maximum number of itarations this method shoud do.
	 * 		If after them still no unique folder could be found, a {@link IOException}
	 * 		is thrown.
	 * @param uniqueOutOf The range of random numbers to apply (0 - given value)
	 *
	 * @return A File pointing to an unique folder under the given rootFolder and with the given prefix
	 * @throws IOException in case of an error
	 */
	public static synchronized File createUniqueRandomFolder(File rootFolder, final String prefix, long maxIterations, long uniqueOutOf) throws IOException
	{
		long count = 0;
		while(++count <= maxIterations) {
			File f = new File(rootFolder, String.format("%s%x", prefix, (long)(Math.random() * uniqueOutOf)));
			if(!f.exists()) {
				if(!f.mkdirs())
					throw new IOException("The directory "+f.getAbsolutePath()+" could not be created");
				return f;
			}
		}
		throw new IOException("Reached end of maxIteration("+maxIterations+"), but could not acquire a unique fileName for folder "+rootFolder);
	}

	/**
	 * Tries to find a unique, not existing folder name under the given root folder with a random
	 * number (in hex format) added to the given prefix. When found, the directory will be created.
	 * <p>
	 * This is a convenience method for {@link createUniqueRandomFolder}
	 * and calls it with 10000 as maxIterations and 10000 as number range.
	 * <p>
	 * Note that this method might throw a {@link IOException}
	 * if it will not find a unique name within 10000 iterations.
	 * <p>
	 * Note that the creation of the directory is not completely safe. This method is
	 * synchronized, but other processes could "steal" the unique filename.
	 *
	 * @param rootFolder The rootFolder to find a unique subfolder for
	 * @param prefix A prefix for the folder that has to be found.
	 * @return A File pointing to an unique (non-existing) Folder under the given rootFolder and with the given prefix
	 * @throws IOException in case of an error
	 */
	public static File createUniqueRandomFolder(File rootFolder, final String prefix) throws IOException
	{
		return createUniqueRandomFolder(rootFolder, prefix, 10000, 10000);
	}

	/**
	 * Get a file object from a base directory and a list of subdirectories or files.
	 * @param file The base directory
	 * @param subDirs The subdirectories or files
	 * @return The new file instance
	 */
	public static File getFile(File file, String ... subDirs)
	{
		File f = file;
		for (String subDir : subDirs)
			f = new File(f, subDir);
		return f;
	}

	/**
	 * Write text to a file.
	 * @param file The file to write the text to
	 * @param text The text to write
	 * @param encoding The caracter set to use as file encoding (e.g. "UTF-8")
	 * @throws IOException in case of an io error
	 * @throws FileNotFoundException if the file exists but is a directory
	 *                   rather than a regular file, does not exist but cannot
	 *                   be created, or cannot be opened for any other reason
	 * @throws UnsupportedEncodingException If the named encoding is not supported
	 */
	public static void writeTextFile(File file, String text, String encoding)
	throws IOException, FileNotFoundException, UnsupportedEncodingException
	{
		FileOutputStream out = null;
		OutputStreamWriter w = null;
		try {
			out = new FileOutputStream(file);
			w = new OutputStreamWriter(out, encoding);
			w.write(text);
		} finally {
			if (w != null) w.close();
			if (out != null) out.close();
		}
	}

	/**
	 * Read a text file and return the contents as string.
	 * @param f The file to read, maximum size 1 GB
	 * @param encoding The file encoding, e.g. "UTF-8"
	 * @throws FileNotFoundException if the file was not found
	 * @throws IOException in case of an io error
	 * @throws UnsupportedEncodingException If the named encoding is not supported
	 * @return The contents of the text file
	 */
	public static String readTextFile(File f, String encoding)
	throws FileNotFoundException, IOException, UnsupportedEncodingException
	{
		if (f.length() > GIGABYTE)
			throw new IllegalArgumentException("File exceeds " + GIGABYTE + " bytes: " + f.getAbsolutePath());

		StringBuffer sb = new StringBuffer();
		FileInputStream fin = new FileInputStream(f);
		try {
			InputStreamReader reader = new InputStreamReader(fin, encoding);
			try {
				char[] cbuf = new char[1024];
				int bytesRead;
				while (true) {
					bytesRead = reader.read(cbuf);
					if (bytesRead <= 0)
						break;
					else
						sb.append(cbuf, 0, bytesRead);
				}
			} finally {
				reader.close();
			}
		} finally {
			fin.close();
		}
		return sb.toString();
	}

	/**
	 * Read a UTF-8 encoded text file and return the contents as string.
	 * <p>For other encodings, use {@link readTextFile}.
	 * @param f The file to read, maximum size 1 GB
	 * @throws FileNotFoundException if the file was not found
	 * @throws IOException in case of an io error
	 * @return The contents of the text file
	 */
	public static String readTextFile(File f)
	throws FileNotFoundException, IOException
	{
		return readTextFile(f, CHARSET_NAME_UTF_8);
	}

	/**
	 * Write text to a file using UTF-8 encoding.
	 * @param file The file to write the text to
	 * @param text The text to write
	 * @throws IOException in case of an io error
	 * @throws FileNotFoundException if the file exists but is a directory
	 *                   rather than a regular file, does not exist but cannot
	 *                   be created, or cannot be opened for any other reason
	 */
	public static void writeTextFile(File file, String text)
	throws IOException, FileNotFoundException, UnsupportedEncodingException
	{
		writeTextFile(file, text, CHARSET_NAME_UTF_8);
	}

	/**
	 * Read a text file from an {@link InputStream} using
	 * the given encoding.
	 * <p>
	 * This method does NOT close the input stream!
	 * @param in The stream to read from. It will not be closed by this operation.
	 * @param encoding The charset used for decoding, e.g. "UTF-8"
	 * @return The contents of the input stream file
	 */
	public static String readTextFile(InputStream in, String encoding)
	throws FileNotFoundException, IOException
	{
		StringBuffer sb = new StringBuffer();
		InputStreamReader reader = new InputStreamReader(in, encoding);
		char[] cbuf = new char[1024];
		int bytesRead;
		while (true) {
			bytesRead = reader.read(cbuf);
			if (bytesRead <= 0)
				break;
			else
				sb.append(cbuf, 0, bytesRead);

			if (sb.length() > GIGABYTE)
				throw new IllegalArgumentException("Text exceeds " + GIGABYTE + " bytes!");
		}
		return sb.toString();
	}

	/**
	 * Read a text file from an {@link InputStream} using
	 * UTF-8 encoding.
	 * <p>
	 * This method does NOT close the input stream!
	 * @param in The stream to read from. It will not be closed by this operation.
	 * @return The contents of the input stream file
	 */
	public static String readTextFile(InputStream in)
	throws FileNotFoundException, IOException
	{
		return readTextFile(in, CHARSET_NAME_UTF_8);
	}

	/**
	 * Get the extension of a filename.
	 * @param fileName A file name (might contain the full path) or <code>null</code>.
	 * @return <code>null</code>, if the given <code>fileName</code> doesn't contain
	 *		a dot (".") or if the given <code>fileName</code> is <code>null</code>. Otherwise,
	 *		returns all AFTER the last dot.
	 */
	public static String getFileExtension(String fileName)
	{
		if (fileName == null)
			return null;

		int lastIndex = fileName.lastIndexOf(".");
		if (lastIndex < 0)
			return null;

		return fileName.substring(lastIndex + 1);
	}

	/**
	 * Get a filename without extension.
	 * @param fileName A file name (might contain the full path) or <code>null</code>.
	 * @return all before the last dot (".") or the full <code>fileName</code> if no dot exists.
	 * 		Returns <code>null</code>, if the given <code>fileName</code> is <code>null</code>.
	 */
	public static String getFileNameWithoutExtension(String fileName)
	{
		if (fileName == null)
			return null;

		int lastIndex = fileName.lastIndexOf(".");
		if (lastIndex < 0)
			return fileName;

		return fileName.substring(0, lastIndex);
	}

	/**
	 * Get the temporary directory.
	 * <p>
	 * Note, that you should better use {@link #getUserTempDir()} in many situations
	 * since there is solely one global temp directory in GNU/Linux and you might run into permissions trouble
	 * and other collisions when using the global temp directory with a hardcoded static subdir.
	 * </p>
	 *
	 * @return The temporary directory.
	 * @see #getUserTempDir()
	 */
	public static File getTempDir()
	{
		if(tempDir == null)
	    tempDir = new File(System.getProperty("java.io.tmpdir"));
		return tempDir;
	}

	/**
	 * Get a user-dependent temp directory in every operating system and create it, if it does not exist.
	 *
	 * @param prefix <code>null</code> or a prefix to be added before the user-name.
	 * @param suffix <code>null</code> or a suffix to be added after the user-name.
	 * @return a user-dependent temp directory, which is created if it does not yet exist.
	 * @throws IOException if the directory does not exist and cannot be created.
	 */
	public static File createUserTempDir(String prefix, String suffix)
	throws IOException
	{
		File userTempDir = getUserTempDir(prefix, suffix);
		if (userTempDir.isDirectory())
			return userTempDir;

		if (userTempDir.exists()) {
			if (!userTempDir.isDirectory()) // two processes might call this method simultaneously - thus we must check again; in case it was just created.
				throw new IOException("The user-dependent temp directory's path exists already, but is no directory: " + userTempDir.getPath());
		}

		if (!userTempDir.mkdirs()) {
			if (!userTempDir.isDirectory())
				throw new IOException("The user-dependent temp directory could not be created: " + userTempDir.getPath());
		}

		return userTempDir;
	}

	/**
	 * Get a user-dependent temp directory in every operating system. Given the same prefix and suffix, the resulting
	 * directory will always be the same on subsequent calls - even across different VMs.
	 * <p>
	 * Since many operating systems (for example GNU/Linux and other unixes) use one
	 * global temp directory for all users, you might run into collisions when using hard-coded sub-directories within the
	 * temp dir. Permission problems can cause exceptions and multiple users sharing the same directory
	 * at the same time might even lead to heisenbugs. Therefore, this method encodes the user
	 * name into a subdirectory under the system's temp dir.
	 * </p>
	 * <p>
	 * In order to prevent illegal directory names to be generated, this method encodes the user name (including the
	 * pre- and suffixes passed) using a {@link ParameterCoderMinusHex} which encodes all chars except
	 * '0'...'9', 'A'...'Z', 'a'...'z', '.', '_' and '+'.
	 * </p>
	 *
	 * @param prefix <code>null</code> or a prefix to be added before the user-name.
	 * @param suffix <code>null</code> or a suffix to be added after the user-name.
	 * @return a user-dependent temp directory.
	 * @see #getTempDir()
	 * @see #createUserTempDir(String, String)
	 */
	public static File getUserTempDir(String prefix, String suffix)
	{
		// In GNU/Linux, there is exactly one temp-directory for all users; hence we need to put the current OS user's name into the path.
		String userNameDir = (prefix == null ? "" : prefix) + String.valueOf(getUserName()) + (suffix == null ? "" : suffix);

		// the user name might contain illegal characters (in windows) => we encode basically all characters.
		try {
			userNameDir = URLEncoder.encode(userNameDir.replace('*', '_'), CHARSET_NAME_UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		return new File(IOUtil.getTempDir(), userNameDir);
	}

	private static String getUserName()
	{
		return System.getProperty("user.name"); //$NON-NLS-1$
	}

	public static File getUserHome()
	{
		String userHome = System.getProperty("user.home"); //$NON-NLS-1$
		if (userHome == null)
			throw new IllegalStateException("System property user.home is not set! This should never happen!"); //$NON-NLS-1$

		return new File(userHome);
	}

	/**
	 * Compares two InputStreams. The amount of bytes given in the parameter
	 * <code>length</code> are consumed from the streams, no matter if their
	 * contents are equal or not.
	 *
	 * @param in1 the first InputStream
	 * @param in2 the second InputStream
	 * @param length how many bytes to read from each InputStream
	 * @return true if both InputStreams contain the identical data or false if not
	 * @throws IOException if an I/O error occurs while reading <code>length</code> bytes
	 * 		from one of the input streams.
	 */
	public static boolean compareInputStreams(InputStream in1, InputStream in2, long length)
	throws IOException
	{
		return compareInputStreams(in1, in2, length, length);
	}

	/**
	 * Compares two InputStreams. This method returns when the first
	 *
	 * @param in1 the first InputStream
	 * @param in2 the second InputStream
	 * @param compareLength how many bytes to compare before considering the stream
	 * 		contents as equal.
	 * @param minimumReadLength how many bytes to read from each InputStream. This amount of
	 * 		bytes is read from the stream, no matter if contents are equal or not.
	 * @return true if both InputStreams contain the identical data or false if not
	 * @throws IOException if an I/O error occurs while reading <code>length</code> bytes
	 * 		from one of the input streams.
	 */
	public static boolean compareInputStreams(InputStream in1, InputStream in2, long compareLength, long minimumReadLength)
	throws IOException
	{
		boolean identical = true;
		int read = 0;
		while(read<compareLength) {
			int int1 = in1.read();
			int int2 = in2.read();
			read++;
			if (int1 != int2) {
				identical = false;
				break;
			}
		}
		if(read < minimumReadLength) {
			in1.skip(minimumReadLength-read);
			in2.skip(minimumReadLength-read);
		}
		return identical;
	}

	/**
	 * Compare the contents of two given files.
	 * @param f1 the first file
	 * @param f2 the second file
	 * @return <code>true</code> if the files have same size and their contents are equal -
	 * 		<code>false</code> otherwise.
	 * @throws FileNotFoundException If one of the files could not be found.
	 * @throws IOException If reading one of the files failed.
	 */
	public static boolean compareFiles(File f1, File f2) throws FileNotFoundException, IOException
	{
		if(!f1.exists())
			throw new FileNotFoundException(f1.getAbsolutePath());
		if(!f2.exists())
			throw new FileNotFoundException(f2.getAbsolutePath());
		if(f1.equals(f2))
			return true;
		if(f1.length() != f2.length())
			return false;
		FileInputStream in1 = new FileInputStream(f1);
		FileInputStream in2 = new FileInputStream(f2);
		try {
			return compareInputStreams(in1, in2, f1.length(), 0);
		} finally {
			in1.close();
			in2.close();
		}
	}

	/**
	 * Copy a directory recursively.
	 * @param sourceDirectory The source directory
	 * @param destinationDirectory The destination directory
	 * @throws IOException in case of an error
	 */
	public static void copyDirectory(File sourceDirectory, File destinationDirectory) throws IOException
	{
		if(!sourceDirectory.exists() || !sourceDirectory.isDirectory())
			throw new IOException("No such source directory: "+sourceDirectory.getAbsolutePath());
		if(destinationDirectory.exists()) {
			if(!destinationDirectory.isDirectory())
				throw new IOException("Destination exists but is not a directory: "+sourceDirectory.getAbsolutePath());
		} else
			destinationDirectory.mkdirs();

		File[] files = sourceDirectory.listFiles();
		for (File file : files) {
			File destinationFile = new File(destinationDirectory, file.getName());
			if(file.isDirectory())
				copyDirectory(file, destinationFile);
			else
				copyFile(file, destinationFile);
		}

		destinationDirectory.setLastModified(sourceDirectory.lastModified());
	}

	/**
	 * Copy a file.
	 * <p>
	 * This is a convenience method for <code>copyFile(new File(sourceFilename), new File(destinationFilename))</code>
	 * @param sourceFilename The source file to copy
	 * @param destinationFilename To which file to copy the source
	 * @throws IOException in case of an error
	 */
	public static void copyFile(String sourceFilename, String destinationFilename)
	throws IOException
	{
		copyFile(new File(sourceFilename), new File(destinationFilename));
	}

	/**
	 * Copy a resource loaded by the class loader of a given class to a file.
	 * <p>
	 * This is a convenience method for <code>copyResource(sourceResClass, sourceResName, new File(destinationFilename))</code>.
	 * @param sourceResClass The class whose class loader to use. If the class
	 * 		was loaded using the bootstrap class loaderClassloader.getSystemResourceAsStream
	 * 		will be used. See {@link Class#getResourceAsStream(String)} for details.
	 * @param sourceResName The name of the resource
	 * @param destinationFilename Where to copy the contents of the resource
	 * @throws IOException in case of an error
	 */
	public static void copyResource(Class<?> sourceResClass, String sourceResName, String destinationFilename)
	throws IOException
	{
		copyResource(sourceResClass, sourceResName, new File(destinationFilename));
	}


	/**
	 * Copy a resource loaded by the class loader of a given class to a file.
	 * @param sourceResClass The class whose class loader to use. If the class
	 * 		was loaded using the bootstrap class loaderClassloader.getSystemResourceAsStream
	 * 		will be used. See {@link Class#getResourceAsStream(String)} for details.
	 * @param sourceResName The name of the resource
	 * @param destinationFile Where to copy the contents of the resource
	 * @throws IOException in case of an error
	 */
	public static void copyResource(Class<?> sourceResClass, String sourceResName, File destinationFile)
	throws IOException
	{
		InputStream source = null;
		FileOutputStream destination = null;
		try{
			source = sourceResClass.getResourceAsStream(sourceResName);
			if (source == null)
				throw new FileNotFoundException("Class " + sourceResClass.getName() + " could not find resource " + sourceResName);

			if (destinationFile.exists()) {
				if (destinationFile.isFile()) {
					if (!destinationFile.canWrite())
						throw new IOException("FileCopy: destination file is unwriteable: " + destinationFile.getCanonicalPath());
				} else
					throw new IOException("FileCopy: destination is not a file: " +	destinationFile.getCanonicalPath());
			} else {
				File parentdir = destinationFile.getAbsoluteFile().getParentFile();
				if (parentdir == null || !parentdir.exists())
					throw new IOException("FileCopy: destination's parent directory doesn't exist: " + destinationFile.getCanonicalPath());
				if (!parentdir.canWrite())
					throw new IOException("FileCopy: destination's parent directory is unwriteable: " + destinationFile.getCanonicalPath());
			}
			destination = new FileOutputStream(destinationFile);
			transferStreamData(source,destination);
		} finally {
			if (source != null)
				try { source.close(); } catch (IOException e) { ; }

			if (destination != null)
				try { destination.close(); } catch (IOException e) { ; }
		}
	}

	/**
	 * Copy a file.
	 * @param sourceFile The source file to copy
	 * @param destinationFile To which file to copy the source
	 * @throws IOException in case of an error
	 */
	public static void copyFile(File sourceFile, File destinationFile)
	throws IOException
	{
		copyFile(sourceFile, destinationFile, null);
	}
	public static void copyFile(File sourceFile, File destinationFile, ProgressMonitor monitor)
	throws IOException
	{
		FileInputStream source = null;
		FileOutputStream destination = null;

		try {
			// First make sure the specified source file
			// exists, is a file, and is readable.
			if (!sourceFile.exists() || !sourceFile.isFile())
				throw new IOException("FileCopy: no such source file: "+sourceFile.getCanonicalPath());
			if (!sourceFile.canRead())
			 throw new IOException("FileCopy: source file is unreadable: "+sourceFile.getCanonicalPath());

			// If the destination exists, make sure it is a writeable file.	If the destination doesn't
			// exist, make sure the directory exists and is writeable.
			if (destinationFile.exists()) {
				if (destinationFile.isFile()) {
					if (!destinationFile.canWrite())
						throw new IOException("FileCopy: destination file is unwriteable: " + destinationFile.getCanonicalPath());
				} else
					throw new IOException("FileCopy: destination is not a file: " +	destinationFile.getCanonicalPath());
			} else {
				File parentdir = destinationFile.getParentFile();
				if (parentdir == null || !parentdir.exists())
					throw new IOException("FileCopy: destination directory doesn't exist: " +
									destinationFile.getCanonicalPath());
				 if (!parentdir.canWrite())
					 throw new IOException("FileCopy: destination directory is unwriteable: " +
									destinationFile.getCanonicalPath());
			}
			// If we've gotten this far, then everything is okay; we can
			// copy the file.
			source = new FileInputStream(sourceFile);
			destination = new FileOutputStream(destinationFile);
			transferStreamData(source, destination, 0, sourceFile.length(), monitor);
			// No matter what happens, always close any streams we've opened.
		} finally {
			if (source != null)
				try { source.close(); } catch (IOException e) { ; }
			if (destination != null)
				try { destination.close(); } catch (IOException e) { ; }
		}

		// copy the timestamp
		destinationFile.setLastModified(sourceFile.lastModified());
	}

	/**
	 * Add a trailing file separator character to the
	 * given directory name if it does not already
	 * end with one.
	 * @see File#separator
	 * @param directory A directory name
	 * @return the directory name anding with a file seperator
	 */
	public static String addFinalSlash(String directory)
	{
		if (directory.endsWith(File.separator))
			return directory;
		else
			return directory + File.separator;
	}

	private static enum ParserExpects {
		NORMAL,
		BRACKET_OPEN,
		VARIABLE,
		BRACKET_CLOSE
	}


	/**
	 * Generate a file from a template. The template file can contain variables which are formatted <code>"${variable}"</code>.
	 * All those variables will be replaced, for which a value has been passed in the map <code>variables</code>.
	 * <p>
	 * Example:<br/>
	 * <pre>
	 * ***
	 * Dear ${receipient.fullName},
	 * this is a spam mail trying to sell you ${product.name} for a very cheap price.
	 * Best regards, ${sender.fullName}
	 * ***
	 * </pre>
	 * <br/>
	 * In order to generate a file from the above template, the map <code>variables</code> needs to contain values for these keys:
	 * <ul>
	 * <li>receipient.fullName</li>
	 * <li>product.name</li>
	 * <li>sender.fullName</li>
	 * </ul>
	 * </p>
	 * <p>
	 * If a key is missing in the map, the variable will not be replaced but instead written as-is into the destination file (a warning will be
	 * logged).
	 * </p>
	 *
	 * @param destinationFile The file (absolute!) that shall be created out of the template.
	 * @param templateFile The template file to use. Must not be <code>null</code>.
	 * @param characterSet The charset to use for the input and output file. Use <code>null</code> for the default charset.
	 * @param variables This map defines what variable has to be replaced by what value. The
	 * key is the variable name (without '$' and brackets '{', '}'!) and the value is the
	 * value that will replace the variable in the output. This argument must not be <code>null</code>.
	 * In order to allow passing {@link Properties} directly, this has been redefined as <code>Map&lt;?,?&gt;</code>
	 * from version 1.3.0 on. Map entries with a key that is not of type <code>String</code> or with a <code>null</code>
	 * value are ignored. Values that are not of type <code>String</code> are converted using the
	 * {@link Object#toString() toString()} method.
	 */
	public static void replaceTemplateVariables(File destinationFile, File templateFile, String characterSet, Map<?, ?> variables)
		throws IOException
	{
		if (!destinationFile.isAbsolute())
			throw new IllegalArgumentException("destinationFile is not absolute: " + destinationFile.getPath());

		logger.info("Creating destination file \""+destinationFile.getAbsolutePath()+"\" from template \""+templateFile.getAbsolutePath()+"\".");
		File destinationDirectory = destinationFile.getParentFile();
		if (!destinationDirectory.exists()) {
			logger.info("Directory for destination file does not exist. Creating it: " + destinationDirectory.getAbsolutePath());
			if (!destinationDirectory.mkdirs())
				logger.error("Creating directory for destination file failed: " + destinationDirectory.getAbsolutePath());
		}

		// Create and configure StreamTokenizer to read template file.
		FileInputStream fin = new FileInputStream(templateFile);
		try {
			Reader fr = characterSet != null ? new InputStreamReader(fin, characterSet) : new InputStreamReader(fin);
			try {
				// Create FileWriter
				FileOutputStream fos = new FileOutputStream(destinationFile);
				try {
					Writer fw = characterSet != null ? new OutputStreamWriter(fos, characterSet) : new OutputStreamWriter(fos);
					try {
						replaceTemplateVariables(fw, fr, variables);
					} finally {
						fw.close();
					}
				} finally {
					fos.close();
				}
			} finally {
				fr.close();
			}
		} finally {
			fin.close();
		}
	}

	/**
	 * Replace variables (formatted <code>"${variable}"</code>) with their values
	 * passed in the map <code>variables</code>.
	 * <p>
	 * Example:<br/>
	 * <pre>
	 * ***
	 * Dear ${receipient.fullName},
	 * this is a spam mail trying to sell you ${product.name} for a very cheap price.
	 * Best regards, ${sender.fullName}
	 * ***
	 * </pre>
	 * <br/>
	 * In order to generate a text from the above template, the map <code>variables</code> needs to contain values for these keys:
	 * <ul>
	 * <li>receipient.fullName</li>
	 * <li>product.name</li>
	 * <li>sender.fullName</li>
	 * </ul>
	 * </p>
	 * <p>
	 * If a key is missing in the map, the variable will not be replaced but instead written as-is into the destination file (a warning will be
	 * logged).
	 * </p>
	 *
	 * @param template the input text containing variables (or not).
	 * @param variables This map defines what variable has to be replaced by what value. The
	 * key is the variable name (without '$' and brackets '{', '}'!) and the value is the
	 * value that will replace the variable in the output. This argument must not be <code>null</code>.
	 * In order to allow passing {@link Properties} directly, this has been redefined as <code>Map&lt;?,?&gt;</code>
	 * from version 1.3.0 on. Map entries with a key that is not of type <code>String</code> or with a <code>null</code>
	 * value are ignored. Values that are not of type <code>String</code> are converted using the
	 * {@link Object#toString() toString()} method.
	 * @return the processed text (i.e. the same as the template, but all known variables replaced by their values).
	 */
	public static String replaceTemplateVariables(String template, Map<?, ?> variables)
	{
		try {
			StringReader r = new StringReader(template);
			StringWriter w = new StringWriter();
			try {
				replaceTemplateVariables(w, r, variables);
				w.flush();
				return w.toString();
			} finally {
				r.close();
				w.close();
			}
		} catch (IOException x) {
			throw new RuntimeException(x); // StringReader/Writer should *NEVER* throw any IOException since it's working in-memory only.
		}
	}

	/**
	 * Copy contents from the given reader to the given writer while
	 * replacing variables which are formatted <code>"${variable}"</code> with their values
	 * passed in the map <code>variables</code>.
	 * <p>
	 * Example:<br/>
	 * <pre>
	 * ***
	 * Dear ${receipient.fullName},
	 * this is a spam mail trying to sell you ${product.name} for a very cheap price.
	 * Best regards, ${sender.fullName}
	 * ***
	 * </pre>
	 * <br/>
	 * In order to generate a file from the above template, the map <code>variables</code> needs to contain values for these keys:
	 * <ul>
	 * <li>receipient.fullName</li>
	 * <li>product.name</li>
	 * <li>sender.fullName</li>
	 * </ul>
	 * </p>
	 * <p>
	 * If a key is missing in the map, the variable will not be replaced but instead written as-is into the destination file (a warning will be
	 * logged).
	 * </p>
	 *
	 * @param writer The writer to write the output to.
	 * @param reader The template file to use. Must not be <code>null</code>.
	 * @param variables This map defines what variable has to be replaced by what value. The
	 * key is the variable name (without '$' and brackets '{', '}'!) and the value is the
	 * value that will replace the variable in the output. This argument must not be <code>null</code>.
	 * In order to allow passing {@link Properties} directly, this has been redefined as <code>Map&lt;?,?&gt;</code>
	 * from version 1.3.0 on. Map entries with a key that is not of type <code>String</code> or with a <code>null</code>
	 * value are ignored. Values that are not of type <code>String</code> are converted using the
	 * {@link Object#toString() toString()} method.
	 */
	public static void replaceTemplateVariables(Writer writer, Reader reader, Map<?, ?> variables)
		throws IOException
	{
		StreamTokenizer stk = new StreamTokenizer(reader);
		stk.resetSyntax();
		stk.wordChars(0, Integer.MAX_VALUE);
		stk.ordinaryChar('$');
		stk.ordinaryChar('{');
		stk.ordinaryChar('}');
		stk.ordinaryChar('\n');

		// Read, parse and replace variables from template and write to FileWriter fw.
		String variableName = null;
		StringBuilder tmpBuf = new StringBuilder();
		ParserExpects parserExpects = ParserExpects.NORMAL;
		while (stk.nextToken() != StreamTokenizer.TT_EOF) {
			String stringToWrite = null;

			if (stk.ttype == StreamTokenizer.TT_WORD) {
				switch (parserExpects) {
				case VARIABLE:
					parserExpects = ParserExpects.BRACKET_CLOSE;
					variableName = stk.sval;
					tmpBuf.append(variableName);
					break;
				case NORMAL:
					stringToWrite = stk.sval;
					break;
				default:
					parserExpects = ParserExpects.NORMAL;
				stringToWrite = tmpBuf.toString() + stk.sval;
				tmpBuf.setLength(0);
				}
			}
			else if (stk.ttype == '\n') {
				stringToWrite = new String(Character.toChars(stk.ttype));

				// These chars are not valid within a variable, so we reset the variable parsing, if we're currently parsing one.
				// This helps keeping the tmpBuf small (to check for rowbreaks is not really necessary).
				if (parserExpects != ParserExpects.NORMAL) {
					parserExpects = ParserExpects.NORMAL;
					tmpBuf.append(stringToWrite);
					stringToWrite = tmpBuf.toString();
					tmpBuf.setLength(0);
				}
			}
			else if (stk.ttype == '$') {
				if (parserExpects != ParserExpects.NORMAL) {
					stringToWrite = tmpBuf.toString();
					tmpBuf.setLength(0);
				}
				tmpBuf.appendCodePoint(stk.ttype);
				parserExpects = ParserExpects.BRACKET_OPEN;
			}
			else if (stk.ttype == '{') {
				switch (parserExpects) {
				case NORMAL:
					stringToWrite = new String(Character.toChars(stk.ttype));
					break;
				case BRACKET_OPEN:
					tmpBuf.appendCodePoint(stk.ttype);
					parserExpects = ParserExpects.VARIABLE;
					break;
				default:
					parserExpects = ParserExpects.NORMAL;
					tmpBuf.appendCodePoint(stk.ttype);
					stringToWrite = tmpBuf.toString();
					tmpBuf.setLength(0);
				}
			}
			else if (stk.ttype == '}') {
				switch (parserExpects) {
				case NORMAL:
					stringToWrite = new String(Character.toChars(stk.ttype));
					break;
				case BRACKET_CLOSE:
					parserExpects = ParserExpects.NORMAL;
					tmpBuf.appendCodePoint(stk.ttype);

					if (variableName == null)
						throw new IllegalStateException("variableName is null!!!");

					Object variableValue = variables.get(variableName);
					stringToWrite = variableValue == null ? null : variableValue.toString();
					if (stringToWrite == null) {
						logger.warn("Variable " + tmpBuf.toString() + " occuring in template is unknown!");
						stringToWrite = tmpBuf.toString();
					}
					tmpBuf.setLength(0);
					break;
				default:
					parserExpects = ParserExpects.NORMAL;
					tmpBuf.appendCodePoint(stk.ttype);
					stringToWrite = tmpBuf.toString();
					tmpBuf.setLength(0);
				}
			}

			if (stringToWrite != null)
				writer.write(stringToWrite);
		} // while (stk.nextToken() != StreamTokenizer.TT_EOF) {
	}

	public static byte[] getBytesFromFile(File file) throws IOException {
		// Get the size of the file
		long length = file.length();
		byte[] bytes = new byte[(int)length];

		InputStream is = new FileInputStream(file);
		try {
			// Read in the bytes
			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length-offset)) >= 0) {
				offset += numRead;
			}

			// Ensure all the bytes have been read in
			if (offset < bytes.length) {
				throw new IOException("Could not completely read file "+file.getName());
			}
		} finally {
			// Close the input stream and return bytes
			is.close();
		}
		return bytes;
    }
}
