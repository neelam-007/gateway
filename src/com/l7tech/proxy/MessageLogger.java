/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.util.XmlUtil;
import org.apache.log4j.Category;
import org.w3c.dom.Document;

import java.io.IOException;

/**
 * RequestInterceptor that logs all messages in and out.
 * User: mike
 * Date: Jun 30, 2003
 * Time: 10:52:55 AM
 */
public class MessageLogger implements RequestInterceptor {
    private final static Category log = Category.getInstance(MessageLogger.class);

    public MessageLogger() {
    }

    /**
     * Fired when a message is received from a client, after it is parsed.
     * @param message
     */
    public void onReceiveMessage(Document message) {
        try {
            log.info("Received client request: " + XmlUtil.documentToString(message));
        } catch (IOException e) {
            log.error("Error examining client request", e);
        }
    }

    /**
     * Fired when a reply is read from the SSG, after it is parsed.
     * @param reply
     */
    public void onReceiveReply(SsgResponse reply) {
        log.info("Received server response: " + reply);
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
    public void onPolicyUpdated(Ssg ssg, PolicyAttachmentKey binding, Assertion policy) {
        log.info("Policy updated for SSG: " + ssg);
    }
}
