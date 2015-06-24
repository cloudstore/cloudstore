package co.codewizards.cloudstore.ls.core.invoke.refclean;

import java.beans.PropertyChangeListener;

import co.codewizards.cloudstore.core.bean.PropertyBase;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.filter.ExtMethodInvocationRequest;

public class PropertyChangeListenerCleaner extends AbstractReferenceCleaner {

	@Override
	public void preInvoke(final ExtMethodInvocationRequest extMethodInvocationRequest) {
		final MethodInvocationRequest methodInvocationRequest = extMethodInvocationRequest.getMethodInvocationRequest();

		final Object object = methodInvocationRequest.getObject();
		final String methodName = methodInvocationRequest.getMethodName();
		final Object[] arguments = methodInvocationRequest.getArguments();

		if (object == null)
			return;

		PropertyBase property = null;
		String propertyName = null;
		PropertyChangeListener listener = null;
		if (arguments.length == 1 && arguments[0] instanceof PropertyChangeListener)
			listener = (PropertyChangeListener) arguments[0];
		else if (arguments.length == 2 && arguments[1] instanceof PropertyChangeListener) {
			listener = (PropertyChangeListener) arguments[1];
			if (arguments[0] instanceof PropertyBase)
				property = (PropertyBase) arguments[0];
			else if (arguments[0] instanceof String)
				propertyName = (String) arguments[0];
			else
				return;
		}
		else
			return;

//		if ("addPropertyChangeListener".equals(methodName))
//			trackAddPropertyChangeListener(methodInvocationRequest);
//		else if ("removePropertyChangeListener".equals(methodName))
//			trackRemovePropertyChangeListener(methodInvocationRequest);
	}

	private void trackAddPropertyChangeListener(final Object object, final PropertyBase property, final String propertyName, final PropertyChangeListener listener) {

	}

	private void trackRemovePropertyChangeListener(MethodInvocationRequest methodInvocationRequest) {
		// TODO Auto-generated method stub

	}


	@Override
	public void cleanUp() {
		// TODO Auto-generated method stub

	}

}
