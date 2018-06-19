package co.codewizards.cloudstore.ls.core.provider;

import javax.ws.rs.core.MediaType;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public interface MediaTypeConst
{
	String APPLICATION_JAVA_NATIVE = "application/java-native"; //$NON-NLS-1$
	MediaType APPLICATION_JAVA_NATIVE_TYPE = new MediaType("application", "java-native"); //$NON-NLS-1$ //$NON-NLS-2$

	String APPLICATION_JAVA_NATIVE_WITH_OBJECT_REF = "application/java-native+oref"; //$NON-NLS-1$
	MediaType APPLICATION_JAVA_NATIVE_WITH_OBJECT_REF_TYPE = new MediaType("application", "java-native+oref"); //$NON-NLS-1$ //$NON-NLS-2$
}
