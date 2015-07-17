package co.codewizards.cloudstore.ls.core.invoke.filter;

import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

		if (ByteArrayInputStream.class.equals(targetClass) || ByteArrayOutputStream.class.equals(targetClass))
			return true;

		final Object[] arguments = extMethodInvocationRequest.getMethodInvocationRequest().getArguments();
		if ("hashCode".equals(methodName) && (arguments == null || arguments.length == 0))
			return true;

		if ("equals".equals(methodName) && arguments != null && arguments.length == 1)
			return true;

		if ("toString".equals(methodName) && (arguments == null || arguments.length == 0))
			return true;

		return null;
	}

}
