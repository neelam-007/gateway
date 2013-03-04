package com.l7tech.server.transport.jms;

/**
 * Define the JMS Pre-Defined Properties
 * Refer to Java(TM) Message Service Specification Final Release 1.1
 */
public enum JmsDefinedProperties {

    JMSXUserID("JMSXUserID", String.class),
    JMSXAppID("JMSXAppID", String.class),
    JMSXDeliveryCount("JMSXDeliveryCount", Integer.class),
    JMSXGroupID("JMSXGroupID", String.class),
    JMSXGroupSeq("JMSXGroupSeq", Integer.class),
    JMSXProducerTXID("JMSXProducerTXID", String.class),
    JMSXConsumerTXID("JMSXConsumerTXID", String.class),
    JMSXRcvTimestamp("JMSXRcvTimestamp", Long.class),
    JMSXState("JMSXState", Integer.class);

    private final String name;
    private final Class type;

    public String getName() {
        return name;
    }

    public Class getType() {
        return type;
    }

    private JmsDefinedProperties(String name, Class type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Retrieve the enum constant by its {@link #getName()}
     *
     * @param name Jms property name
     * @return the enum constant or throw IllegalArgumentException if provided name is not a Jms predefined property.
     */
    public static JmsDefinedProperties fromName(String name) {
        for (JmsDefinedProperties e : JmsDefinedProperties.values()) {
            if (e.getName().equals(name)) {
                return e;
            }
        }
        throw new IllegalArgumentException(name);
    }

    /**
     * Validate the Jms predefined message property and data type.
     *
     * @param name The Jms predefined message property
     * @param value The value of predefined message
     * @return True if the message property is valid, False if the message property is not valid.
     */
    public static boolean isValid(String name, String value) {
        try {
            JmsDefinedProperties p = fromName(name);
            if (p.getType().isAssignableFrom(Integer.class)) {
                Integer.parseInt(value);
            } else if (p.getType().isAssignableFrom(Long.class)) {
                Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }
}
