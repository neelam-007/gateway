/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.PolicyManager;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.util.ThreadLocalHttpClient;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Category;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * Class that processes messages in request->response fashion.
 *
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
     * @return the SOAP Envelope returned by the server
     * @throws PolicyAssertionException if the policy could not be fulfilled for this request to this SSG
     * @throws ConfigurationException if the SSG configuration is not valid
     * @throws IOException if there was a problem obtaining the response from the server
     * @throws SAXException if the response from the server was not a valid SOAP envelope
     */
    public SOAPEnvelope processMessage(PendingRequest pendingRequest)
            throws PolicyAssertionException, ConfigurationException, SAXException, IOException
    {
        Assertion policy = policyManager.getPolicy(pendingRequest);
        AssertionStatus result = policy.decorateRequest(pendingRequest);
        if (result != AssertionStatus.NONE) {
            if (pendingRequest.isCredentialsWouldHaveHelped()) {
                log.info("Policy failed, possibly due to lack of credentials.  Will ask for some, then trying again");
                Managers.getCredentialManager().getCredentials(pendingRequest.getSsg());
                pendingRequest.reset();
                result = policy.decorateRequest(pendingRequest);
            }
        }
        if (result != AssertionStatus.NONE)
            log.error("Unable to conform to policy as required by server; attempting to continue anyway");
        return callSsg(pendingRequest);
    }

    /**
     * Transmit the modified request to the SSG and return its response.
     * We may make more than one call to the SSG if we need to resolve a policy.
     * @param pendingRequest
     * @return SOAPEnvelope containing the response message from the server.
     * @throws ConfigurationException if the SSG configuration is not valid
     * @throws IOException if there was a problem obtaining the response from the server
     * @throws SAXException if the response from the server was not a valid SOAP envelope
     * @throws PolicyAssertionException if our internal recursive call to processMessage() threw it
     */
    // You might want to close your eyes for this part
    private SOAPEnvelope callSsg(PendingRequest pendingRequest)
            throws ConfigurationException, IOException, SAXException, PolicyAssertionException
    {
        Ssg ssg = pendingRequest.getSsg();

        URL url = null;
        try {
            url = new URL(ssg.getServerUrl());
            if (pendingRequest.isSslRequired()) {
                if ("http".equalsIgnoreCase(url.getProtocol())) {
                    log.info("Changing http to https per policy for this request (using SSL port " +
                             ssg.getSslPort() + ")");
                    url = new URL("https", url.getHost(), ssg.getSslPort(), url.getFile());
                } else
                    throw new ConfigurationException("Couldn't find an SSL-enabled version of protocol " +
                                                     url.getProtocol());
            }
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Client Proxy: SSG \"" + ssg + "\" has an invalid server url: " +
                                             ssg.getServerUrl());
        }

        HttpClient client = ThreadLocalHttpClient.getHttpClient();
        HttpState state = client.getState();
        PostMethod postMethod = new PostMethod(url.toString());

        state.setAuthenticationPreemptive(false);
        if (pendingRequest.isBasicAuthRequired()) {
            log.info("Enabling HTTP Basic auth with username=" + pendingRequest.getHttpBasicUsername());
            postMethod.setDoAuthentication(true);
            state.setAuthenticationPreemptive(true);
            state.setCredentials(null, null,
                                 new UsernamePasswordCredentials(pendingRequest.getHttpBasicUsername(),
                                                                 new String(pendingRequest.getHttpBasicPassword())));
        } else if (pendingRequest.isDigestAuthRequired()) {
            log.info("Enabling HTTP Digest auth with username=" + pendingRequest.getHttpDigestUsername());
            postMethod.setDoAuthentication(true);
            state.setCredentials(null, null,
                                 new UsernamePasswordCredentials(pendingRequest.getHttpDigestUsername(),
                                                                 new String(pendingRequest.getHttpDigestPassword())));
        } else
            log.info("No authentication specified by current policy");

        postMethod.setRequestBody(pendingRequest.getSoapEnvelope().toString());
        try {
            log.info("Posting request to SSG url " + ssg.getServerUrl());
            int status = client.executeMethod(postMethod);
            log.info("POST to SSG completed with HTTP status code " + status);
            Header policyUrlHeader = postMethod.getResponseHeader("PolicyUrl");
            if (policyUrlHeader != null) {
                // Have we already updated a policy while processing this request?
                if (pendingRequest.isPolicyUpdated())
                    throw new ConfigurationException("Policy was updated, but SSG says it's still out-of-date");
                try {
                    URL policyUrl = new URL(policyUrlHeader.getValue());
                    postMethod.releaseConnection(); // free up our thread's HTTP client
                    policyManager.updatePolicy(pendingRequest, policyUrl);
                    // Recursively attempt message processing again, with the updated policy
                    pendingRequest.setPolicyUpdated(true);
                    pendingRequest.reset();
                    return processMessage(pendingRequest);
                } catch (MalformedURLException e) {
                    throw new ConfigurationException("SSG gave us an invalid Policy URL");
                }
            }
            if (status == 401) {
                Managers.getCredentialManager().getCredentials(pendingRequest.getSsg());
                if (!pendingRequest.isCredentialsUpdated()) {
                    // Retry message processing a single time with possibly-shiny-and-new credentials
                    pendingRequest.setCredentialsUpdated(true);
                    pendingRequest.reset();
                    return processMessage(pendingRequest);
                }
                if (policyUrlHeader == null)
                    Managers.getCredentialManager().notifyInvalidCredentials(ssg);
            }

            try {
                final InputStream responseStream = postMethod.getResponseBodyAsStream();
                SOAPEnvelope response = new SOAPEnvelope(new InputStream() {
                    StringBuffer accum = new StringBuffer(1024);
                    public int read() throws IOException {
                        int c = responseStream.read();
                        if (c == -1) {
                            log.info("Read server response: " + accum);
                        } else {
                            accum.append((char)c);
                        }
                        return c;
                    }
                });
                return response;
            } catch (NullPointerException e) {
                log.error(e);
                throw new IOException("Unable to discern a SOAPEnvelope from the server's response: " + e.toString());
            }
        } finally {
            postMethod.releaseConnection();
        }
    }


}
