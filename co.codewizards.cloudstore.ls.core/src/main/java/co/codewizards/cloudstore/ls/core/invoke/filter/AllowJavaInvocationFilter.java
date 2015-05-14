package co.codewizards.cloudstore.ls.core.invoke.filter;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class AllowJavaInvocationFilter extends AbstractInvocationFilter {

	@Override
	public Boolean canInvoke(final ExtMethodInvocationRequest extMethodInvocationRequest) {
		final Class<?> targetClass = extMethodInvocationRequest.getTargetClass();
		if (Collection.class.isAssignableFrom(targetClass)
				|| Map.class.isAssignableFrom(targetClass)
				|| Iterator.class.isAssignableFrom(targetClass)
				|| PropertyChangeListener.class.isAssignableFrom(targetClass))
			return true;

		final String methodName = extMethodInvocationRequest.getMethodInvocationRequest().getMethodName();

		if (System.class.equals(targetClass) && "currentTimeMillis".equals(methodName))
			return true;

		return null;
	}

}
