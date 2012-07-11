package com.l7tech.external.assertions.mqnative.server;


import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.l7tech.external.assertions.mqnative.MqNativeAdmin;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import static com.ibm.mq.constants.MQConstants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MqNativeAdminServerSupportTest extends AbstractJUnit4SpringContextTests {

    @Mock
    private ApplicationContext context;
    @Mock
    private SsgActiveConnector connector;
    @Mock
    private MQQueueManager queueManager;
    @Mock
    private MQException exception;

    private ServerConfig serverConfig;

    @Before
    public void setup() {
          serverConfig = ApplicationContexts.getTestApplicationContext().getBean("serverConfig", ServerConfig.class);
    }

    @Test
    public void getInstanceTest() throws MqNativeAdmin.MqNativeTestException {

        MqNativeAdminServerSupport mqNativeAdminServerSupport = MqNativeAdminServerSupport.getInstance(context);

        when(context.getBean("serverConfig",ServerConfig.class)).thenReturn(serverConfig);

        assertTrue(mqNativeAdminServerSupport instanceof MqNativeAdminServerSupport);

        mqNativeAdminServerSupport.init(context);
        assertEquals(mqNativeAdminServerSupport.getDefaultMqMessageMaxBytes(),2621440L);
    }

    @Test
    public void testMessageDetail() {

        MqNativeAdminServerSupport mqNativeAdminServerSupport = MqNativeAdminServerSupport.getInstance(context);

        when(exception.getReason()).thenReturn(MQRC_CONNECTION_BROKEN)
                                   .thenReturn(MQRC_NOT_AUTHORIZED)
                                   .thenReturn(MQRC_Q_MGR_NAME_ERROR)
                                   .thenReturn(MQRC_Q_MGR_NOT_AVAILABLE)
                                   .thenReturn(MQRC_UNKNOWN_OBJECT_NAME)
                                   .thenReturn(MQRC_JSSE_ERROR)
                                   .thenReturn(0);

        assertEquals(mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception),"Invalid channel name");
        assertEquals(mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception), "The user is not authorized to perform the operation attempted");
        assertEquals(mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception),"Invalid queue manager name");
        assertEquals(mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception),"Invalid host name or port number");
        assertEquals(mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception),"Invalid queue name, reply queue name, or failure queue name");
        assertEquals(mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception),"Invalid SSL setting");
        mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception);  // covers last catchall case.
    }
}
