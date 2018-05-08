package com.l7tech.external.assertions.websocket.server;

import com.l7tech.server.GatewayState;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import java.util.logging.Logger;

/**
 * Test the WebSocketOutboundAssertion.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerWebSocketAssertionTest {

    private static final Logger log = Logger.getLogger(ServerWebSocketAssertionTest.class.getName());

    @Mock
    private ApplicationContext applicationContext;
    private ClusterPropertyManager clusterPropertyManager;
    @Mock
    private GenericEntityManager genericEntityManager;
    private GatewayState gatewayState = new GatewayState();
    private ApplicationEventProxy applicationEventProxy = new ApplicationEventProxy();

    @Before
    public void before() {
        clusterPropertyManager = new MockClusterPropertyManager();
        Mockito.when(applicationContext.getBean(Mockito.eq("clusterPropertyManager"), Mockito.eq(ClusterPropertyManager.class))).thenReturn(clusterPropertyManager);
        Mockito.when(applicationContext.getBean(Mockito.eq("genericEntityManager"), Mockito.eq(GenericEntityManager.class))).thenReturn(genericEntityManager);
        Mockito.when(applicationContext.getBean(Mockito.eq("gatewayState"), Mockito.eq(GatewayState.class))).thenReturn(gatewayState);
        Mockito.when(applicationContext.getBean(Mockito.eq("applicationEventProxy"), Mockito.eq(ApplicationEventProxy.class))).thenReturn(applicationEventProxy);
        WebSocketLoadListener.onModuleLoaded(applicationContext);
    }

    @Test
    public void getTimePropOKPath() throws Exception {
        final String key = "myKey";

        //no such key
        int result = WebSocketLoadListener.getTimeProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 123, result);

        // 1 minute
        clusterPropertyManager.putProperty(key, "1m");
        result = WebSocketLoadListener.getTimeProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 60000, result);

        // 1 without unit
        clusterPropertyManager.putProperty(key, "1");
        result = WebSocketLoadListener.getTimeProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 1, result);

        // 1 hour
        clusterPropertyManager.putProperty(key, "1h");
        result = WebSocketLoadListener.getTimeProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 3600000, result);

        // null should be default
        clusterPropertyManager.putProperty(key, null);
        result = WebSocketLoadListener.getTimeProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 123, result);

        // empty should be default
        clusterPropertyManager.putProperty(key, "");
        result = WebSocketLoadListener.getTimeProp(key, 456);
        Assert.assertEquals("Unexpected property key returned", 456, result);

        // time too long should be default.
        clusterPropertyManager.putProperty(key, "100000000d");
        result = WebSocketLoadListener.getTimeProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 123, result);

        // time unit with invalid suffix, should return value in mills
        clusterPropertyManager.putProperty(key, "10q");
        result = WebSocketLoadListener.getTimeProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 10, result);

        // not a integer should return default
        clusterPropertyManager.putProperty(key, "asd");
        result = WebSocketLoadListener.getTimeProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 123, result);

        // not a integer (alphanumeric) should return default
        clusterPropertyManager.putProperty(key, "1a2s3d");
        result = WebSocketLoadListener.getTimeProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 123, result);

        // not a integer (invalid prefix) should return default
        clusterPropertyManager.putProperty(key, "asd123");
        result = WebSocketLoadListener.getTimeProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 123, result);
    }

    @Test
    public void getIntegerPropOKPath() throws Exception {
        final String key = "myKey";

        //no such key
        int result = WebSocketLoadListener.getIntegerProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 123, result);

        // time unit should return default
        clusterPropertyManager.putProperty(key, "1m");
        result = WebSocketLoadListener.getIntegerProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 123, result);

        // 1 without unit
        clusterPropertyManager.putProperty(key, "1");
        result = WebSocketLoadListener.getIntegerProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 1, result);

        // time unit should return default
        clusterPropertyManager.putProperty(key, "1h");
        result = WebSocketLoadListener.getIntegerProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 123, result);

        // null should be default
        clusterPropertyManager.putProperty(key, null);
        result = WebSocketLoadListener.getIntegerProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 123, result);

        // empty should be default
        clusterPropertyManager.putProperty(key, "");
        result = WebSocketLoadListener.getIntegerProp(key, 456);
        Assert.assertEquals("Unexpected property key returned", 456, result);

        // integer too long should be default.
        clusterPropertyManager.putProperty(key, Integer.MAX_VALUE + "0");
        result = WebSocketLoadListener.getIntegerProp(key, 123);
        Assert.assertEquals("Unexpected property key returned", 123, result);
    }

}
