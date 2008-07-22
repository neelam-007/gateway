/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.transport.jms;

/**
 * Enumerated type for JMS Reply Types
 *
 * @author alex
 */
public enum JmsReplyType {

    /**
     * The reply type is inferred from the inbound message.
     */
    AUTOMATIC( "Automatic" ),

    /**
     * No reply is expected, use this type for one way messages.
     */
    NO_REPLY( "No reply" ),

    /**
     * Send the reply to a specific queue.
     */
    REPLY_TO_OTHER( "Reply to other" );

    /**
     * Get the name of this reply type.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the ID for this reply type.
     *
     * @return The ID for the type
     */
    public int getNum() {
        return ordinal();
    }

    /**
     * Get a reply type by ID.
     *
     * @param num The id of the type.
     * @return The type or null
     */
    public static JmsReplyType getByNum(final int num) {
        return values()[num];
    }

    /**
     * Get the String representation for the type.
     *
     * @return The descriptive text
     */
    public String toString() {
        return "<JmsReplyType num=\"" + getNum() + "\" name=\"" + getName() + "\"/>";
    }

    //- PRIVATE

    private final String name;

    private JmsReplyType(final String name) {
        this.name = name;
    }
}
