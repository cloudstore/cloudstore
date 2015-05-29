package co.codewizards.cloudstore.core.jmockit;

import java.util.concurrent.Semaphore;

import org.junit.After;
import org.junit.Before;
/**
 * Base class for tests that use JMockit. All such tests should extend this class,
 * because they may fail when executed parallelly.
 * @see <a href="https://groups.google.com/d/msg/jmockit-users/vc-7J3lIrlM/fse1P5Bv5E4J">https://groups.google.com/d/msg/jmockit-users/vc-7J3lIrlM/fse1P5Bv5E4J</a>
 * @author Wojtek Wilk wilk.wojtek at gmail.com
 *
 */
public class BaseJMockitTest {

	private static final Semaphore SEMAPHORE = new Semaphore(1);

	@Before
	public final void setUp() throws InterruptedException{
		SEMAPHORE.acquire();
	}

	@After
	public final void tearDown(){
		SEMAPHORE.release();
	}
}
