/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.message.PolicyApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RequestInterceptor that logs all messages in and out.
 * @author mike
 */
public class MessageLogger implements RequestInterceptor {
    private final static Logger log = Logger.getLogger(MessageLogger.class.getName());

    public MessageLogger() {
    }

    /**
     * Fired when a message is received from a client, after it is parsed.
     */
    public void onReceiveMessage(PolicyApplicationContext context) {
        if (!log.isLoggable(Level.FINE)) return;
        try {
            log.fine("Received client request: " + XmlUtil.nodeToString(context.getRequest().getXmlKnob().getOriginalDocument()));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error examining client request", e);
        }
    }

    public void onReceiveReply(PolicyApplicationContext context) {
        if (!log.isLoggable(Level.FINE)) return;
        String responseStr = null;
        MimeKnob mk = context.getResponse().getMimeKnob();
        InputStream is = null;
        BufferPoolByteArrayOutputStream baos = null;
        try {
            if (mk != null) {
                is = mk.getFirstPart().getInputStream(false);
                baos = new BufferPoolByteArrayOutputStream();
                HexUtils.copyStream(is, baos);
                responseStr = baos.toString();
            }
        } catch (IOException e) {
            responseStr = "<IOException: " + ExceptionUtils.getMessage(e) + ">";
        } catch (NoSuchPartException e) {
            responseStr = "<NoSuchPartException: " + ExceptionUtils.getMessage(e) + ">";
        } finally {
            if (is != null) try { is.close(); } catch (IOException e) { /* too late to care */ }
            if (baos != null) baos.close();
        }
        log.fine("Received server response: " + responseStr == null ? "<null>" : responseStr);
    }

    /**
     * Fired when an error is encountered while reading the message from a client.
     * @param t The error that occurred during the request.
     */
    public void onMessageError(Throwable t) {
        if (!log.isLoggable(Level.WARNING)) return;
        log.warning("Error while processing request from client: " + t);
    }

    /**
     * Fired when an error is encountered while obtaining a reply from the server.
     * @param t The error that occurred during the request.
     */
    public void onReplyError(Throwable t) {
        if (!log.isLoggable(Level.INFO)) return;
        log.info("Error while processing response from server: " + t);
    }

    public void onPolicyUpdated(Ssg ssg, PolicyAttachmentKey binding, Policy policy) {
        if (!log.isLoggable(Level.INFO)) return;
        log.info("Policy updated for Gateway: " + ssg);
    }

    public void onPolicyError(Ssg ssg, PolicyAttachmentKey binding, Throwable error) {
        if (!log.isLoggable(Level.WARNING)) return;
        log.warning("Policy download failed for Gateway: " + ssg + ": " + ExceptionUtils.getMessage(error));
    }
}
