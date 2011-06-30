package com.l7tech.server.admin;

import com.l7tech.util.Either;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test case for {@link ExtensionInterfaceManager}.
 */
public class ExtensionInterfaceManagerTest {
    ExtensionInterfaceManager manager = new ExtensionInterfaceManager(null, null, null);

    @Test
    public void testRegisterInterface() throws Exception {
        assertFalse(manager.isInterfaceRegistered(TestFace.class.getName(), null));
        manager.registerInterface(TestFace.class, null, new TestImpl());
        assertTrue(manager.isInterfaceRegistered(TestFace.class.getName(), null));
    }

    @Test
    public void testUnRegisterInterface() throws Exception {
        assertFalse(manager.isInterfaceRegistered(TestFace.class.getName(), null));
        manager.registerInterface(TestFace.class, null, new TestImpl());
        assertTrue(manager.isInterfaceRegistered(TestFace.class.getName(), null));
        manager.unRegisterInterface(TestFace.class.getName(), null);
        assertFalse(manager.isInterfaceRegistered(TestFace.class.getName(), null));
    }

    @Test
    public void testInvocation() throws Exception {
        manager.registerInterface(TestFace.class, null, new TestImpl());
        Either<Object, Throwable> result = manager.invokeExtensionMethod(TestFace.class.getName(), null, "echo", new Class[]{String.class}, new Object[]{"whatToEcho"});
        assertEquals("Echo: whatToEcho", result.left());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterDuplicate() throws Exception {
        manager.registerInterface(TestFace.class, null, new TestImpl());
        manager.registerInterface(TestFace.class, null, new TestImpl());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterDuplicateName() throws Exception {
        manager.registerInterface(TestFace.class, "a", new TestImpl());
        manager.registerInterface(TestFace.class, "a", new TestImpl());
    }

    @Test
    public void testRegisterDifferentName() throws Exception {
        manager.registerInterface(TestFace.class, "a", new TestImpl() {
            @Override
            public String echo(String in) {
                return "(From a)" + super.echo(in);
            }
        });

        manager.registerInterface(TestFace.class, "b", new TestImpl() {
            @Override
            public String echo(String in) {
                return "(From b)" + super.echo(in);
            }
        });

        Either<Object, Throwable> result;

        result = manager.invokeExtensionMethod(TestFace.class.getName(), "a", "echo", new Class[]{String.class}, new Object[]{"whatToEcho"});
        assertEquals("(From a)Echo: whatToEcho", result.left());

        result = manager.invokeExtensionMethod(TestFace.class.getName(), "b", "echo", new Class[]{String.class}, new Object[]{"whatToEcho"});
        assertEquals("(From b)Echo: whatToEcho", result.left());
    }

    @Test
    public void testUnregisterAllFromClassLoader() {
        manager.registerInterface(TestFace.class, "a", new TestImpl());
        manager.registerInterface(TestFace.class, "b", new TestImpl());
        manager.registerInterface(TestFace.class, "c", new TestImpl());
        manager.registerInterface(TestFace.class, null, new TestImpl());
        assertEquals(4, manager.getRegisteredInterfaces().size());
        manager.unregisterAllFromClassLoader(TestImpl.class.getClassLoader());
        assertTrue(manager.getRegisteredInterfaces().isEmpty());
    }

    public static interface TestFace {
        String echo(String in);
        void fail(String msg) throws IOException;
    }

    public static class TestImpl implements TestFace {
        @Override
        public String echo(String in) {
            return "Echo: " + in;
        }

        @Override
        public void fail(String msg) throws IOException {
            throw new IOException(msg);
        }
    }
}
