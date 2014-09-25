package com.l7tech.external.assertions.mqnative.server.decorator;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.MQHeaderList;
import com.ibm.mq.headers.MQRFH2;
import com.l7tech.external.assertions.mqnative.server.MqMessageProxy;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Date;

import static com.ibm.mq.constants.CMQC.MQFMT_RF_HEADER_2;
import static junit.framework.Assert.assertEquals;


@RunWith(MockitoJUnitRunner.class)
public class HeaderDecoratorTest {

    @Mock
    private PolicyEnforcementContext context;
    @Mock
    private Audit audit;

    /**
     * Test over-writing message format value
     * @throws Exception
     */
    @Test
    public void testMessageFormat() throws Exception {
        MQMessage mqMessage = createMqMessage();
        MqMessageProxy mqMessageProxy = new MqMessageProxy(mqMessage);
        mqMessage = new PassThroughDecorator(mqMessage, mqMessageProxy, null, null, context, audit);
        mqMessage = new HeaderDecorator((MqMessageDecorator) mqMessage);
        mqMessage = ((MqMessageDecorator) mqMessage).decorate();
        assertEquals(mqMessage.format, MQFMT_RF_HEADER_2);
    }

    /**
     * Test over-writing message format value
     * @throws Exception
     */
    @Test
    public void testOverwriteMessageFormat() throws Exception {
        MQMessage mqMessage = createMqMessage();
        mqMessage.format = "1234";
        MqMessageProxy mqMessageProxy = new MqMessageProxy(mqMessage);
        mqMessage = new PassThroughDecorator(mqMessage, mqMessageProxy, null, null, context, audit);
        mqMessage = new HeaderDecorator((MqMessageDecorator) mqMessage);
        mqMessage = ((MqMessageDecorator) mqMessage).decorate();
        assertEquals(mqMessage.format, "1234");
    }

    /**
     * Test over-writing message format with null value
     * @throws Exception
     */
    @Test
    public void testNullOverwriteMessageFormat() throws Exception {
        MQMessage mqMessage = createMqMessage();
        mqMessage.format = null;
        MqMessageProxy mqMessageProxy = new MqMessageProxy(mqMessage);
        mqMessage = new PassThroughDecorator(mqMessage, mqMessageProxy, null, null, context, audit);
        mqMessage = new HeaderDecorator((MqMessageDecorator) mqMessage);
        mqMessage = ((MqMessageDecorator) mqMessage).decorate();
        assertEquals(mqMessage.format, null);
    }

    private MQMessage createMqMessage() throws IOException, MQException {
        MQMessage mqMessage = new MQMessage();
        mqMessage.format = MQFMT_RF_HEADER_2;
        MQRFH2 rfh2 = new MQRFH2();
        rfh2.setFieldValue("folder", "rfh2Field1", "rhf2Value1");

        MQHeaderList headerList = new MQHeaderList();
        headerList.add(rfh2);
        headerList.write(mqMessage);

        mqMessage.setStringProperty("folder.propertyField1", "propertyValue1");
        mqMessage.setStringProperty("folder.propertyField2", "propertyValue2");

        // set data
        String output = "Written to requestQ by MQ v7.1 client (" + new Date().toString() + ")";

        mqMessage.write(output.getBytes());
        return mqMessage;
    }
}
