package co.codewizards.cloudstore.ls.core.invoke.refclean;

import co.codewizards.cloudstore.ls.core.invoke.filter.ExtMethodInvocationRequest;

public abstract class AbstractReferenceCleaner implements ReferenceCleaner {

	@Override
	public void preInvoke(ExtMethodInvocationRequest extMethodInvocationRequest) {
	}

	@Override
	public void postInvoke(ExtMethodInvocationRequest extMethodInvocationRequest, Object resultObject, Throwable error) {
	}

}
