package com.l7tech.proxy.policy.assertion.xmlsec;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.common.security.xml.SecureConversationTokenHandler;
import com.l7tech.common.security.xml.Session;
import com.l7tech.common.security.xml.SoapMsgSigner;
import com.l7tech.common.security.xml.XmlMangler;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.SsgSessionManager;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpClientCert;
import org.apache.log4j.Category;
import org.w3c.dom.Document;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

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
     */
    public AssertionStatus decorateRequest(PendingRequest request)
            throws OperationCanceledException, BadCredentialsException,
            GeneralSecurityException, IOException
    {
        // GET THE SOAP DOCUMENT
        Document soapmsg = null;
        soapmsg = request.getSoapEnvelope(); // this will make a defensive copy as needed

        // GET THE CLIENT CERT AND PRIVATE KEY
        // We must have credentials to get the private key
        Ssg ssg = request.getSsg();
        PrivateKey userPrivateKey = null;
        X509Certificate userCert = null;
        Session session;
        synchronized (ssg) {
            request.getCredentials();
            session = SsgSessionManager.getOrCreateSession(ssg);

            if (!SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
                try {
                    request.getClientProxy().obtainClientCertificate(request);
                } catch (ServerCertificateUntrustedException e) {
                    throw e;
                }
            }

            try {
                userPrivateKey = SsgKeyStoreManager.getClientCertPrivateKey(ssg);
                userCert = SsgKeyStoreManager.getClientCert(ssg);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e); // can't happen
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
            XmlMangler.encryptXml(soapmsg, keyreq, Long.toString(sessId));
            log.info("Encrypted request OK");
        }

        // DIGITAL SIGNATURE
        SoapMsgSigner dsigHelper = new SoapMsgSigner();
        try {
            dsigHelper.signEnvelope(soapmsg, userPrivateKey, userCert);
        } catch (SignatureStructureException e) {
            throw new RuntimeException("error signing document", e);
        } catch (XSignatureException e) {
            throw new RuntimeException("error signing document", e);
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

    public String getName() {
        return"XML Request Security - " + (data.isEncryption() ? "sign and encrypt" : "sign only");
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }

    protected XmlRequestSecurity data;
    private static final Category log = Category.getInstance(ClientHttpClientCert.class);
}
