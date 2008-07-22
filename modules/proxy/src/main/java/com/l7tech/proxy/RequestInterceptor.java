package com.l7tech.proxy;

import com.l7tech.common.http.HttpHeader;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.message.PolicyApplicationContext;

import java.util.List;

/**
 * Interface implemented by someone who wants to trace progress of each client proxy request.
 *
 * User: mike
 * Date: May 22, 2003
 * Time: 3:55:39 PM
 */
public interface RequestInterceptor {
    /**
     * Fired immediately after a message is received from a client, after it is parsed
     * but before it has been transformed or fed to our message processor.
     *
     * @param context  the policy application context. required
     */
    void onFrontEndRequest(PolicyApplicationContext context);

    /**
     * Fired when an undecorated reply is sent back to the client
     *
     * @param context  the policy application context. required
     */
    void onFrontEndReply(PolicyApplicationContext context);

    /**
     * Fired when a decorated request is sent to the SSG.
     * May be fired multiple times for a single request if it is retransmitted (due to policy updates etc).
     *
     * @param context  the policy application context. required
     * @param headersSent the headers to be sent to the back end.  This is included separately because it may
     *                    differ from the hedaers sent with the original request (Bug #4322)
     */
    void onBackEndRequest(PolicyApplicationContext context, List<HttpHeader> headersSent);

    /**
     * Fired when a reply is received from the SSG.
     * May be fired multiple times for a single request cycle if the request is retransmitted (due to policy updates etc).
     *
     * @param context  the policy application context. required
     */
    void onBackEndReply(PolicyApplicationContext context);

    /**
     * Fired when an error is encountered while reading the message from a client.
     *
     * @param t The error that occurred during the request.
     */
    void onMessageError(Throwable t);

    /**
     * Fired when an error is encountered while obtaining a reply from the server.
     *
     * @param t The error that occurred during the request.
     */
    void onReplyError(Throwable t);

    /**
     * Fired when a policy is updated.
     *
     * @param ssg        the Gateway account whose policy was updated.  Required.
     * @param binding    the policy attachment key that was updated.  Required.
     * @param policy     the policy that was saved there.  Required.
     */
    void onPolicyUpdated(Ssg ssg, PolicyAttachmentKey binding, Policy policy);

    /**
     * Fired when there is an error downloading a policy.
     *
     * @param ssg        the Gateway account whose policy was updated.  Required.
     * @param binding    the policy attachment key that was updated.  Required.
     * @param error      the error that occurred.  Required.
     */
    void onPolicyError(Ssg ssg, PolicyAttachmentKey binding, Throwable error);
}
