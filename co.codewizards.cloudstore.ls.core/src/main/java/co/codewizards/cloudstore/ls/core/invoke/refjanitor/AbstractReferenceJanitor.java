package co.codewizards.cloudstore.ls.core.invoke.refjanitor;

import co.codewizards.cloudstore.ls.core.invoke.filter.ExtMethodInvocationRequest;

public abstract class AbstractReferenceJanitor implements ReferenceJanitor {
	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public void preInvoke(ExtMethodInvocationRequest extMethodInvocationRequest) {
	}

	@Override
	public void postInvoke(ExtMethodInvocationRequest extMethodInvocationRequest, Object resultObject, Throwable error) {
	}

}
