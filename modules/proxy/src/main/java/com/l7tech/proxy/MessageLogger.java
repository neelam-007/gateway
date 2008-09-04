/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.IOUtils;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.message.MimeKnob;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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
    public void onFrontEndRequest(PolicyApplicationContext context) {
        if (!log.isLoggable(Level.FINE)) return;
        try {
            String requestStr = toString(context.getRequest().getMimeKnob());
            log.fine("Received client request: " + requestStr);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error examining client request", e);
        }
    }

    public void onFrontEndReply(PolicyApplicationContext context) {
        if (!log.isLoggable(Level.FINE)) return;
        String responseStr = toString(context.getResponse().getMimeKnob());
        log.fine("Received server response: " + (responseStr == null ? "<null>" : responseStr));
    }

    public void onBackEndRequest(PolicyApplicationContext context, List<HttpHeader> headersSent) {
        if (!log.isLoggable(Level.FINE)) return;
        String str = toString(context.getRequest().getMimeKnob());
        log.fine("Transmitting decorated request: " + (str == null ? "<null>" : str));
    }

    public void onBackEndReply(PolicyApplicationContext context) {
        if (!log.isLoggable(Level.FINE)) return;
        String str = toString(context.getResponse().getMimeKnob());
        log.fine("Received decorated response: " + (str == null ? "<null>" : str));
    }

    private static String toString(MimeKnob mk) {
        String responseStr = null;
        InputStream is = null;
        BufferPoolByteArrayOutputStream baos = null;
        try {
            if (mk != null) {
                is = mk.getFirstPart().getInputStream(false);
                baos = new BufferPoolByteArrayOutputStream();
                IOUtils.copyStream(is, baos);
                responseStr = baos.toString();
            }
        } catch (IOException e) {
            responseStr = "<IOException: " + ExceptionUtils.getMessage(e) + '>';
        } catch (NoSuchPartException e) {
            responseStr = "<NoSuchPartException: " + ExceptionUtils.getMessage(e) + '>';
        } finally {
            if (is != null) try { is.close(); } catch (IOException e) { /* too late to care */ }
            if (baos != null) baos.close();
        }
        return responseStr;
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
