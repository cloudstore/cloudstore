package co.codewizards.cloudstore.ls.core.invoke;

import javax.ws.rs.core.SecurityContext;

public interface ObjectRefConverterFactory {

	ObjectRefConverter createObjectRefConverter(SecurityContext securityContext);

}
