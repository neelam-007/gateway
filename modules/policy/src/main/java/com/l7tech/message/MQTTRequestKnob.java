package com.l7tech.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The MQTT knob holds MQTT message options
 */
public interface MQTTRequestKnob extends TcpKnob {

    public enum MessageType {
        CONNECT, PUBLISH, SUBSCRIBE, UNSUBSCRIBE, DISCONNECT
    }

    /**
     * Returns the message type. This is one of CONNECT, PUBLISH, SUBSCRIBE, UNSUBSCRIBE, DISCONNECT
     *
     * @return The MQTT message type.
     */
    @NotNull
    public MessageType getMessageType();


    /**
     * Returns the client identifier
     *
     * @return The MQTT client identifier
     */
    @Nullable
    public String getClientIdentifier();

    /**
     * The user name that was specified in the connect message
     *
     * @return The user name that was specified in the connect message
     */
    @Nullable
    public String getUserName();

    /**
     * The user password that was specified in the connect message
     *
     * @return The user password that was specified in the connect message
     */
    @Nullable
    public String getUserPassword();

    /**
     * Returns any connection parameters set on this knob
     *
     * @return Any mqtt connection parameters
     */
    @Nullable
    public MQTTConnectParameters getMQTTConnectParameters();

    /**
     * Returns any publish parameters set on this MQTT knob
     *
     * @return Any MQTT Publish parameters
     */
    @Nullable
    public MQTTPublishParameters getMQTTPublishParameters();

    /**
     * Returns any mqtt subscribe parameters set on this knob
     *
     * @return Any mqtt subscribe parameters
     */
    @Nullable
    public MQTTSubscribeParameters getMQTTSubscribeParameters();

    /**
     * MQTT Publish parameters
     */
    public interface MQTTPublishParameters {
        /**
         * The topic that the message is being published to
         *
         * @return The topic that the message is being published to
         */
        public String getTopic();

        /**
         * The qos to publish the message with
         *
         * @return The qos to publish the message with
         */
        public int getQOS();

        /**
         * If the message should be retained by the broker
         *
         * @return True if the message should be retained by the broker
         */
        public boolean isRetain();
    }

    /**
     * Subscription parameters
     */
    public interface MQTTSubscribeParameters {
        /**
         * Gets the subscriptions requested. This is a json string in the following form: [{"<topic>":<qos>},
         * {"<topic>":<qos>}, ...]
         *
         * @return The subscriptions requested represented as a json array
         */
        public String getSubscriptions();
    }

    /**
     * The mqtt connection parameters
     */
    public interface MQTTConnectParameters {

        /**
         * Is a create session requested
         *
         * @return True if a clean session is requested
         */
        public boolean isCleanSession();

        /**
         * This is the keep alive interval
         *
         * @return The keep alive interval
         */
        public int getKeepAlive();

        /**
         * True is a will message is present
         *
         * @return true if a will message is present
         */
        public boolean isWillPresent();

        /**
         * The will message topic
         *
         * @return The will message topic
         */
        public String getWillTopic();

        /**
         * The will message body
         *
         * @return The will message body
         */
        public String getWillMessage();

        /**
         * The will message qos
         *
         * @return The will message qos
         */
        public int getWillQOS();

        /**
         * The will message retain flag
         *
         * @return The will message retain flag
         */
        public boolean isWillRetain();
    }
}
