package com.l7tech.proxy;

import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgResponse;

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
     * @param request
     */
    void onReceiveMessage(PendingRequest request);

    /**
     * Fired when a reply is read from the SSG, after it has come back through our message processor.
     *
     * @param reply
     */
    void onReceiveReply(SsgResponse reply);

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
     * @param binding
     * @param policy
     */
    void onPolicyUpdated(Ssg ssg, PolicyAttachmentKey binding, Policy policy);
}
