package com.l7tech.objectmodel.mqtt;

/**
 * This is the MQTT QOS2 Proxy. It is used to proxy QOS2 publish messages between the listen port and the publish
 * assertions when publishing a qos 2 message.
 */
public interface MQTTQOS2Proxy {
    /**
     * Proxy a pub rec message with the given message ID
     *
     * @param messageId The message id of the message to proxy
     * @throws MQTTException This is thrown it there was some error proxying the message
     */
    public void proxyPubRec(int messageId) throws MQTTException;

    /**
     * Proxy a pub rel message with the given message ID
     *
     * @param messageId The message id of the message to proxy
     * @throws MQTTException This is thrown it there was some error proxying the message
     */
    public void proxyPubRel(int messageId) throws MQTTException;

    /**
     * Proxy a pub comp message with the given message ID
     *
     * @param messageId The message id of the message to proxy
     * @throws MQTTException This is thrown it there was some error proxying the message
     */
    public void proxyPubComp(int messageId) throws MQTTException;
}
