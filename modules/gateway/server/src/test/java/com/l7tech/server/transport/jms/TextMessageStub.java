package com.l7tech.server.transport.jms;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.StringTokenizer;

public class TextMessageStub implements TextMessage {
    private String text;

    // set to true if you want to throw a JMSException for any header getter
    private boolean throwExceptionForHeaders;

    //headers
    private String destination;
    private int deliveryMode;
    private long expiration;
    private int priority;
    private String messageId;
    private long timestamp;
    private String correlationId;
    private String replyTo;
    private String type;
    private boolean redelivered;

    private HashMap<String, Object> properties = new HashMap<>();

    public void setThrowExceptionForHeaders(final boolean throwExceptionForHeaders) {
        this.throwExceptionForHeaders = throwExceptionForHeaders;
    }

    private void checkThrow() throws JMSException {
        if (throwExceptionForHeaders) {
            throw new JMSException("stubbing exception");
        }
    }

    @Override
    public void setText(String s) throws JMSException {
        this.text = s;
    }

    @Override
    public String getText() throws JMSException {
        return text;
    }

    @Override
    public String getJMSMessageID() throws JMSException {
        checkThrow();
        return messageId;
    }

    @Override
    public void setJMSMessageID(String s) throws JMSException {
        messageId = s;
    }

    @Override
    public long getJMSTimestamp() throws JMSException {
        checkThrow();
        return timestamp;
    }

    @Override
    public void setJMSTimestamp(long l) throws JMSException {
        timestamp = l;
    }

    @Override
    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        checkThrow();
        return correlationId.getBytes();
    }

    @Override
    public void setJMSCorrelationIDAsBytes(byte[] bytes) throws JMSException {
        correlationId = new String(bytes);
    }

    @Override
    public void setJMSCorrelationID(String s) throws JMSException {
        correlationId = s;
    }

    @Override
    public String getJMSCorrelationID() throws JMSException {
        checkThrow();
        return correlationId;
    }

    @Override
    public Destination getJMSReplyTo() throws JMSException {
        checkThrow();
        return replyTo == null ? null : new DestinationStub(replyTo);
    }

    @Override
    public void setJMSReplyTo(Destination destination) throws JMSException {
        this.replyTo = destination == null ? null : destination.toString();
    }

    public void setJMSReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    @Override
    public Destination getJMSDestination() throws JMSException {
        checkThrow();
        return destination == null ? null : new DestinationStub(destination);
    }

    @Override
    public void setJMSDestination(Destination destination) throws JMSException {
        this.destination = destination == null ? null : destination.toString();
    }

    public void setJMSDestination(String destination) {
        this.destination = destination;
    }

    @Override
    public int getJMSDeliveryMode() throws JMSException {
        checkThrow();
        return deliveryMode;
    }

    @Override
    public void setJMSDeliveryMode(int i) throws JMSException {
        deliveryMode = i;
    }

    @Override
    public boolean getJMSRedelivered() throws JMSException {
        checkThrow();
        return redelivered;
    }

    @Override
    public void setJMSRedelivered(boolean b) throws JMSException {
        redelivered = b;
    }

    @Override
    public String getJMSType() throws JMSException {
        checkThrow();
        return type;
    }

    @Override
    public void setJMSType(String s) throws JMSException {
        type = s;
    }

    @Override
    public long getJMSExpiration() throws JMSException {
        checkThrow();
        return expiration;
    }

    @Override
    public void setJMSExpiration(long l) throws JMSException {
        expiration = l;
    }

    @Override
    public int getJMSPriority() throws JMSException {
        checkThrow();
        return priority;
    }

    @Override
    public void setJMSPriority(int i) throws JMSException {
        priority = i;
    }

    @Override
    public void clearProperties() throws JMSException {
    }

    @Override
    public boolean propertyExists(String s) throws JMSException {
        return false;
    }

    @Override
    public boolean getBooleanProperty(String s) throws JMSException {
        return false;
    }

    @Override
    public byte getByteProperty(String s) throws JMSException {
        return 0;
    }

    @Override
    public short getShortProperty(String s) throws JMSException {
        return 0;
    }

    @Override
    public int getIntProperty(String s) throws JMSException {
        return 0;
    }

    @Override
    public long getLongProperty(String s) throws JMSException {
        return 0;
    }

    @Override
    public float getFloatProperty(String s) throws JMSException {
        return 0;
    }

    @Override
    public double getDoubleProperty(String s) throws JMSException {
        return 0;
    }

    @Override
    public String getStringProperty(String s) throws JMSException {
        return null;
    }

    @Override
    public Object getObjectProperty(String s) throws JMSException {
        return properties.get(s);
    }

    @Override
    public Enumeration<String> getPropertyNames() throws JMSException {
        return Collections.enumeration(properties.keySet());
    }

    @Override
    public void setBooleanProperty(String s, boolean b) throws JMSException {
    }

    @Override
    public void setByteProperty(String s, byte b) throws JMSException {
    }

    @Override
    public void setShortProperty(String s, short i) throws JMSException {
    }

    @Override
    public void setIntProperty(String s, int i) throws JMSException {
    }

    @Override
    public void setLongProperty(String s, long l) throws JMSException {
    }

    @Override
    public void setFloatProperty(String s, float v) throws JMSException {
    }

    @Override
    public void setDoubleProperty(String s, double v) throws JMSException {
    }

    @Override
    public void setStringProperty(String s, String s1) throws JMSException {
    }

    @Override
    public void setObjectProperty(String s, Object o) throws JMSException {
        properties.put(s, o);
    }

    @Override
    public void acknowledge() throws JMSException {
    }

    @Override
    public void clearBody() throws JMSException {
    }
}
