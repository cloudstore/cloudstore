package co.codewizards.cloudstore.ls.core.invoke.filter;

public abstract class AbstractInvocationFilter implements InvocationFilter {

	@Override
	public int getPriority() {
		return 0;
	}
}
