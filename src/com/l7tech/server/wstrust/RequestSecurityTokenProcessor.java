package com.l7tech.server.wstrust;

import org.w3c.dom.Document;

import java.util.logging.Logger;

/**
 * @author emil
 * @version 12-Aug-2004
 */
public class RequestSecurityTokenProcessor implements Handler {
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Message chain handler for message invocation.
     *
     * @param i The invocation.
     */
    public void invoke(MessageInvocation i) throws Throwable {
        Document doc = i.getRequestDocument();

        i.invokeNext();
    }
}
