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
    public void onReceiveMessage(SOAPEnvelope message) {
    }

    /**
     * Fired when a reply is read from the SSG, but before it is parsed.
     * @param reply
     */
    public void onReceiveReply(SOAPEnvelope reply) {
    }
}
