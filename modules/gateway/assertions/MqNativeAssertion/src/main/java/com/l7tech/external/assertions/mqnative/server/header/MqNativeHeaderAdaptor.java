package com.l7tech.external.assertions.mqnative.server.header;

import com.ibm.mq.MQException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

/**
 * Adaptor to handle MQ Native Header Type.
 */
public interface MqNativeHeaderAdaptor {

    /**
     * Apply the name-value pair map to the message header.
     * @param messageHeaderValueMap The header attributes in name-value pair
     * @throws IOException
     * @throws MQException
     */
    void applyHeaderValuesToMessage(@NotNull final Map<String, Object> messageHeaderValueMap) throws IOException, MQException;

    /**
     * Retrieve the header format, the header format will apply to the MQMessage descriptor format attribute.
     * @return The Message Format.
     */
    String getMessageFormat();

    /**
     * Retrieve the Header Object, the returned header object will add the MQHeaderList
     * @return The header object
     */
    Object getHeader();

    /**
     * Set the Header Object.
     * @param header The Header Object.
     */
    void setHeader(Object header);

    /**
     * Retrieve the header attributes in name-value pair map
     * @return All headers name and values
     * @throws IOException
     */
    Map<String, Object> parseHeaderValues() throws IOException;

}
