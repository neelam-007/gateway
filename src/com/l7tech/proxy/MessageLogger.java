/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.message.PolicyApplicationContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RequestInterceptor that logs all messages in and out.
 * User: mike
 * Date: Jun 30, 2003
 * Time: 10:52:55 AM
 */
public class MessageLogger implements RequestInterceptor {
    private final static Logger log = Logger.getLogger(MessageLogger.class.getName());

    public MessageLogger() {
    }

    /**
     * Fired when a message is received from a client, after it is parsed.
     */
    public void onReceiveMessage(PolicyApplicationContext context) {
        try {
            log.info("Received client request: " + XmlUtil.nodeToString(context.getRequest().getXmlKnob().getOriginalDocument()));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error examining client request", e);
        }
    }

    /**
     * Fired when a reply is read from the SSG, after it is parsed.
     * @param context
     */
    public void onReceiveReply(PolicyApplicationContext context) {
        log.info("Received server response: " + context.getResponse());
    }

    /**
     * Fired when an error is encountered while reading the message from a client.
     * @param t The error that occurred during the request.
     */
    public void onMessageError(Throwable t) {
        log.info("Error while processing request from client: " + t);
    }

    /**
     * Fired when an error is encountered while obtaining a reply from the server.
     * @param t The error that occurred during the request.
     */
    public void onReplyError(Throwable t) {
        log.info("Error while processing response from server: " + t);
    }

    /**
     * Fired when a policy is updated.
     * @param binding
     * @param policy
     */
    public void onPolicyUpdated(Ssg ssg, PolicyAttachmentKey binding, Policy policy) {
        log.info("Policy updated for Gateway: " + ssg);
    }
}
