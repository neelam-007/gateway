package com.l7tech.external.assertions.websocket.server;

import com.l7tech.external.assertions.websocket.WebSocketConnectionEntity;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.test.BugId;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;

import javax.net.ssl.TrustManager;
import java.lang.reflect.Method;
import java.security.SecureRandom;

import static com.l7tech.objectmodel.Goid.DEFAULT_GOID;
import static org.junit.Assert.assertEquals;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class WebSocketConnectionManagerTest {

    @Mock
    private static SsgKeyStoreManager keyStoreManager;

    @Mock
    private static TrustManager trustManager;

    @Mock
    private static SecureRandom secureRandom;

    @Mock
    private static DefaultKey defaultKey;

    private static WebSocketConnectionManager webSocketConnectionManager;

    private WebSocketConnectionEntity connectionEntity;

    @BeforeClass
    public static void init() throws WebSocketConnectionManagerException {
        WebSocketConnectionManager.createConnectionManager(keyStoreManager, trustManager, secureRandom, defaultKey);
        webSocketConnectionManager = WebSocketConnectionManager.getInstance();
    }

    @Before
    public void setUp(){
        // configure entity with valid dummy settings
        connectionEntity = new WebSocketConnectionEntity();
        connectionEntity.setGoid(DEFAULT_GOID);
        connectionEntity.setOutboundCipherSuites(new String[] {"TLS_RSA_WITH_AES_128_CBC_SHA"});
        connectionEntity.setOutboundTlsProtocols(new String[] {"TLSv1.2"});
        connectionEntity.setOutboundPrivateKeyId(DEFAULT_GOID);
    }

    @After
    public void tearDown() {
        // remove existing clients and SSL context factories
        webSocketConnectionManager.deregisterOutboundHandler(DEFAULT_GOID.toString());
    }

    @BugId("DE364397")
    @Test
    public void getOutboundSslCtxFactory_GivenValidParam_LifecycleStateStarted() throws Exception {
        Method method = getAccessibleGetOutboundSslCtxFactory();

        SslContextFactory sslContextFactory = (SslContextFactory) method.invoke(webSocketConnectionManager, connectionEntity);

        // sslContextFactory should be started
        assertEquals(AbstractLifeCycle.STARTED, sslContextFactory.getState());
    }

    @BugId("DE364397")
    @Test
    public void getOutboundSslCtxFactory_InvokedTwiceWithSameParam_SameInstanceReturnedWithoutStateChange() throws Exception {
        Method method = getAccessibleGetOutboundSslCtxFactory();

        SslContextFactory sslContextFactoryFirst = (SslContextFactory) method.invoke(webSocketConnectionManager, connectionEntity);

        // initial call expected to create and start new instance
        assertEquals(AbstractLifeCycle.STARTED, sslContextFactoryFirst.getState());

        // stop sslContextFactory
        sslContextFactoryFirst.stop();
        assertEquals(AbstractLifeCycle.STOPPED, sslContextFactoryFirst.getState());

        // subsequent call must return the same instance, must not be re-started
        SslContextFactory sslContextFactorySecond = (SslContextFactory) method.invoke(webSocketConnectionManager, connectionEntity);

        assertEquals(sslContextFactoryFirst, sslContextFactorySecond);

        // sslContextFactory should be still be stopped
        assertEquals(AbstractLifeCycle.STOPPED, sslContextFactorySecond.getState());
    }

    /**
     * WebSocketConnectionManager.getOutboundSslCtxFactory(WebSocketConnectionEntity) is private - this provides an
     * accessible reference to it for testing
     * @return Method for WebSocketConnectionManager.getOutboundSslCtxFactory(WebSocketConnectionEntity) that is accessible
     */
    @NotNull
    private Method getAccessibleGetOutboundSslCtxFactory() throws NoSuchMethodException {
        Method method = webSocketConnectionManager.getClass().getDeclaredMethod("getOutboundSslCtxFactory", WebSocketConnectionEntity.class);
        method.setAccessible(true);
        return method;
    }
}
