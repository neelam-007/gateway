/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.security.xml.Session;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.CertificateAlreadyIssuedException;
import com.l7tech.proxy.datamodel.exceptions.ClientCertificateException;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.PolicyRetryableException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.util.ClientLogger;
import org.w3c.dom.Document;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * Holds request state while the client proxy is processing it.
 * User: mike
 * Date: Jun 16, 2003
 * Time: 12:04:09 PM
 */
public class PendingRequest {
    private static final ClientLogger log = ClientLogger.getInstance(PendingRequest.class);

    //private ClientProxy clientProxy;
    private final CredentialManager credentialManager = Managers.getCredentialManager();
    private Document soapEnvelope;
    private Document initialEnvelope;
    private final Ssg ssg;
    private final RequestInterceptor requestInterceptor;
    private String soapAction = "";
    private String uri = "";
    private SsgResponse lastErrorResponse = null; // Last response received from SSG in the case of 401 or 500 status
    private boolean isPolicyUpdated = false;
    private URL originalUrl = null;
    private Long nonce = null; // nonce.  set on-demand, and only set once
    private HttpHeaders headers = null;
    private PasswordAuthentication pw = null;
    private ClientSidePolicy clientSidePolicy = ClientSidePolicy.getPolicy();

    // Policy settings, filled in by traversing policy tree
    private static class PolicySettings {
        private Policy activePolicy= null; // the policy that we most recently started applying to this request, if any
        private boolean isSslRequired = false;
        private boolean sslForbidden = false;  // ssl is forbidden for this request
        private boolean isBasicAuthRequired = false;
        private boolean isDigestAuthRequired = false;
        private boolean isNonceRequired = false;
        private Session session = null; // session for the response to this request.  can't be changed once sent
    }
    private PolicySettings policySettings = new PolicySettings();

    /** Construct a PendingRequest around the given SOAPEnvelope going to the given SSG. */
    public PendingRequest(Document soapEnvelope, Ssg ssg, RequestInterceptor requestInterceptor) {
        this.soapEnvelope = soapEnvelope;
        this.initialEnvelope = soapEnvelope;
        this.ssg = ssg;
        this.requestInterceptor = requestInterceptor;
    }

    public PendingRequest(Document soapEnvelope, Ssg ssg, RequestInterceptor ri, URL origUrl, HttpHeaders headers) {
        this(soapEnvelope, ssg, ri);
        this.originalUrl = origUrl;
        this.headers = headers;
    }

    /**
     * Reset all policy settings in preperation for starting processing over again with a different policy.
     */
    public void reset() {
        policySettings = new PolicySettings();
        soapEnvelope = initialEnvelope;
    }

    /**
     * Ensure that a client certificate is available for the current request.  Will apply for one
     * if necessary.
     */
    public void prepareClientCertificate() throws OperationCanceledException, KeyStoreCorruptException,
            GeneralSecurityException, ClientCertificateException,
            ServerCertificateUntrustedException, BadCredentialsException, PolicyRetryableException
    {
        try {
            if (!SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
                log.info("PendingRequest: applying for client certificate");
                SsgKeyStoreManager.obtainClientCertificate(ssg, getCredentials());
            }
        } catch (CertificateAlreadyIssuedException e) {
            // Bug #380 - if we haven't updated policy yet, try that first - mlyons
            if (!isPolicyUpdated()) {
                Managers.getPolicyManager().flushPolicy(this);
                throw new PolicyRetryableException();
            } else {
                Managers.getCredentialManager().notifyCertificateAlreadyIssued(ssg);
                throw new OperationCanceledException();
            }
        } catch (IOException e) {
            throw new ClientCertificateException("Unable to obtain a client certificate", e);
        }
    }

    /** Manually set the credentials to use throughout this request. */
    public void setCredentials(PasswordAuthentication pw) {
        this.pw = pw;
    }

    /**
     * Assert that credentials must be available to continue processing this request,
     * but that the existing ones were found to be no good.  Other than first throwing out
     * any existing configured credentials and displaying an error message,
     * this behaves like gatherCredentials().
     */
    public PasswordAuthentication getNewCredentials() throws OperationCanceledException, HttpChallengeRequiredException {
        if (ssg.isChainCredentialsFromClient())
            throw new HttpChallengeRequiredException("Invalid username or password");
        return this.pw = credentialManager.getNewCredentials(ssg);
    }

    /**
     * Assert that credentials must be available to continue processing this request.
     * The password will be loaded from the keystore if necessary.
     * The user will be prompted for credentials if necessary.
     * If this method returns, getUsername() and getPassword() are guaranteed to return non-null
     * values for the rest of the lifetime of this request.
     *
     * @throws OperationCanceledException if credentials are not available, and the CredentialManager
     *                                    was unable to get some, possibly because the user canceled
     *                                    the logon dialog.
     */
    public PasswordAuthentication getCredentials() throws OperationCanceledException {
        if (pw == null || pw.getUserName() == null || pw.getUserName().length() < 1 || pw.getPassword() == null)
            pw = credentialManager.getCredentials(ssg);
        return pw;
    }

    public String getUsername() throws OperationCanceledException {
        return getCredentials().getUserName();
    }

    public char[] getPassword() throws OperationCanceledException {
        return getCredentials().getPassword();
    }


    public long getNonce() {
        if (nonce == null)
            nonce = new Long(new SecureRandom().nextLong());
        return nonce.longValue();
   }

    // Getters and setters

    /**
     * Get (a copy of) the Document representing the request.  This returns a copy that can be
     * modified freely.  If you will not be modifying the returned Document in any way, you can
     * get additional performance by using getSoapEnvelopeDirect() instead.
     *
     * If you want your changes to stick, you'll need to save them back by calling setSoapEnvelope().
     *
     * @return A copy of the SOAP envelope Document, which may be freely modified.
     */
    public Document getSoapEnvelope() {
        return (Document) soapEnvelope.cloneNode(true);
    }

    /**
     * Get the actual Document representing the request, which should not be modified.  Any change
     * to this Document will prevent the reset() method from returning this PendingRequest to
     * its original state.  If you need to change the Document, use getSoapEnvelope() instead.
     *
     * @return A reference to the SOAP envelope Document, which must not be modified in any way.
     */
    public Document getSoapEnvelopeDirectly() {
        return soapEnvelope;
    }

    /**
     * Replace the active Soap Envelope with a new one.  The original envelope is in any case kept
     * squirreled away so that reset() will work.
     * @param newEnvelope
     */
    public void setSoapEnvelope(Document newEnvelope) {
        soapEnvelope = newEnvelope;
    }

    public Ssg getSsg() {
        return ssg;
    }

    public String getSoapAction() {
        return soapAction;
    }

    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public RequestInterceptor getRequestInterceptor() {
        return requestInterceptor;
    }

    public boolean isSslRequired() {
        return policySettings.isSslRequired;
    }

    public void setSslRequired(boolean sslRequired) {
        policySettings.isSslRequired = sslRequired;
    }

    public boolean isNonceRequired() {
        return policySettings.isNonceRequired;
    }

    public void setNonceRequired(boolean isNonceRequired) {
        policySettings.isNonceRequired = isNonceRequired;
    }

    public boolean isBasicAuthRequired() {
        return policySettings.isBasicAuthRequired;
    }

    public void setBasicAuthRequired(boolean basicAuthRequired) {
        policySettings.isBasicAuthRequired = basicAuthRequired;
    }

    public boolean isDigestAuthRequired() {
        return policySettings.isDigestAuthRequired;
    }

    public void setDigestAuthRequired(boolean digestAuthRequired) {
        policySettings.isDigestAuthRequired = digestAuthRequired;
    }

    public boolean isPolicyUpdated() {
        return isPolicyUpdated;
    }

    public void setPolicyUpdated(boolean policyUpdated) {
        isPolicyUpdated = policyUpdated;
        if (isPolicyUpdated)
            // Forget any cached session cookies, for all services shared by this SSG
            ssg.clearSessionCookies();
    }

    /**
     * Get the session that was passed with the initial request, or null if we don't have one yet.
     *
     * @return
     */
    public Session getSession() {
        return policySettings.session;
    }

    /**
     * Get or create a session for this Ssg.
     */
    public Session getOrCreateSession()
            throws ServerCertificateUntrustedException, BadCredentialsException, IOException,
                   OperationCanceledException
    {
        Session session = getSession();
        if (session != null)
            return session;

        session = SsgSessionManager.getOrCreateSession(getSsg(), getActivePolicy().getVersion());
        setSession(session);
        return session;
    }

    /**
     * Set the session that will be referred to in the request, and used to decrypt/etc the reply.
     * A reference to it is kept here so that even if the real session is invalidated on the Ssg
     * by another thread after the request is sent, we'll still be able to work with the outstanding
     * reply for this request.  (We can't just reissue the request at that point, since it will already
     * have been successfully passed through the ssg to the target server.)
     *
     * @param session
     */
    public void setSession(Session session) {
        policySettings.session = session;
    }

    public SsgResponse getLastErrorResponse() {
        return lastErrorResponse;
    }

    public void setLastErrorResponse(SsgResponse lastErrorResponse) {
        this.lastErrorResponse = lastErrorResponse;
    }

    public URL getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(URL originalUrl) {
        this.originalUrl = originalUrl;
    }

    public void setSslForbidden(boolean sslForbidden) {
        this.policySettings.sslForbidden = sslForbidden;
    }

    public boolean isSslForbidden() {
        return this.policySettings.sslForbidden;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public ClientSidePolicy getClientSidePolicy() {
        return clientSidePolicy;
    }

    /** @return the policy we are currently applying to this request, or null if we don't know or don't have one. */
    public Policy getActivePolicy() {
        return this.policySettings.activePolicy;
    }

    /** Set the policy we are going to apply to this request. */
    public void setActivePolicy(Policy policy) {
        this.policySettings.activePolicy = policy;
    }
}
