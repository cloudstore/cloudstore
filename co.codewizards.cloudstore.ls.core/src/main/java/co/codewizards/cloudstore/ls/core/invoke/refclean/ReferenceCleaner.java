package co.codewizards.cloudstore.ls.core.invoke.refclean;

import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.filter.ExtMethodInvocationRequest;

/**
 * A {@code ReferenceCleaner} is notified about all method invocations in order to clean tangling references
 * when an {@link ObjectManager} is evicted.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface ReferenceCleaner {

	void preInvoke(ExtMethodInvocationRequest extMethodInvocationRequest);

	void postInvoke(ExtMethodInvocationRequest extMethodInvocationRequest, Object resultObject, Throwable error);

	void cleanUp();

}
