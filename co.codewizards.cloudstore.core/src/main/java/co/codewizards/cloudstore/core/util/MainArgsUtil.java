package co.codewizards.cloudstore.core.util;

import java.util.ArrayList;
import java.util.List;

public final class MainArgsUtil {

	private MainArgsUtil() { }

	public static String[][] separateSystemPropertiesFromOtherArgs(String[] args) {
		List<String> sysPropArgs = new ArrayList<String>(args.length);
		List<String> otherArgs = new ArrayList<String>(args.length);

		for (String arg : args) {
			if (arg.startsWith("-D"))
				sysPropArgs.add(arg);
			else
				otherArgs.add(arg);
		}
		return new String[][] {
			sysPropArgs.toArray(new String[sysPropArgs.size()]),
			otherArgs.toArray(new String[otherArgs.size()])
		};
	}

	public static String[] extractAndApplySystemPropertiesReturnOthers(String[] args) {
		String[][] sysPropArgsAndOtherArgs = separateSystemPropertiesFromOtherArgs(args);
		String[] sysPropArgs = sysPropArgsAndOtherArgs[0];
		String[] otherArgs = sysPropArgsAndOtherArgs[1];
		for (String arg : sysPropArgs) {
			if (!arg.startsWith("-D"))
				throw new IllegalStateException("sysPropArgs contains element not starting with '-D': " + arg);

			String kv = arg.substring(2);
			int equalsIndex = kv.indexOf('=');
			if (equalsIndex >= 0) {
				String k = kv.substring(0, equalsIndex);
				String v = kv.substring(equalsIndex + 1);
				System.setProperty(k, v);
			}
			else
				System.setProperty(kv, "");
		}
		return otherArgs;
	}
}
