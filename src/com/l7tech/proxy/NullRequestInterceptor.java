package com.l7tech.proxy;

import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.message.PolicyApplicationContext;

/**
 * A RequestInterceptor that ignores all events.
 * This is a singleton class.  Use NullRequestInterceptor.INSTANCE
 * User: mike
 * Date: May 22, 2003
 * Time: 3:58:20 PM
 */
public class NullRequestInterceptor implements RequestInterceptor {
    public static final NullRequestInterceptor INSTANCE = new NullRequestInterceptor();

    private NullRequestInterceptor() {
    }

    /**
     * Fired when a message is received from a client, after it is parsed.
     * @param context
     */
    public void onReceiveMessage(PolicyApplicationContext context) {
    }

    /**
     * Fired when a reply is read from the SSG, after it is parsed.
     * @param context
     */
    public void onReceiveReply(PolicyApplicationContext context) {
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
    public void onPolicyUpdated(Ssg ssg, PolicyAttachmentKey binding, Policy policy) {
    }
}
