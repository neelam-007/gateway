/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.PolicyManager;
import com.l7tech.proxy.datamodel.Ssg;
import org.apache.axis.client.Call;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.log4j.Category;
import org.mortbay.http.HttpException;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * Class that processes messages in request->response fashion.
 * User: mike
 * Date: Jun 17, 2003
 * Time: 10:12:25 AM
 */
public class MessageProcessor {
    private static final Category log = Category.getInstance(MessageProcessor.class);
    private PolicyManager policyManager;

    public MessageProcessor(PolicyManager policyManager) {
        this.policyManager = policyManager;
    }

    /**
     * Main message-processing entry point for the Client Proxy.
     * Given a request from a client, decorates it according to policy, sends it to the SSG, and
     * returns the response.
     * @param pendingRequest
     * @return
     * @throws HttpException
     */
    public SOAPEnvelope processMessage(PendingRequest pendingRequest) throws PolicyAssertionException, RemoteException, HttpException {
        Assertion policy = policyManager.getPolicy(pendingRequest);
        policy.decorateRequest(pendingRequest);
        return callSsg(pendingRequest);
    }

    /**
     * Transmit the modified request to the SSG and return its response.
     * We may make more than one call to the SSG if we need to resolve a policy.
     * @param pendingRequest
     * @return
     * @throws HttpException
     */
    private SOAPEnvelope callSsg(PendingRequest pendingRequest) throws HttpException, RemoteException {
        Ssg ssg = pendingRequest.getSsg();
        try {
            URL url = new URL(ssg.getServerUrl());
            if (pendingRequest.isSslRequired()) {
                if ("http".equalsIgnoreCase(url.getProtocol())) {
                    log.info("Changing http to https per policy for this request (using SSL port " + ssg.getSslPort() + ")");
                    url = new URL("https", url.getHost(), ssg.getSslPort(), url.getFile());
                } else
                    throw new HttpException(500, "Couldn't find an SSL-enabled version of protocol " + url.getProtocol());
            }

            Call call = new Call(url);
            if (pendingRequest.isBasicAuthRequired()) {
                call.setUsername(pendingRequest.getHttpBasicUsername());
                call.setPassword(pendingRequest.getHttpBasicPassword());
            } else if (pendingRequest.isDigestAuthRequired()) {
                call.setUsername(pendingRequest.getHttpDigestPassword());
                call.setPassword(pendingRequest.getHttpDigestPassword());
            }

            SOAPEnvelope result;
            try {
                result = call.invoke(pendingRequest.getSoapEnvelope());
            } catch (RemoteException e) {
                log.error("callSsg(): Got back a RemoteException: ");
                log.error(e);
                throw e;
            }
            return result;
        } catch (MalformedURLException e) {
            throw new HttpException(500, "Client Proxy: this SSG has an invalid server url: " + ssg.getServerUrl());
        }
    }
}
