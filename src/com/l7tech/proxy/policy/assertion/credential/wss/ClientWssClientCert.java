/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.wss;

import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.wss.WssClientCert;
import com.l7tech.xmlsig.SoapMsgSigner;
import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import org.w3c.dom.Document;

import java.security.PrivateKey;
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
            soapmsg = request.getSoapEnvelope().getAsDocument();
        } catch (Exception e) {
            throw new PolicyAssertionException("cannot get request document", e);
        }

        PrivateKey userPrivateKey = null;
        X509Certificate userCert = null;
        // todo, where do i get those two things ?

        SoapMsgSigner dsigHelper = new SoapMsgSigner();
        try {
            dsigHelper.signEnvelope(soapmsg, userPrivateKey, userCert);
        } catch (SignatureStructureException e) {
            throw new PolicyAssertionException("error signing document", e);
        } catch (XSignatureException e) {
            throw new PolicyAssertionException("error signing document", e);
        }

        // todo, feed back soapmsg into the PendingRequest

        // return AssertionStatus.NONE;
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

    protected WssClientCert data;
}
