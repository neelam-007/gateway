package com.l7tech.server.transport.jms;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jms.JMSException;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class JmsUtilTest {
    private static final String DESTINATION = "testQueue";
    private static final int DELIVERYMODE = 1;
    private static final long EXPIRATION = 1234L;
    private static final int PRIORITY = 0;
    private static final String MESSAGEID = "abcdefg";
    private static final long TIMESTAMP = 56789L;
    private static final String CORRELATIONID = "abc124";
    private static final String REPLYTO = "replyQueue";
    private static final String TYPE = "type";
    private static final boolean REDELIVERED = false;
    private TextMessageStub jmsRequest;

    @Before
    public void setup() throws Exception{
        jmsRequest = new TextMessageStub();
        JmsMessageTestUtility.setDefaultHeaders(jmsRequest);
    }

    @Test
    public void getHeaders() throws JMSException {
        final Map<String, String> headers = JmsUtil.getJmsHeaders(jmsRequest);

        assertEquals(10, headers.size());
        assertEquals(DESTINATION, headers.get(JmsUtil.JMS_DESTINATION));
        assertEquals(String.valueOf(DELIVERYMODE), headers.get(JmsUtil.JMS_DELIVERY_MODE));
        assertEquals(String.valueOf(EXPIRATION), headers.get(JmsUtil.JMS_EXPIRATION));
        assertEquals(String.valueOf(PRIORITY), headers.get(JmsUtil.JMS_PRIORITY));
        assertEquals(MESSAGEID, headers.get(JmsUtil.JMS_MESSAGE_ID));
        assertEquals(String.valueOf(TIMESTAMP), headers.get(JmsUtil.JMS_TIMESTAMP));
        assertEquals(CORRELATIONID, headers.get(JmsUtil.JMS_CORRELATION_ID));
        assertEquals(REPLYTO, headers.get(JmsUtil.JMS_REPLY_TO));
        assertEquals(TYPE, headers.get(JmsUtil.JMS_TYPE));
        assertEquals(String.valueOf(REDELIVERED), headers.get(JmsUtil.JMS_REDELIVERED));
    }

    @Test
    public void getHeadersNullDestination() throws JMSException {
        JmsMessageTestUtility.setHeadersOnMessage(jmsRequest, null, DELIVERYMODE, EXPIRATION, PRIORITY, MESSAGEID, TIMESTAMP, CORRELATIONID, REPLYTO, TYPE, REDELIVERED);

        final Map<String, String> headers = JmsUtil.getJmsHeaders(jmsRequest);

        assertEquals(9, headers.size());
        assertFalse(headers.containsKey(JmsUtil.JMS_DESTINATION));
        assertEquals(String.valueOf(DELIVERYMODE), headers.get(JmsUtil.JMS_DELIVERY_MODE));
        assertEquals(String.valueOf(EXPIRATION), headers.get(JmsUtil.JMS_EXPIRATION));
        assertEquals(String.valueOf(PRIORITY), headers.get(JmsUtil.JMS_PRIORITY));
        assertEquals(MESSAGEID, headers.get(JmsUtil.JMS_MESSAGE_ID));
        assertEquals(String.valueOf(TIMESTAMP), headers.get(JmsUtil.JMS_TIMESTAMP));
        assertEquals(CORRELATIONID, headers.get(JmsUtil.JMS_CORRELATION_ID));
        assertEquals(REPLYTO, headers.get(JmsUtil.JMS_REPLY_TO));
        assertEquals(TYPE, headers.get(JmsUtil.JMS_TYPE));
        assertEquals(String.valueOf(REDELIVERED), headers.get(JmsUtil.JMS_REDELIVERED));
    }

    @Test
    public void getHeadersNullMessageId() throws JMSException {
        JmsMessageTestUtility.setHeadersOnMessage(jmsRequest, DESTINATION, DELIVERYMODE, EXPIRATION, PRIORITY, null, TIMESTAMP, CORRELATIONID, REPLYTO, TYPE, REDELIVERED);

        final Map<String, String> headers = JmsUtil.getJmsHeaders(jmsRequest);

        assertEquals(9, headers.size());
        assertFalse(headers.containsKey(JmsUtil.JMS_MESSAGE_ID));
        assertEquals(DESTINATION, headers.get(JmsUtil.JMS_DESTINATION));
        assertEquals(String.valueOf(DELIVERYMODE), headers.get(JmsUtil.JMS_DELIVERY_MODE));
        assertEquals(String.valueOf(EXPIRATION), headers.get(JmsUtil.JMS_EXPIRATION));
        assertEquals(String.valueOf(PRIORITY), headers.get(JmsUtil.JMS_PRIORITY));
        assertEquals(String.valueOf(TIMESTAMP), headers.get(JmsUtil.JMS_TIMESTAMP));
        assertEquals(CORRELATIONID, headers.get(JmsUtil.JMS_CORRELATION_ID));
        assertEquals(REPLYTO, headers.get(JmsUtil.JMS_REPLY_TO));
        assertEquals(TYPE, headers.get(JmsUtil.JMS_TYPE));
        assertEquals(String.valueOf(REDELIVERED), headers.get(JmsUtil.JMS_REDELIVERED));
    }

    @Test
    public void getHeadersNullCorrelationId() throws JMSException {
        JmsMessageTestUtility.setHeadersOnMessage(jmsRequest, DESTINATION, DELIVERYMODE, EXPIRATION, PRIORITY, MESSAGEID, TIMESTAMP, null, REPLYTO, TYPE, REDELIVERED);

        final Map<String, String> headers = JmsUtil.getJmsHeaders(jmsRequest);

        assertEquals(9, headers.size());
        assertFalse(headers.containsKey(JmsUtil.JMS_CORRELATION_ID));
        assertEquals(DESTINATION, headers.get(JmsUtil.JMS_DESTINATION));
        assertEquals(String.valueOf(DELIVERYMODE), headers.get(JmsUtil.JMS_DELIVERY_MODE));
        assertEquals(String.valueOf(EXPIRATION), headers.get(JmsUtil.JMS_EXPIRATION));
        assertEquals(String.valueOf(PRIORITY), headers.get(JmsUtil.JMS_PRIORITY));
        assertEquals(MESSAGEID, headers.get(JmsUtil.JMS_MESSAGE_ID));
        assertEquals(String.valueOf(TIMESTAMP), headers.get(JmsUtil.JMS_TIMESTAMP));
        assertEquals(REPLYTO, headers.get(JmsUtil.JMS_REPLY_TO));
        assertEquals(TYPE, headers.get(JmsUtil.JMS_TYPE));
        assertEquals(String.valueOf(REDELIVERED), headers.get(JmsUtil.JMS_REDELIVERED));
    }

    @Test
    public void getHeadersNullReplyTo() throws JMSException {
        JmsMessageTestUtility.setHeadersOnMessage(jmsRequest, DESTINATION, DELIVERYMODE, EXPIRATION, PRIORITY, MESSAGEID, TIMESTAMP, CORRELATIONID, null, TYPE, REDELIVERED);

        final Map<String, String> headers = JmsUtil.getJmsHeaders(jmsRequest);

        assertEquals(9, headers.size());
        assertFalse(headers.containsKey(JmsUtil.JMS_REPLY_TO));
        assertEquals(DESTINATION, headers.get(JmsUtil.JMS_DESTINATION));
        assertEquals(String.valueOf(DELIVERYMODE), headers.get(JmsUtil.JMS_DELIVERY_MODE));
        assertEquals(String.valueOf(EXPIRATION), headers.get(JmsUtil.JMS_EXPIRATION));
        assertEquals(String.valueOf(PRIORITY), headers.get(JmsUtil.JMS_PRIORITY));
        assertEquals(MESSAGEID, headers.get(JmsUtil.JMS_MESSAGE_ID));
        assertEquals(String.valueOf(TIMESTAMP), headers.get(JmsUtil.JMS_TIMESTAMP));
        assertEquals(CORRELATIONID, headers.get(JmsUtil.JMS_CORRELATION_ID));
        assertEquals(TYPE, headers.get(JmsUtil.JMS_TYPE));
        assertEquals(String.valueOf(REDELIVERED), headers.get(JmsUtil.JMS_REDELIVERED));
    }

    @Test
    public void getHeadersNullType() throws JMSException {
        JmsMessageTestUtility.setHeadersOnMessage(jmsRequest, DESTINATION, DELIVERYMODE, EXPIRATION, PRIORITY, MESSAGEID, TIMESTAMP, CORRELATIONID, REPLYTO, null, REDELIVERED);

        final Map<String, String> headers = JmsUtil.getJmsHeaders(jmsRequest);

        assertEquals(9, headers.size());
        assertFalse(headers.containsKey(JmsUtil.JMS_TYPE));
        assertEquals(DESTINATION, headers.get(JmsUtil.JMS_DESTINATION));
        assertEquals(String.valueOf(DELIVERYMODE), headers.get(JmsUtil.JMS_DELIVERY_MODE));
        assertEquals(String.valueOf(EXPIRATION), headers.get(JmsUtil.JMS_EXPIRATION));
        assertEquals(String.valueOf(PRIORITY), headers.get(JmsUtil.JMS_PRIORITY));
        assertEquals(MESSAGEID, headers.get(JmsUtil.JMS_MESSAGE_ID));
        assertEquals(String.valueOf(TIMESTAMP), headers.get(JmsUtil.JMS_TIMESTAMP));
        assertEquals(CORRELATIONID, headers.get(JmsUtil.JMS_CORRELATION_ID));
        assertEquals(REPLYTO, headers.get(JmsUtil.JMS_REPLY_TO));
        assertEquals(String.valueOf(REDELIVERED), headers.get(JmsUtil.JMS_REDELIVERED));
    }

    @Test(expected = JMSException.class)
    public void getHeadersThrowsJmsException() throws JMSException {
        jmsRequest.setThrowExceptionForHeaders(true);

        JmsUtil.getJmsHeaders(jmsRequest);
    }
}
