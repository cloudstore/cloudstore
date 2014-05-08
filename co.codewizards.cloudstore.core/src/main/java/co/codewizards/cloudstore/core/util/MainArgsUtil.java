package co.codewizards.cloudstore.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MainArgsUtil {
	private static final Logger logger = LoggerFactory.getLogger(MainArgsUtil.class);

	private MainArgsUtil() { }

	public static String[][] separateSystemPropertiesFromOtherArgs(String[] args) {
		if (logger.isDebugEnabled())
			logger.debug("separateSystemPropertiesFromOtherArgs: args={}", Arrays.toString(args));

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
				logger.debug("extractAndApplySystemPropertiesReturnOthers: k='{}' v='{}'", k, v);
				System.setProperty(k, v);
			}
			else {
				logger.debug("extractAndApplySystemPropertiesReturnOthers: kv='{}'", kv);
				System.setProperty(kv, "");
			}
		}
		return otherArgs;
	}
}
