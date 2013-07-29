package com.l7tech.external.assertions.mqnative.server;

import com.l7tech.external.assertions.mqnative.MqNativeDynamicProperties;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Option;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MqNativeEndpointConfigTest {

    private static final String name = "name";
    private static final String queueName = "q";
    private static final String replyToQueueName = "q";

    @Mock
    private SsgActiveConnector ssgActiveConnector;
    @Mock
    private Option<String> password;
    @Mock
    private Option<MqNativeDynamicProperties> dynamicProperties;
    @Mock
    private MqNativeDynamicProperties realDynamicProperties;

    @Test
    public void constructorTest() throws MqNativeConfigException {

        when(ssgActiveConnector.getCopy()).thenReturn(ssgActiveConnector);
        when(ssgActiveConnector.getName()).thenReturn(name);
        when(ssgActiveConnector.getGoid()).thenReturn(new Goid(0,0));
        when(ssgActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_TEMPLATE_QUEUE)).thenReturn(false);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME)).thenReturn(queueName);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME)).thenReturn(replyToQueueName);
        when(ssgActiveConnector.getProperty(anyString(),anyString())).thenReturn("");

        when(dynamicProperties.isSome()).thenReturn(true);
        when(dynamicProperties.some()).thenReturn(realDynamicProperties);

        MqNativeEndpointConfig mqNativeEndpointConfig = new MqNativeEndpointConfig(ssgActiveConnector,new Option<String>("password"),dynamicProperties);

        assertFalse(mqNativeEndpointConfig.isDynamic());
        assertNotNull(mqNativeEndpointConfig.getMqEndpointKey());
        assertNull(mqNativeEndpointConfig.getQueueManagerName());
        assertFalse(mqNativeEndpointConfig.isCopyCorrelationId());
        assertEquals(mqNativeEndpointConfig.getReplyToQueueName(), replyToQueueName);
        assertNull(mqNativeEndpointConfig.getReplyType());
        assertEquals(mqNativeEndpointConfig.getQueueName(), queueName);
        assertNull(mqNativeEndpointConfig.getReplyToModelQueueName());
        assertEquals(mqNativeEndpointConfig.getName(), name);
        mqNativeEndpointConfig.validate();    // throws exception if not configured

        MqNativeEndpointConfig.MqNativeEndpointKey endEndpointKey = mqNativeEndpointConfig.getMqEndpointKey();

        assertEquals(endEndpointKey.getId(),new Goid(0,0));
        assertEquals(endEndpointKey.getVersion(),0);
        assertTrue(endEndpointKey.equals(new MqNativeEndpointConfig.MqNativeEndpointKey(new Goid(0,0), 0)));
        assertFalse(endEndpointKey.equals(new MqNativeEndpointConfig.MqNativeEndpointKey(new Goid(0,1), 0)));
        assertEquals(endEndpointKey.toString(),"MqNativeEndpointKey["+new Goid(0,0).toString()+",0]");
    }

}
