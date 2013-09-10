package com.l7tech.server.admin;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.test.BugId;
import com.l7tech.util.Either;
import com.l7tech.util.Option;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

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
    @BugId("SSG-5798")
    public void testRegisterInterfaceNoAnnotations() throws Exception {
        assertFalse(manager.isInterfaceRegistered(TestFaceNoSec.class.getName(), null));
        try {
            manager.registerInterface(TestFaceNoSec.class, null, new TestFaceNoSecImpl());
            fail("Expected exception was not thrown (interface not annotated with @Secured)");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("@Secured"));
        }
    }

    @Test
    @BugId("SSG-5798")
    public void testRegisterInterfaceNoSecurityCheck() throws Exception {
        // Allow registration of unannotated interface if allowUnsecured=true
        assertFalse(manager.isInterfaceRegistered(TestFaceNoSec.class.getName(), null));
        manager.registerInterface(TestFaceNoSec.class, null, new TestFaceNoSecImpl(), true);
        assertTrue(manager.isInterfaceRegistered(TestFaceNoSec.class.getName(), null));
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
        Either<Throwable,Option<Object>> result = manager.invokeExtensionMethod(TestFace.class.getName(), null, "echo", new Class[]{String.class}, new Object[]{"whatToEcho"});
        assertEquals("Echo: whatToEcho", result.right().toNull());
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

        Either<Throwable,Option<Object>> result;

        result = manager.invokeExtensionMethod(TestFace.class.getName(), "a", "echo", new Class[]{String.class}, new Object[]{"whatToEcho"});
        assertEquals("(From a)Echo: whatToEcho", result.right().toNull());

        result = manager.invokeExtensionMethod(TestFace.class.getName(), "b", "echo", new Class[]{String.class}, new Object[]{"whatToEcho"});
        assertEquals("(From b)Echo: whatToEcho", result.right().toNull());
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

    @Secured
    public static interface TestFace {
        @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
        String echo(String in);

        @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
        void fail(String msg) throws IOException;
    }

    public static interface TestFaceNoSec {
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

    public static class TestFaceNoSecImpl implements TestFaceNoSec {
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
