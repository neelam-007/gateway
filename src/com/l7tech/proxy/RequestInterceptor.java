package com.l7tech.proxy;

import org.apache.axis.message.SOAPEnvelope;

/**
 * Interface implemented by someone who wants to trace progress of each client proxy request.
 * User: mike
 * Date: May 22, 2003
 * Time: 3:55:39 PM
 * To change this template use Options | File Templates.
 */
public interface RequestInterceptor {
    /**
     * Fired when a message is received from a client, after it is parsed.
     * @param message
     */
    public void onReceiveMessage(SOAPEnvelope message);

    /**
     * Fired when a reply is read from the SSG, after it is parsed.
     * @param reply
     */
    public void onReceiveReply(SOAPEnvelope reply);

    /**
     * Fired when an error is encountered while reading the message from a client.
     * @param t The error that occurred during the request.
     */
    public void onMessageError(Throwable t);

    /**
     * Fired when an error is encountered while obtaining a reply from the server.
     * @param t The error that occurred during the request.
     */
    public void onReplyError(Throwable t);
}
