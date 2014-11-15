package com.l7tech.server.transport.jms;

import com.l7tech.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.l7tech.server.transport.jms.JmsMessageTestUtility.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class JmsUtilTest {
    private TextMessageStub jmsRequest;

    @Before
    public void setup() throws Exception {
        jmsRequest = new TextMessageStub();
        JmsMessageTestUtility.setDefaultHeaders(jmsRequest);
    }

    @Test
    public void getHeaders() throws JMSException {
        final Map<String, String> headers = JmsUtil.getJmsHeaders(jmsRequest);

        assertEquals(10, headers.size());
        assertEquals(DESTINATION, headers.get(JmsUtil.JMS_DESTINATION));
        assertEquals(JmsUtil.DELIVERY_MODE_NON_PERSISTENT, headers.get(JmsUtil.JMS_DELIVERY_MODE));
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
        assertEquals(JmsUtil.DELIVERY_MODE_NON_PERSISTENT, headers.get(JmsUtil.JMS_DELIVERY_MODE));
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
        assertEquals(JmsUtil.DELIVERY_MODE_NON_PERSISTENT, headers.get(JmsUtil.JMS_DELIVERY_MODE));
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
        assertEquals(JmsUtil.DELIVERY_MODE_NON_PERSISTENT, headers.get(JmsUtil.JMS_DELIVERY_MODE));
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
        assertEquals(JmsUtil.DELIVERY_MODE_NON_PERSISTENT, headers.get(JmsUtil.JMS_DELIVERY_MODE));
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
        assertEquals(JmsUtil.DELIVERY_MODE_NON_PERSISTENT, headers.get(JmsUtil.JMS_DELIVERY_MODE));
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

    @Test
    public void setJmsHeadersTest() throws Exception {
        Message jmsMsg = new TextMessageStub();
        Map<String, Object> jmsHeaderMap = new HashMap<>();
        jmsHeaderMap.put(JmsUtil.JMS_CORRELATION_ID, CORRELATIONID);
        jmsHeaderMap.put(JmsUtil.JMS_DELIVERY_MODE, DELIVERYMODE);
        jmsHeaderMap.put(JmsUtil.JMS_DESTINATION, new DestinationStub(DESTINATION));
        jmsHeaderMap.put(JmsUtil.JMS_EXPIRATION, EXPIRATION);
        jmsHeaderMap.put(JmsUtil.JMS_MESSAGE_ID, MESSAGEID);
        jmsHeaderMap.put(JmsUtil.JMS_PRIORITY, PRIORITY);
        jmsHeaderMap.put(JmsUtil.JMS_REDELIVERED, REDELIVERED);
        jmsHeaderMap.put(JmsUtil.JMS_REPLY_TO, new DestinationStub(REPLYTO));
        jmsHeaderMap.put(JmsUtil.JMS_TIMESTAMP, TIMESTAMP);
        jmsHeaderMap.put(JmsUtil.JMS_TYPE, TYPE);
        setJmsHeaders(jmsMsg, jmsHeaderMap);
        assertEquals(CORRELATIONID,jmsMsg.getJMSCorrelationID());
        assertEquals(DELIVERYMODE, jmsMsg.getJMSDeliveryMode());
        assertNull( jmsMsg.getJMSDestination());
        assertEquals(EXPIRATION, jmsMsg.getJMSExpiration());
        assertNull(jmsMsg.getJMSMessageID());
        assertEquals(PRIORITY, jmsMsg.getJMSPriority());
        assertEquals(REPLYTO, jmsMsg.getJMSReplyTo().toString());
        assertEquals(0L, jmsMsg.getJMSTimestamp());
        assertEquals(TYPE, jmsMsg.getJMSType());
    }

    @Test
    public void setJmsExpirationHeader_stringValue() throws Exception {
        Message msg = new TextMessageStub();
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put(JmsUtil.JMS_EXPIRATION, "1234");
        headerMap.put(JmsUtil.JMS_DELIVERY_MODE, "1");
        headerMap.put(JmsUtil.JMS_PRIORITY, "0");
        headerMap.put(JmsUtil.JMS_TIMESTAMP, "12345667790");
        headerMap.put(JmsUtil.JMS_REDELIVERED, "true");
        setJmsHeaders(msg, headerMap);
        assertEquals(1234L, msg.getJMSExpiration());
        assertEquals(1, msg.getJMSDeliveryMode());
        assertEquals(0, msg.getJMSPriority());
        assertEquals(0L, msg.getJMSTimestamp());
        assertFalse(msg.getJMSRedelivered());
    }


    @Test
    public void setJmsExpirationHeader_wrongType() throws Exception {
        Message msg = new TextMessageStub();
        Map<String, Object> headerMap = new LinkedHashMap<>();
        headerMap.put(JmsUtil.JMS_EXPIRATION, new Object());
        try {
            setJmsHeaders(msg, headerMap);
            fail("Should throw ClassCastException");
        } catch (JMSException e) {
            assertTrue(e.getLinkedException() instanceof ClassCastException);
        }
    }

    @Test(expected = JMSException.class)
    public void setJmsReplyTo_wrongType() throws Exception {
        Message msg = new TextMessageStub();
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put(JmsUtil.JMS_REPLY_TO, "invalidDestination");
        setJmsHeaders(msg, headerMap);
    }

    @Test
    public void setJmsCorrelationID_wrongType() throws Exception {
        Message msg = new TextMessageStub();
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put(JmsUtil.JMS_CORRELATION_ID, new Object());
        try {
            setJmsHeaders(msg, headerMap);
        } catch (JMSException e) {
            assertTrue(e.getLinkedException() instanceof ClassCastException);
        }
    }

    @Test (expected = JMSException.class)
    public void setJmsCorrelationID_nullValue() throws Exception {
        Message msg = new TextMessageStub();
        Map<String, Object> headerMap = new HashMap<>();
        String sNull = null;
        headerMap.put(JmsUtil.JMS_CORRELATION_ID, sNull);
        setJmsHeaders(msg, headerMap);
    }

    private void setJmsHeaders(Message jmsMsg, Map<String, Object> jmsHeaderMap) throws JMSException {
        for(Map.Entry<String, Object> entry : jmsHeaderMap.entrySet()) {
            JmsUtil.setJmsHeader(jmsMsg, new Pair<>(entry.getKey(), entry.getValue()));
        }
    }
}
