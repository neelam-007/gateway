package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQHeaderList;
import com.l7tech.external.assertions.mqnative.MqNativeMessageHeaderType;
import com.l7tech.external.assertions.mqnative.server.header.MqNativeHeaderHandler;

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

/**
 * As a proxy to access MQMessage attributes
 */
public class MqMessageProxy {

    private MQMessage message;
    private MQHeaderList headers;
    private List<Map<String, Object>> headersProperties = new ArrayList<Map<String, Object>>();
    private MqNativeMessageDescriptor messageDescriptor;
    private Map<String, Object> messageProperties = new LinkedHashMap<String, Object>();
    private MqNativeMessageHeaderType primaryType;

    public MqMessageProxy(MQMessage message) throws IOException, MQDataException, MQException {
        this.message = message;
        message.seek(0);
        headers = new MQHeaderList(message);
        messageDescriptor = new MqNativeMessageDescriptor(message);
        messageDescriptor.copyFrom(message);
        populateMessageProperties();
        populateHeader();
    }

    /**
     * Retrieve MQMessage Descriptor
     *
     * @return
     * @throws IOException
     * @throws MQDataException
     */
    public MqNativeMessageDescriptor getMessageDescriptor() throws IOException, MQDataException {
        return messageDescriptor;
    }

    /**
     * Retrieve the value of the message Descriptor
     *
     * @param name The name of the message Descriptor
     * @return The value of the message Descriptor or null if no such descriptor.
     */
    public Object getMessageDescriptor(String name) {
        try {
            Field field = MQMessage.class.getField(name);
            return field.get(message);
        } catch (NoSuchFieldException e) {
            if (name.equals("version")) {
                return message.getVersion();
            } else {
                return null;
            }
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * Retrieve the headers of the MQMessage
     *
     * @return The MQMessage Header
     */
    public MQHeaderList getHeaders() {
        return headers;
    }

    /**
     * Retrieve the primary header of the MQMessage
     *
     * @return The primary header of the MQMessage, null if there is not header for the MQMessage
     */
    public Object getPrimaryHeader() {
        try {
            return headers.get(0);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Retrieve the primary header type.
     *
     * @return The primary header type
     */
    public MqNativeMessageHeaderType getPrimaryType() {
        return primaryType;
    }

    /**
     * Retrieve the primary header value
     *
     * @param name The primary header attribute name
     * @return The value of the primary header attribute, null if no header or no such header attribute
     */
    public Object getPrimaryHeaderValue(String name) {
        return getHeaderValue(0, name);
    }

    public Object getHeaderValue(int index, String name) {
        try {
            Map<String, Object> properties = headersProperties.get(index);
            return properties.get(name);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public Map<String, Object> getPrimaryHeaderProperties() {
        return getHeaderProperties(0);
    }

    public Map<String, Object> getHeaderProperties(int index) {
        try {
            return headersProperties.get(index);
        } catch (IndexOutOfBoundsException e) {
            return new HashMap<String, Object>();
        }
    }

    public Map<String, Object> getMessageProperties() {
        return messageProperties;
    }

    public Object getMessageProperty(String name) {
        return messageProperties.get(name);
    }

    private void populateHeader() {

        try {
            if (headers != null) {
                for (int i = 0; i < headers.size(); i++) {
                    MqNativeHeaderHandler h = new MqNativeHeaderHandler(headers.get(i));
                    if (i == 0) primaryType = h.getType();
                    headersProperties.add(h.parsePrimaryHeaderValues());
                }
            }
            //Should not throw exception is headers cannot be parse, user may just pass through it.
        } catch (IOException e) {
        } catch (MQDataException e) {
        } catch (MqNativeConfigException e) {
        }
    }

    private void populateMessageProperties() throws MQException, EOFException {
        message.seek(0);
        for (Enumeration e = message.getPropertyNames("%"); e.hasMoreElements(); ) {
            String propertyName = (String) e.nextElement();
            messageProperties.put(propertyName, message.getObjectProperty(propertyName));
        }
    }
}
