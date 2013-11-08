package com.l7tech.server.transport.jms2;

import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;

import javax.jms.Message;

/**
 * Interface definition for Jms request handlers that is responsible for
 * processing inbound jms message.
 *
 * @author: vchan
 */
public interface JmsRequestHandler {

    /**
     * Handle an incoming JMS SOAP request.  Also takes care of sending the reply if appropriate.
     *
     * @param endpointCfg The Jms endpoint configuration that this handler operates on
     * @param bag The JMS context
     * @param transacted True is the session is transactional (so commit when done)
     * @param jmsRequest The request message to process
     * @throws com.l7tech.server.transport.jms.JmsRuntimeException if an error occurs
     */
    void onMessage( JmsEndpointConfig endpointCfg,
                    JmsBag bag,
                    boolean transacted,
                    Message jmsRequest ) throws JmsRuntimeException;
}
