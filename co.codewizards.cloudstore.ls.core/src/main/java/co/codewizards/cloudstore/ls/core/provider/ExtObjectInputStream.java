package co.codewizards.cloudstore.ls.core.provider;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

class ExtObjectInputStream extends ObjectInputStream
{
	private ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
	private ClassLoader staticClassLoader = JavaNativeMessageBodyReader.getClassLoader();

	public ExtObjectInputStream(InputStream in) throws IOException {
		super(in);
	}

	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException
	{
		try {
			return super.resolveClass(desc);
		} catch (final ClassNotFoundException x) {
			doNothing(); // try again with ContextClassLoader
		} catch (final NoClassDefFoundError x) {
			doNothing(); // try again with ContextClassLoader
		}

		try {
			return Class.forName(desc.getName(), false, contextClassLoader);
		} catch (final ClassNotFoundException x) {
			if (staticClassLoader == null)
				throw x;
		} catch (final NoClassDefFoundError x) {
			if (staticClassLoader == null)
				throw x;
		}

		return Class.forName(desc.getName(), false, staticClassLoader);
	}
}