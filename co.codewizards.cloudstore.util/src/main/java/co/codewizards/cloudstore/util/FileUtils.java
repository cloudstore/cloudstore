package co.codewizards.cloudstore.util;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class FileUtils {

	private static final boolean LIKE_JAVA_NIO_PATH_RELATIVIZE = true;

	public static String getRelativePath(final File from, final File to) {
		return getRelativePath(from, to, File.separatorChar);
	}

	public static String getRelativePath(final File from, final File to,
			final char separatorChar) {
		final String fromPath = from.getAbsolutePath();
		final String toPath = to.getAbsolutePath();
		final boolean isDirectory = from.isDirectory();
		return getRelativePath(fromPath, toPath, isDirectory, separatorChar);
	}

	public static String getRelativePath(final String fromPath,
			final String toPath, boolean isFromPathDirectory,
			final char separatorChar) {
		isFromPathDirectory = LIKE_JAVA_NIO_PATH_RELATIVIZE || isFromPathDirectory; //from observation, left for explanation
		final ArrayList<String> fromPathSegments = splitPath(fromPath);
		final ArrayList<String> toPathSegments = splitPath(toPath);
		while (!fromPathSegments.isEmpty() && !toPathSegments.isEmpty()) {
			if (!(fromPathSegments.get(0).equals(toPathSegments.get(0)))) {
				break;
			}
			fromPathSegments.remove(0);
			toPathSegments.remove(0);
		}

		final StringBuffer sb = new StringBuffer();
		for (int i = 0; i < fromPathSegments.size() - (isFromPathDirectory ? 0 : 1); i++) {
			sb.append("..");
			sb.append(separatorChar);
		}
		for (final String s : toPathSegments) {
			sb.append(s);
			sb.append(separatorChar);
		}
		return sb.substring(0, sb.length() - 1);
	}

	private static ArrayList<String> splitPath(final String path) {
		final ArrayList<String> pathSegments = new ArrayList<String>();
		final StringTokenizer st = new StringTokenizer(path, File.separator);
		while (st.hasMoreTokens()) {
			final String token = st.nextToken();
			if (token.equals(".")) {
				// skip
			} else if (token.equals("..")) {
				if (!pathSegments.isEmpty()) {
					pathSegments.remove(pathSegments.size() - 1);
				}
			} else {
				pathSegments.add(token);
			}
		}
		return pathSegments;
	}
}
