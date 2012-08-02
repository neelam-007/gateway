package com.l7tech.gui.util;

import com.l7tech.util.SyspropUtil;
import org.junit.*;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Unit test for {@link SwingWorker}.
 */
public class SwingWorkerTest {

    private static final String WORKER_EXCEPTION_MESSAGE = "Error from worker!";
    private long testValue = 438374L;
    private boolean shouldThrow = false;

    static final AtomicReference<Throwable> handledException = new AtomicReference<Throwable>();

    SwingWorker sw = new SwingWorker() {
        @Override
        public Object construct() {
            try {
                Thread.sleep(250);
                if (shouldThrow)
                    throw new RuntimeException(WORKER_EXCEPTION_MESSAGE);
                return testValue;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    };

    @Before
    public void before() {
        handledException.set(null);
    }

    @After
    public  void after() {
        handledException.set(null);
        cleanupSystemProperties();
    }

    @BeforeClass
    public static void beforeClass() {
        cleanupSystemProperties();
    }

    @AfterClass
    public static void afterClass() {
        cleanupSystemProperties();
    }

    private static void cleanupSystemProperties() {
        SyspropUtil.clearProperties("sun.awt.exception.handler");
    }

    @Test
    public void testConstruct() throws Exception {
        sw.start();
        assertTrue(sw.isAlive());
        assertNull(sw.getValue());
        assertEquals(testValue, sw.get());
        assertFalse(sw.isAlive());
    }

    @Test
    public void testInterrupt() throws Exception {
        sw.start();
        assertTrue(sw.isAlive());
        sw.interrupt();
        assertNull(sw.get());
        assertFalse(sw.isAlive());
    }

    @Test
    public void testNoErrorHandler() throws Exception {
        shouldThrow = true;
        sw.start();
        assertNull(sw.get());
        Throwable t = handledException.get();
        assertNull(t);
    }

    @Test
    public void testErrorHandler() throws Exception {
        SyspropUtil.setProperty("sun.awt.exception.handler", SwingWorkerTestErrorHandler.class.getName());
        shouldThrow = true;
        sw.start();
        assertNull(sw.get());
        Throwable t = handledException.get();
        assertNotNull(t);
        assertTrue(RuntimeException.class.equals(t.getClass()));
        assertEquals(WORKER_EXCEPTION_MESSAGE, t.getMessage());
    }

    public static class SwingWorkerTestErrorHandler {
        public void handle(final Throwable t) {
            handledException.set(t);
        }
    }
}
