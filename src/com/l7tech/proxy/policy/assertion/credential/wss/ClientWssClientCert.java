/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.wss;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.wss.WssClientCert;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.xmlsig.SoapMsgSigner;
import org.w3c.dom.Document;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientWssClientCert implements ClientAssertion {
    public ClientWssClientCert( WssClientCert data ) {
        this.data = data;
    }

    /**
     * ClientProxy client-side processing of the given request.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        Document soapmsg = null;
        try {
            soapmsg = request.getSoapEnvelope();
        } catch (Exception e) {
            throw new PolicyAssertionException("cannot get request document", e);
        }

        Ssg ssg = request.getSsg();

        // We must have credentials to get the private key
        if (!ssg.isCredentialsConfigured()) {
            request.setCredentialsWouldHaveHelped(true);
            return AssertionStatus.FAILED;
        }

        // We must have a client cert
        if (!SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
            request.setClientCertWouldHaveHelped(true);
            return AssertionStatus.FAILED;
        }

        PrivateKey userPrivateKey = null;
        X509Certificate userCert = null;
        try {
            userPrivateKey = SsgKeyStoreManager.getPrivateKey(ssg);
            userCert = SsgKeyStoreManager.getClientCert(ssg);
        } catch (NoSuchAlgorithmException e) {
            throw new PolicyAssertionException(e);
        } catch (IOException e) {
            throw new PolicyAssertionException(e);
        } catch (CertificateException e) {
            throw new PolicyAssertionException(e);
        } catch (KeyStoreException e) {
            throw new PolicyAssertionException(e);
        } catch (UnrecoverableKeyException e) {
            throw new PolicyAssertionException(e);
        }

        SoapMsgSigner dsigHelper = new SoapMsgSigner();
        try {
            dsigHelper.signEnvelope(soapmsg, userPrivateKey, userCert);
        } catch (SignatureStructureException e) {
            throw new PolicyAssertionException("error signing document", e);
        } catch (XSignatureException e) {
            throw new PolicyAssertionException("error signing document", e);
        }

        request.setSoapEnvelope(soapmsg);
        return AssertionStatus.NONE;
    }

    protected WssClientCert data;
}
