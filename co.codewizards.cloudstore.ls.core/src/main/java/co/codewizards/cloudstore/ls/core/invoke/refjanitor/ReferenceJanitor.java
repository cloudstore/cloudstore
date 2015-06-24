package co.codewizards.cloudstore.ls.core.invoke.refjanitor;

import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.filter.ExtMethodInvocationRequest;

/**
 * A {@code ReferenceJanitor}'s primary duty is to clean up references.
 * <p>
 * But it may manipulate a method invocation's arguments, too. Hence, it's not simply a "Cleaner" but
 * more a "Janitor" having an advanced job ;-)
 * <p>
 * In order to do his work, he is notified about all method invocations. So he can track which references (primarily
 * listeners) are registered where. Finally, he can clean the tangling references up, when an {@link ObjectManager} is evicted.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface ReferenceJanitor {

	int getPriority();

	void preInvoke(ExtMethodInvocationRequest extMethodInvocationRequest);

	void postInvoke(ExtMethodInvocationRequest extMethodInvocationRequest, Object resultObject, Throwable error);

	void cleanUp();

}
