package com.l7tech.proxy.policy.assertion.xmlsec;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.SsgSessionManager;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpClientCert;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.xmlsig.SoapMsgSigner;
import com.l7tech.xmlsig.SecureConversationTokenHandler;
import com.l7tech.xmlenc.XmlMangler;
import com.l7tech.xmlenc.Session;
import org.w3c.dom.Document;
import org.apache.log4j.Category;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.io.IOException;

/**
 * User: flascell
 * Date: Aug 26, 2003
 * Time: 2:54:01 PM
 * $Id$
 *
 * XML Digital signature on the soap request sent from the proxy to the ssg server. Also does XML
 * Encryption of the request's body if the assertion's property requires it.
 *
 * On the server side, this must verify that the SoapRequest contains a valid xml d-sig for the entire envelope and
 * maybe decyphers the body.
 *
 * On the proxy side, this must decorate a request with an xml d-sig and maybe encrypt the body.
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
    public AssertionStatus decorateRequest(PendingRequest request)
            throws PolicyAssertionException, OperationCanceledException, BadCredentialsException,
                   ServerCertificateUntrustedException
    {
        // GET THE SOAP DOCUMENT
        Document soapmsg = null;
        try {
            soapmsg = request.getSoapEnvelope(); // this will make a defensive copy as needed
        } catch (Exception e) {
            throw new PolicyAssertionException("cannot get request document", e);
        }

        // GET THE CLIENT CERT AND PRIVATE KEY
        // We must have credentials to get the private key
        Ssg ssg = request.getSsg();
        PrivateKey userPrivateKey = null;
        X509Certificate userCert = null;
        Session session;
        synchronized (ssg) {
            if (!ssg.isCredentialsConfigured())
                Managers.getCredentialManager().getCredentials(ssg);

            try {
                session = SsgSessionManager.getOrCreateSession(ssg);
            } catch (IOException e) {
                throw new PolicyAssertionException("Unable to establish session with SSG " + ssg, e);
            }

            if (!SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
                try {
                    request.getClientProxy().obtainClientCertificate(ssg);
                } catch (GeneralSecurityException e) {
                    throw new PolicyAssertionException("Unable to obtain a client certificate with SSG " + ssg, e);
                } catch (IOException e) {
                    throw new PolicyAssertionException("Unable to obtain a client certificate with SSG " + ssg, e);
                }
            }

            try {
                userPrivateKey = SsgKeyStoreManager.getClientCertPrivateKey(ssg);
                userCert = SsgKeyStoreManager.getClientCert(ssg);
            } catch (NoSuchAlgorithmException e) {
                throw new PolicyAssertionException(e);
            } catch (BadCredentialsException e) {
                throw new PolicyAssertionException(e);
            } catch (OperationCanceledException e) {
                throw new PolicyAssertionException(e);
            }
        }

        // DECORATE REQUEST WITH SESSION INFO AND SEQ NR
        request.setSession(session);
        long sessId = session.getId();
        long seqNr = session.nextSequenceNumber();
        byte[] keyreq = session.getKeyReq();
        SecureConversationTokenHandler.appendSessIdAndSeqNrToDocument(soapmsg, sessId, seqNr);

        // ENCRYPTION
        if (data.isEncryption()) {
            try {
                XmlMangler.encryptXml(soapmsg, keyreq, Long.toString(sessId));
            } catch (GeneralSecurityException e) {
                throw new PolicyAssertionException("error encrypting document", e);
            } catch (IOException e) {
                throw new PolicyAssertionException("error encrypting document", e);
            } catch (IllegalArgumentException e) {
                throw new PolicyAssertionException("error encrypting document", e);
            }
            log.info("Encrypted request OK");
        }

        // DIGITAL SIGNATURE
        SoapMsgSigner dsigHelper = new SoapMsgSigner();
        try {
            dsigHelper.signEnvelope(soapmsg, userPrivateKey, userCert);
        } catch (SignatureStructureException e) {
            throw new PolicyAssertionException("error signing document", e);
        } catch (XSignatureException e) {
            throw new PolicyAssertionException("error signing document", e);
        }
        log.info("Signed request OK");

        // SET BACK ALL THIS IN PENDING REQUEST
        request.setSoapEnvelope(soapmsg);

        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected XmlRequestSecurity data;
    private static final Category log = Category.getInstance(ClientHttpClientCert.class);
}
