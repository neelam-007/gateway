package com.l7tech.proxy;

import org.apache.axis.message.SOAPEnvelope;

/**
 * A RequestInterceptor that ignores all events.
 * This is a singleton class.  Use NullRequestInterceptor.INSTANCE
 * User: mike
 * Date: May 22, 2003
 * Time: 3:58:20 PM
 * To change this template use Options | File Templates.
 */
public class NullRequestInterceptor implements RequestInterceptor {
    public static NullRequestInterceptor INSTANCE = new NullRequestInterceptor();

    private NullRequestInterceptor() {
    }

    /**
     * Fired when a message is received from a client, but before it is parsed.
     * @param message
     */
    public void onReceiveMessage(final SOAPEnvelope message) {
    }

    /**
     * Fired when a reply is read from the SSG, but before it is parsed.
     * @param reply
     */
    public void onReceiveReply(final SOAPEnvelope reply) {
    }

    /**
     * Fired when an error is encountered while reading the message from a client.
     * @param t The error that occurred during the request.
     */
    public void onMessageError(final Throwable t) {
    }

    /**
     * Fired when an error is encountered while obtaining a reply from the server.
     * @param t The error that occurred during the request.
     */
    public void onReplyError(final Throwable t) {
    }
}
