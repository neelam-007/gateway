/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.common.util.CertificateDownloader;
import com.l7tech.common.util.SslUtils;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.ClientKeyManager;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.PolicyManager;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.util.CannedSoapFaults;
import com.l7tech.proxy.util.ThreadLocalHttpClient;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Category;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.JDKKeyPairGenerator;

import javax.net.ssl.SSLHandshakeException;
import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Class that processes messages in request->response fashion.
 * TODO: This class is a total hairball.  Needs refactoring, bad.
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
     * @return String containing the message returned by the server
     * @throws PolicyAssertionException if the policy could not be fulfilled for this request to this SSG
     * @throws ConfigurationException if the SSG configuration is not valid
     * @throws IOException if there was a problem obtaining the response from the server
     * @throws GeneralSecurityException if there was a problem with the login or the certificates
     */
    public String processMessage(PendingRequest pendingRequest)
            throws PolicyAssertionException, ConfigurationException, IOException, GeneralSecurityException
    {
        if (pendingRequest.incrementTimesAttempted() > 10)
            throw new ConfigurationException("Unable to fulfil request after 10 attempts to contact the SSG; giving up");
        Ssg ssg = pendingRequest.getSsg();
        Assertion policy = policyManager.getPolicy(pendingRequest);
        AssertionStatus result = policy.decorateRequest(pendingRequest);
        if (result != AssertionStatus.NONE) {
            if (pendingRequest.isCredentialsWouldHaveHelped()) {
                log.info("Policy failed, possibly due to lack of credentials.  Will ask for some, then try again");
                Managers.getCredentialManager().getCredentials(ssg);
                pendingRequest.reset();
                result = policy.decorateRequest(pendingRequest);
            }
            if (pendingRequest.isClientCertWouldHaveHelped()) {
                if (!ssg.isCredentialsConfigured()) {
                    log.warn("Could not find username and password for SSG " + ssg + "; will try without them");
                } else {
                    log.info("Policy failed, possibly due to lack of a client certificate.  Will request one, then try again.");
                    JDKKeyPairGenerator.RSA kpg = new JDKKeyPairGenerator.RSA();
                    KeyPair keyPair = kpg.generateKeyPair();
                    PKCS10CertificationRequest csr = SslUtils.makeCsr(ssg.getUsername(),
                                                                      keyPair.getPublic(),
                                                                      keyPair.getPrivate());
                    X509Certificate cert = SslUtils.obtainClientCertificate(ssg.getServerCertRequestUrl(),
                                                                            ssg.getUsername(),
                                                                            ssg.getPassword(),
                                                                            csr);
                    ClientKeyManager.saveClientCertificate(ssg, keyPair.getPrivate(), cert);
                    pendingRequest.reset();
                    result = policy.decorateRequest(pendingRequest);
                }
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
     * @return String containing the response message from the server.
     * @throws ConfigurationException if the SSG configuration is not valid
     * @throws IOException if there was a problem obtaining the response from the server
     * @throws PolicyAssertionException if our internal recursive call to processMessage() threw it
     * @throws CertificateException if the SSG provides a certificate that makes no sense
     * @throws KeyStoreException if the SSG key could not be stored in our trustStore
     */
    // You might want to close your eyes for this part
    private String callSsg(PendingRequest pendingRequest)
            throws ConfigurationException, IOException, PolicyAssertionException, GeneralSecurityException
    {
        Ssg ssg = pendingRequest.getSsg();

        URL url = null;
        try {
            url = ssg.getServerUrl();
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

        postMethod.addRequestHeader("SOAPAction", pendingRequest.getSoapAction());
        postMethod.setRequestBody(pendingRequest.getSoapEnvelope().toString());
        try {
            log.info("Posting request to SSG " + ssg + ", url " + url);
            int status = 0;
            try {
                status = client.executeMethod(postMethod);
            } catch (SSLHandshakeException e) {
                installSsgServerCertificate(pendingRequest);
                postMethod.releaseConnection(); // free up our thread's HTTP client
                return callSsg(pendingRequest); // try again
            }
            log.info("POST to SSG completed with HTTP status code " + status);
            Header policyUrlHeader = postMethod.getResponseHeader("PolicyUrl");
            if (policyUrlHeader != null) {
                log.info("SSG response contained a PolicyUrl header: " + policyUrlHeader.getValue());
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
            if (status == 401 || status == 500) {
                Managers.getCredentialManager().notifyInvalidCredentials(ssg);
                Managers.getCredentialManager().getCredentials(pendingRequest.getSsg());
                if (pendingRequest.getTimesCredentialsUpdated() < 2) {
                    // Retry message processing with possibly-shiny-and-new credentials
                    postMethod.releaseConnection(); // Free up our thread's HTTP client
                    pendingRequest.incrementTimesCredentialsUpdated();
                    pendingRequest.reset();
                    return processMessage(pendingRequest);
                }
                // fall through and allow SSG error message to reach client
            }

            Header contentType = postMethod.getResponseHeader("Content-Type");
            log.info("Response Content-Type: " + contentType);
            if (contentType == null || contentType.getValue() == null || contentType.getValue().indexOf("text/xml") < 0)
                return CannedSoapFaults.RESPONSE_NOT_XML;

            try {
                final InputStream responseStream = postMethod.getResponseBodyAsStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
                StringBuffer response = new StringBuffer();
                String line;
                while ((line = reader.readLine()) != null)
                    response.append(line);
                log.info("Got response from SSG: " + response);
                return response.toString();
            } catch (NullPointerException e) {
                log.error(e);
                e.printStackTrace();
                throw new IOException("Unable to discern a SOAPEnvelope from the server's response: " + e.toString());
            }
        } finally {
            postMethod.releaseConnection();
        }
    }

    /**
     * Get credentials, and download and install the SSG certificate.  If this completes successfully, the
     * next attempt to connect to the SSG via SSL should succeed.
     *
     * @throws KeyStoreException if the SSG key could not be stored in our trustStore
     */
    private void installSsgServerCertificate(PendingRequest req)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, LoginException
    {
        Ssg ssg = req.getSsg();
        CertificateDownloader cd = new CertificateDownloader(ssg.getServerUrl(),
                                                             ssg.getUsername(),
                                                             ssg.getPassword());

        for (;;) {
            while (ssg.getUsername() == null || ssg.getUsername().length() < 1 || ssg.getPassword() == null) {
                if (!ssg.isPromptForUsernameAndPassword() || req.getTimesCredentialsUpdated() > 2)
                    throw new LoginException("User canceled the login dialog.");

                Managers.getCredentialManager().getCredentials(ssg);
                req.incrementTimesCredentialsUpdated();
            }
            cd.setUsername(ssg.getUsername());
            cd.setPassword(ssg.getPassword());

            if (cd.downloadCertificate()) {
                ClientKeyManager.saveSsgCertificate(ssg, (X509Certificate) cd.getCertificate());
                return; // Success.
            } else {
                if (!ssg.isPromptForUsernameAndPassword() || req.getTimesCredentialsUpdated() > 2)
                    throw new LoginException("User canceled the login dialog.");
                Managers.getCredentialManager().notifyInvalidCredentials(ssg);
                Managers.getCredentialManager().getCredentials(req.getSsg());
                req.incrementTimesCredentialsUpdated();
            }
        }
    }
}
