package com.l7tech.server.event.system;

import java.util.logging.Level;

import com.l7tech.common.Component;

/**
 * Event class for use by JMS receivers.
 *
 * @author $Author$
 * @version $Revision$
 */
public class JMSEvent extends TransportEvent {

    //- PUBLIC

    public JMSEvent(
            Object source,
            Level level,
            String ip,
            String message) {
        super(source, Component.GW_JMSRECV, ip, level, message, NAME);
    }

    //- PRIVATE

    private static final String NAME = "Connect";
}
