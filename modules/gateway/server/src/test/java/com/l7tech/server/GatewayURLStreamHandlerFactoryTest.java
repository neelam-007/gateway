package com.l7tech.server;

import org.junit.BeforeClass;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import static org.junit.Assert.assertNotNull;

/**
 * Unit testing for {@link GatewayURLStreamHandlerFactory}
 */
public class GatewayURLStreamHandlerFactoryTest {

    @BeforeClass
    public static void install() throws LifecycleException {
        GatewayURLStreamHandlerFactory.install();
    }

    /**
     * Test with java default protocol handling assert that gateway does not mess up with defaults.
     */
    @Test
    public void testDefaultProtocol() throws MalformedURLException {
        URL url = this.getClass().getClassLoader().getResource("com/l7tech/server/GatewayURLStreamHandlerFactoryTest.txt");
        assertNotNull("test file missing: GatewayURLStreamHandlerFactoryTest.txt", url);
        new URL(url.toExternalForm()); // expected is this to work without errors
    }

    /**
     * With a custom protocol but no handler behaviour should be an exception.
     */
    @Test(expected = MalformedURLException.class)
    public void testCustomProtocolNoHandler() throws MalformedURLException {
        new URL("gateway1://URLStreamTest"); // expected is this to throw exception
    }

    /**
     * Custom protocol and handler should work fine.
     */
    @Test
    public void testCustomProtocolWithHandler() throws MalformedURLException {
        GatewayURLStreamHandlerFactory.registerHandlerFactory("gateway2", protocol -> new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) {
                return null;
            }
        });

        new URL("gateway2://URLStreamTest"); // expected is this work properly using the registered handler
    }
}
