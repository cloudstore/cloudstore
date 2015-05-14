package co.codewizards.cloudstore.ls.core.invoke.filter;


/**
 * Filter denying all invocations targeting a class in the <code>java.lang.reflect</code> package.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class DenyReflectInvocationFilter extends AbstractInvocationFilter {

	@Override
	public int getPriority() {
		return 500;
	}

	@Override
	public Boolean canInvoke(final ExtMethodInvocationRequest extMethodInvocationRequest) {
		if (extMethodInvocationRequest.getTargetClass().getName().startsWith("java.lang.reflect."))
			return false;

		return null;
	}
}
