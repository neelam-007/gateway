package com.l7tech.server.transport.jms;

import com.l7tech.message.JmsKnob;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.jms.JMSException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public final class JmsMessageTestUtility {
    public static final String DESTINATION = "testQueue";
    public static final int DELIVERYMODE = 1;
    public static final long EXPIRATION = 1234L;
    public static final int PRIORITY = 0;
    public static final String MESSAGEID = "abcdefg";
    public static final long TIMESTAMP = 56789L;
    public static final String CORRELATIONID = "abc124";
    public static final String REPLYTO = "replyQueue";
    public static final String TYPE = "type";
    public static final boolean REDELIVERED = false;

    public static void setDefaultHeaders(final TextMessageStub message) throws JMSException {
        setHeadersOnMessage(message, DESTINATION, DELIVERYMODE, EXPIRATION, PRIORITY, MESSAGEID, TIMESTAMP, CORRELATIONID, REPLYTO, TYPE, REDELIVERED);
    }

    public static void setHeadersOnMessage(final TextMessageStub message, final String destination, final int deliveryMode, final long expiration,
                                       final int priority, final String messageId, final long timestamp,
                                       final String correlationId, final String replyTo, final String type,
                                       final boolean redelivered) throws JMSException {
        message.setJMSDestination(destination);
        message.setJMSDeliveryMode(deliveryMode);
        message.setJMSExpiration(expiration);
        message.setJMSPriority(priority);
        message.setJMSMessageID(messageId);
        message.setJMSTimestamp(timestamp);
        message.setJMSCorrelationID(correlationId);
        message.setJMSReplyTo(replyTo);
        message.setJMSType(type);
        message.setJMSRedelivered(redelivered);
    }

    public static void assertDefaultHeadersPresent(final JmsKnob jmsKnob) {
        final String[] headerNames = jmsKnob.getHeaderNames();
        assertEquals(10, headerNames.length);
        final List<String> headerNamesAsList = Arrays.asList(headerNames);
        assertTrue(headerNamesAsList.contains(JmsUtil.JMS_DESTINATION));
        assertTrue(headerNamesAsList.contains(JmsUtil.JMS_DELIVERY_MODE));
        assertTrue(headerNamesAsList.contains(JmsUtil.JMS_EXPIRATION));
        assertTrue(headerNamesAsList.contains(JmsUtil.JMS_PRIORITY));
        assertTrue(headerNamesAsList.contains(JmsUtil.JMS_MESSAGE_ID));
        assertTrue(headerNamesAsList.contains(JmsUtil.JMS_TIMESTAMP));
        assertTrue(headerNamesAsList.contains(JmsUtil.JMS_CORRELATION_ID));
        assertTrue(headerNamesAsList.contains(JmsUtil.JMS_REPLY_TO));
        assertTrue(headerNamesAsList.contains(JmsUtil.JMS_TYPE));
        assertTrue(headerNamesAsList.contains(JmsUtil.JMS_REDELIVERED));
        assertEquals(DESTINATION, jmsKnob.getHeaderValues(JmsUtil.JMS_DESTINATION)[0]);
        assertEquals(String.valueOf(DELIVERYMODE), jmsKnob.getHeaderValues(JmsUtil.JMS_DELIVERY_MODE)[0]);
        assertEquals(String.valueOf(EXPIRATION), jmsKnob.getHeaderValues(JmsUtil.JMS_EXPIRATION)[0]);
        assertEquals(String.valueOf(PRIORITY), jmsKnob.getHeaderValues(JmsUtil.JMS_PRIORITY)[0]);
        assertEquals(MESSAGEID, jmsKnob.getHeaderValues(JmsUtil.JMS_MESSAGE_ID)[0]);
        assertEquals(String.valueOf(TIMESTAMP), jmsKnob.getHeaderValues(JmsUtil.JMS_TIMESTAMP)[0]);
        assertEquals(CORRELATIONID, jmsKnob.getHeaderValues(JmsUtil.JMS_CORRELATION_ID)[0]);
        assertEquals(REPLYTO, jmsKnob.getHeaderValues(JmsUtil.JMS_REPLY_TO)[0]);
        assertEquals(TYPE, jmsKnob.getHeaderValues(JmsUtil.JMS_TYPE)[0]);
        assertEquals(String.valueOf(REDELIVERED), jmsKnob.getHeaderValues(JmsUtil.JMS_REDELIVERED)[0]);
    }
}
