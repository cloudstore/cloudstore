package co.codewizards.cloudstore.ls.core.invoke.filter;

import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;

import java.lang.reflect.Proxy;

import co.codewizards.cloudstore.core.util.ReflectionUtil;

public class AllowCloudStoreInvocationFilter extends AbstractInvocationFilter {

	@Override
	public int getPriority() {
		return 100;
	}

	@Override
	public Boolean canInvoke(final ExtMethodInvocationRequest extMethodInvocationRequest) {
		final Class<?> targetClass = extMethodInvocationRequest.getTargetClass();
		if (isBlackListed(targetClass))
			return false;

		if (isWhiteListed(targetClass))
			return true;

		if (Proxy.isProxyClass(targetClass)) {
			for (final Class<?> iface : getAllInterfaces(targetClass)) {
				if (isWhiteListed(iface))
					return true;
			}
		}

		return null;
	}

	private boolean isBlackListed(Class<?> classOrInterface) {
		return ReflectionUtil.class.equals(classOrInterface);
	}

	private boolean isWhiteListed(Class<?> classOrInterface) {
		return classOrInterface.getName().startsWith("co.codewizards.cloudstore.");
	}
}
