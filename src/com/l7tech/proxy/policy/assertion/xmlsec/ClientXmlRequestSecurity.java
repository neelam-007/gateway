package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
import com.l7tech.xmlsig.SoapMsgSigner;
import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import org.w3c.dom.Document;
import java.security.PrivateKey;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.io.IOException;

/**
 * User: flascell
 * Date: Aug 26, 2003
 * Time: 2:54:01 PM
 * $Id$
 *
 * XML Digital signature on the soap request sent from a requestor (probably proxy) to the ssg server
 *
 * On the server side, this must verify that the SoapRequest contains a valid xml d-sig for the entire envelope.
 * On the proxy side, this must decorate a request with an xml d-sig
 *
 * This extends CredentialSourceAssertion because once the validity of the signature if confirmed, the cert is used
 * as credentials.
 *
 * @author flascell
 */
public class ClientXmlRequestSecurity extends ClientAssertion {

    public ClientXmlRequestSecurity(XmlRequestSecurity data) {
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

        // must we encrypt the body before signing the envelope?
        if (data.isEncryption()) {
            // todo, encryption of body
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

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected XmlRequestSecurity data;
}
