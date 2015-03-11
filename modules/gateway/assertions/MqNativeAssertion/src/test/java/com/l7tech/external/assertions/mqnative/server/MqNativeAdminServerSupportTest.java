package com.l7tech.external.assertions.mqnative.server;


import com.ibm.mq.MQException;
import com.l7tech.external.assertions.mqnative.MqNativeAdmin;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
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
    private MQException exception;

    private MqNativeAdminServerSupport mqNativeAdminServerSupport;

    @Before
    public void setup() {
        mqNativeAdminServerSupport = MqNativeAdminServerSupport.getInstance(context);
    }

    @Test
    public void getInstanceTest() throws MqNativeAdmin.MqNativeTestException {
        ServerConfig serverConfig = ServerConfig.getInstance();

        when(context.getBean("serverConfig", ServerConfig.class)).thenReturn(serverConfig);

        assertTrue(mqNativeAdminServerSupport != null);

        mqNativeAdminServerSupport.init(context);

        assertEquals(Long.parseLong(serverConfig.getProperty(ServerConfigParams.PARAM_IO_MQ_MESSAGE_MAX_BYTES)),
                mqNativeAdminServerSupport.getDefaultMqMessageMaxBytes());
    }

    @Test
    public void testMessageDetail() {
        when(exception.getReason()).thenReturn(MQRC_CONNECTION_BROKEN)
                                   .thenReturn(MQRC_NOT_AUTHORIZED)
                                   .thenReturn(MQRC_Q_MGR_NAME_ERROR)
                                   .thenReturn(MQRC_Q_MGR_NOT_AVAILABLE)
                                   .thenReturn(MQRC_UNKNOWN_OBJECT_NAME)
                                   .thenReturn(MQRC_JSSE_ERROR)
                                   .thenReturn(MQRC_HOST_NOT_AVAILABLE)
                                   .thenReturn(MQRC_UNKNOWN_CHANNEL_NAME)
                                   .thenReturn(0);

        assertEquals(mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception),"Invalid channel name");
        assertEquals(mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception), "The user is not authorized to perform the operation attempted");
        assertEquals(mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception),"Invalid queue manager name");
        assertEquals(mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception),"Cannot connect to MQ Queue Manager");
        assertEquals(mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception),"Invalid queue name, reply queue name, or failure queue name");
        assertEquals(mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception),"Invalid SSL setting");
        assertEquals(mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception),"Cannot communicate with MQ Queue Host");
        assertEquals(mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception),"Unknown MQ Channel Name");
        mqNativeAdminServerSupport.getMeaningfulMqErrorDetail(exception);  // covers last catchall case.
    }
}
