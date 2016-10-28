package co.codewizards.cloudstore.ls.core.invoke;

import static org.assertj.core.api.Assertions.*;

import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import co.codewizards.cloudstore.core.Uid;

public class ClassManagerTest {

	private ClassManager classManager;

	@Before
	public void before() {
		classManager = new ClassManager(new Uid());
	}

	@After
	public void after() {
		classManager = null;
	}

	@Test
	public void classInfo_equalsOverridden() {
		int classId = classManager.getClassIdOrCreate(Uid.class);
		ClassInfo classInfo = classManager.getClassInfo(classId);
		assertThat(classInfo.isEqualsOverridden()).isTrue();

		classId = classManager.getClassIdOrCreate(ClassManagerTest.class);
		classInfo = classManager.getClassInfo(classId);
		assertThat(classInfo.isEqualsOverridden()).isFalse();

		Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { PropertyChangeListener.class }, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				return null;
			}
		});

		classId = classManager.getClassIdOrCreate(proxy.getClass());
		classInfo = classManager.getClassInfo(classId);
		assertThat(classInfo.isEqualsOverridden()).isTrue();
	}
}
