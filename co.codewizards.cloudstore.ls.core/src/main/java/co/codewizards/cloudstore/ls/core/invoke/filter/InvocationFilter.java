package co.codewizards.cloudstore.ls.core.invoke.filter;


public interface InvocationFilter {

	int getPriority();

	Boolean canInvoke(ExtMethodInvocationRequest extMethodInvocationRequest);

}
