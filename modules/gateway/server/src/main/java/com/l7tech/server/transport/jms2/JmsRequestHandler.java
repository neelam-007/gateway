package com.l7tech.server.transport.jms2;

import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;

import javax.jms.Message;
import javax.jms.QueueSender;

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
     * @param failureQueue The queue for failed messages (may be null)
     * @param jmsRequest The request message to process
     * @throws com.l7tech.server.transport.jms.JmsRuntimeException if an error occurs
     */
    public void onMessage( final JmsEndpointConfig endpointCfg,
                           final JmsBag bag,
                           final boolean transacted,
                           final QueueSender failureQueue,
                           final Message jmsRequest ) throws JmsRuntimeException;
}
