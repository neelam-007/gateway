/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.client.Call;
import org.mortbay.http.HttpException;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.PolicyManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;

import java.io.IOException;
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
    public SOAPEnvelope processMessage(PendingRequest pendingRequest) throws HttpException {
        try {
            Assertion policy = policyManager.getPolicy(pendingRequest.getSsg());
            policy.decorateRequest(pendingRequest);
            return callSsg(pendingRequest);
        } catch (IOException e) {
            throw new HttpException(500, "Unable to obtain the policy for this SSG: " + e.toString());
        } catch (PolicyAssertionException e) {
            throw new HttpException(500, "Unable to conform to the policy required by this SGS: " + e.toString());
        }
    }

    /**
     * Transmit the modified request to the SSG and return its response.
     * @param pendingRequest
     * @return
     * @throws HttpException
     */
    private SOAPEnvelope callSsg(PendingRequest pendingRequest) throws HttpException {
        Ssg ssg = pendingRequest.getSsg();
        try {
            URL url = new URL(ssg.getServerUrl());
            if (pendingRequest.isSslRequired()) {
                if ("http".equalsIgnoreCase(url.getProtocol()))
                    url = new URL("https", url.getHost(), url.getPort(), url.getFile());
                else
                    throw new HttpException(500, "Couldn't find an SSL-enabled version of protocol " + url.getProtocol());
            }

            Call call = new Call(ssg.getServerUrl());
            if (pendingRequest.isBasicAuthRequired()) {
                call.setUsername(pendingRequest.getHttpBasicUsername());
                call.setPassword(pendingRequest.getHttpBasicPassword());
            } else if (pendingRequest.isDigestAuthRequired()) {
                call.setUsername(pendingRequest.getHttpDigestPassword());
                call.setPassword(pendingRequest.getHttpDigestPassword());
            }

            return call.invoke(pendingRequest.getSoapEnvelope());
        } catch (MalformedURLException e) {
            throw new HttpException(500, "Client Proxy: this SSG has an invalid server url: " + ssg.getServerUrl());
        } catch (RemoteException e) {
            throw new HttpException(500, "Client Proxy: unable to reach SSG service: " + e.toString());
        }
    }
}
