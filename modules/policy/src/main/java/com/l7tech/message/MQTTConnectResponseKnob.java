package com.l7tech.message;

import com.l7tech.objectmodel.mqtt.MQTTQOS2Proxy;
import org.jetbrains.annotations.Nullable;

/**
 * The MQTTConnectResponseKnob is used to hold properties set from a connection response.
 */
public interface MQTTConnectResponseKnob extends MessageKnob {
    /**
     * This is the connection response code. It is -1 if the response code has not yet been set.
     *
     * @return The connection response code or -1 if it hasn't been set yet.
     */
    int getResponseCode();

    /**
     * Sets the connection response code
     *
     * @param responseCode The connection response code
     */
    void setResponseCode(int responseCode);

    /**
     * This is true if there is a session present.
     *
     * @return true is a session is present on this connection, false otherwise.
     */
    String isSessionPresent();

    /**
     * Sets the session present flag
     *
     * @param sessionPresent True if a session is present on this connection
     */
    void setSessionPresent(String sessionPresent);

    /**
     * Returns the QOS2 proxy that is associated with this connection response. The qos 2 proxy is used to proxy qos2
     * publish messages
     *
     * @return The qos2 proxy associated with this connection response.
     */
    @Nullable
    MQTTQOS2Proxy getMQTTQOS2Proxy();
}
