package com.l7tech.proxy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgResponse;
import org.w3c.dom.Document;

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
    public void onReceiveMessage(final Document message) {
    }

    /**
     * Fired when a reply is read from the SSG, but before it is parsed.
     * @param reply
     */
    public void onReceiveReply(final SsgResponse reply) {
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

    /**
     * Fired when a policy is updated.
     * @param binding
     * @param policy
     */
    public void onPolicyUpdated(Ssg ssg, PolicyAttachmentKey binding, Assertion policy) {
    }
}
