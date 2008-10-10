package com.l7tech.server.transport.email;

import javax.mail.internet.MimeMessage;

/**
 * Interface for objects that can handle incoming messages from an email listener.
 */
public interface EmailHandler {
    /**
     * Handle an incoming email listener SOAP request.  Also takes care of sending the reply if appropriate.
     *
     * @param emailListenerCfg The email listener configuration that this handler operates on
     * @param message The request message to process
     * @throws EmailListenerRuntimeException if an error occurs
     */
    public void onMessage( final EmailListenerConfig emailListenerCfg,
                           final MimeMessage message )
            throws EmailListenerRuntimeException;
}
