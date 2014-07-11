package com.l7tech.external.assertions.websocket.client;

import com.l7tech.external.assertions.websocket.WebSocketUtils;
import com.l7tech.external.assertions.websocket.server.WebSocketConnectionManagerException;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 6/19/12
 * Time: 8:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketTestClient {

    private static final Logger log = Logger.getLogger(WebSocketTestClient.class.getName());
    private static final String hostname = "cirving-01088";
    private static ChatServerTestServer testServer;

    @BeforeClass
    public static void oneTimeSetUp() {
        try {
            WebSocketClientConnectionManager.createConnectionManager();
            testServer = new ChatServerTestServer();
            testServer.start();
        } catch (WebSocketConnectionManagerException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @AfterClass
    public static void oneTimeTearDown() {
            if ( testServer != null) {
                testServer.stop();
            }
    }

    @Test
     public void testChat() throws Exception {
        WebSocketClientHandler handler = new WebSocketClientHandler();
        handler.createConnection(new URI("ws://" + hostname + ":8081/"), false, null);

        handler.sendMessage("Hello");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("Hello", handler.getMessage());

        handler.sendMessage("ola");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("WebSockets rock!!", handler.getMessage());

        handler.sendMessage("");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("", handler.getMessage());
    }

    @Test
    public void testLoopback() throws Exception {
        WebSocketClientHandler handler = new WebSocketClientHandler();
        handler.createConnection(new URI("ws://" + hostname + ":8083/"), false, null);

        handler.sendMessage("Hello");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("Hello", handler.getMessage());

        handler.sendMessage("ola");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("hola", handler.getMessage());

        handler.sendMessage("");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("", handler.getMessage());

    }

    @Test
    public void testBinary() throws Exception {
        WebSocketClientHandler handler = new WebSocketClientHandler();
        handler.createConnection(new URI("ws://" + hostname + ":8081/"), false, null);

        String s = "This is my byte array";
        byte[] bytes = s.getBytes();

        handler.sendMessage(bytes, 0, bytes.length);
        while (!handler.hasMessage()) { Thread.sleep(100);}
        if ( handler.getBinMsg() == null) {
            Assert.fail();
        } else {
            Assert.assertEquals(s, new String(handler.getBinMsg()));
        }

    }

    @Test(expected = ConnectException.class)
    public void testNoServer() throws Exception {
        WebSocketClientHandler handler = new WebSocketClientHandler();
        handler.createConnection(new URI("ws://" + hostname + ":8095/"), false, null);
        Assert.fail();
    }

    @Test(expected = ProtocolException.class)
    public void testLowConnections() throws Exception {
        WebSocketClientHandler handler = new WebSocketClientHandler();
        handler.createConnection(new URI("ws://" + hostname + ":8099/"), false, null);

        handler.sendMessage("Hello");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("Hello", handler.getMessage());

        WebSocketClientHandler handler2 = new WebSocketClientHandler();
        handler2.createConnection(new URI("ws://" + hostname + ":8099/"), false, null);

        Assert.fail();
    }

    @Test
    public void testSSL() throws Exception {
        WebSocketClientHandler handler = new WebSocketClientHandler();
        handler.createConnection(new URI("wss://" + hostname + ":9013/"), true, null);

        handler.sendMessage("Hello");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("Hello", handler.getMessage());

        handler.sendMessage("ola");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("hola", handler.getMessage());

        handler.sendMessage("");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("", handler.getMessage());

    }

    @Test
    public void testSSLRequiredClientAuth() throws Exception {
        WebSocketClientHandler handler = new WebSocketClientHandler();
        handler.createConnection(new URI("wss://" + hostname + ":9014/"), true, null);

        handler.sendMessage("Hello");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("Hello", handler.getMessage());

        handler.sendMessage("ola");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("hola", handler.getMessage());

        handler.sendMessage("");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("", handler.getMessage());

    }

    @Test
    public void testSSLOptionalClientAuth() throws Exception {
        WebSocketClientHandler handler = new WebSocketClientHandler();
        handler.createConnection(new URI("wss://" + hostname + ":9012/"), true, null);

        handler.sendMessage("Hello");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("Hello", handler.getMessage());

        handler.sendMessage("ola");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("hola", handler.getMessage());

        handler.sendMessage("");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("", handler.getMessage());

    }

    @Test(expected = EOFException.class)
    public void testInvalidSSL() throws Exception {
        WebSocketClientHandler handler = new WebSocketClientHandler();
        handler.createConnection(new URI("wss://" + hostname + ":9014/"), false, null);
        Assert.fail();
    }

    @Test()
    public void testHttpBasic() throws Exception {
        WebSocketClientHandler handler = new WebSocketClientHandler();
        Map<String, String> headers = new ConcurrentHashMap<String, String>();
        String token = "Basic " + "d3N0ZXN0OndzdGVzdA==";
        headers.put("Authorization", token);

        handler.createConnection(new URI("ws://" + hostname + ":9001/"), false, headers);

        handler.sendMessage("Hello");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("Hello", handler.getMessage());

        handler.sendMessage("ola");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("WebSockets rock!!", handler.getMessage());
    }

    @Test(expected = ProtocolException.class)
    public void testHttpBasicBadCredentials() throws Exception {
        WebSocketClientHandler handler = new WebSocketClientHandler();
        Map<String, String> headers = new ConcurrentHashMap<String, String>();
        String token = "Basic " + "abcdefGHIJKLdGVzdA==";
        headers.put("Authorization", token);

        handler.createConnection(new URI("ws://" + hostname + ":9001/"), false, headers);

        Assert.fail();
    }

    @Test()
    public void testOAuth() throws Exception {
        WebSocketClientHandler handler = new WebSocketClientHandler();
        Map<String, String> headers = new ConcurrentHashMap<String, String>();
        String token = "Bearer " + System.getProperty("oauthToken");
        headers.put("Authorization", token);

        handler.createConnection(new URI("ws://" + hostname + ":9015/"), false, headers);

        handler.sendMessage("Hello");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("Hello", handler.getMessage());

        handler.sendMessage("ola");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("WebSockets rock!!", handler.getMessage());
    }

    @Test(expected = ProtocolException.class)
    public void testOAuthBadToken() throws Exception {
        WebSocketClientHandler handler = new WebSocketClientHandler();
        Map<String, String> headers = new ConcurrentHashMap<String, String>();
        String token = "Bearer " + "f3f9dbed-651e-41cc-b38c-c7059f98d79f";
        headers.put("Authorization", token);

        handler.createConnection(new URI("ws://" + hostname + ":9015/"), false, headers);

        Assert.fail();
    }

    @Test
    public void testOAuthSSL() throws Exception {
        WebSocketClientHandler handler = new WebSocketClientHandler();
        Map<String, String> headers = new ConcurrentHashMap<String, String>();
        String token = "Bearer " + System.getProperty("oauthToken");
        headers.put("Authorization", token);

        handler.createConnection(new URI("wss://" + hostname + ":9016/"), true, headers);

        handler.sendMessage("Hello");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("Hello", handler.getMessage());

        handler.sendMessage("ola");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("hola", handler.getMessage());

        handler.sendMessage("");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("", handler.getMessage());

    }

    @Test(expected = IOException.class)
    public void testServerCrash() throws Exception {
        WebSocketClientHandler handler = new WebSocketClientHandler();
        handler.createConnection(new URI("ws://" + hostname + ":8081/"), false, null);

        handler.sendMessage("Hello");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("Hello", handler.getMessage());

        handler.sendMessage("ola");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertEquals("WebSockets rock!!", handler.getMessage());

        testServer.stop();
        handler.sendMessage("Hello");
        while (!handler.hasMessage()) { Thread.sleep(100);}
        Assert.assertNull(handler.getMessage());

        testServer.start();

        //Connection should be closed
        handler.sendMessage("ola");
        Assert.fail();
    }
}
