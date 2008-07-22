package com.l7tech.gateway.common.transport.jms;

/**
 * Enumerated type for JMS Acknowledgement Modes.
 *
 * @author Steve Jones
 */
public enum JmsAcknowledgementType {

    /**
     * The message is acknowledged when taken from the Queue.
     * 
     * @see javax.jms.Session#AUTO_ACKNOWLEDGE
     */
    AUTOMATIC,

    /**
     * The message is acknowledged when policy execution completes.
     */
    ON_COMPLETION;
}
