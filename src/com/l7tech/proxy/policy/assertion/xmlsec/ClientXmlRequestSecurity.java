package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.xml.SecureConversationTokenHandler;
import com.l7tech.common.security.xml.Session;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.common.security.xml.ElementSecurity;
import com.l7tech.common.security.xml.SecurityProcessor;
import com.l7tech.common.security.xml.SecurityProcessorException;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpClientCert;
import com.l7tech.proxy.util.ClientLogger;
import org.w3c.dom.Document;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * XML Digital signature on the soap request sent from the proxy to the ssg server. Also does XML
 * Encryption of the request's body if the assertion's property requires it.
 * <p/>
 * On the server side, this must verify that the SoapRequest contains a valid xml d-sig for the entire envelope and
 * maybe decyphers the body.
 * <p/>
 * On the proxy side, this must decorate a request with an xml d-sig and maybe encrypt the body.
 * <p/>
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ClientXmlRequestSecurity extends ClientAssertion {
    private static final ClientLogger log = ClientLogger.getInstance(ClientHttpClientCert.class);

    public ClientXmlRequestSecurity(XmlRequestSecurity data) {
        this.xmlRequestSecurity = data;
        if (data == null) {
            throw new IllegalArgumentException("security elements is null");
        }
    }

    /**
     * ClientProxy client-side processing of the given request.
     *
     * @param request The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     */
    public AssertionStatus decorateRequest(PendingRequest request)
      throws OperationCanceledException, BadCredentialsException,
      GeneralSecurityException, IOException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, ClientCertificateException {
        // GET THE SOAP DOCUMENT
        Document soapmsg = null;
        soapmsg = request.getSoapEnvelope(); // this will make a defensive copy as needed

        // get the client cert and private key
        // We must have credentials to get the private key
        Ssg ssg = request.getSsg();
        PrivateKey userPrivateKey = null;
        X509Certificate userCert = null;
        Session session;
        request.getCredentials();
        session = request.getOrCreateSession();

        request.prepareClientCertificate();

        try {
            userPrivateKey = SsgKeyStoreManager.getClientCertPrivateKey(ssg);
            userCert = SsgKeyStoreManager.getClientCert(ssg);
            X509Certificate ssgCert = SsgKeyStoreManager.getServerCert(ssg);
            final SignerInfo si = new SignerInfo(userPrivateKey, new X509Certificate[] { userCert, ssgCert });
            // decorate request with session info and seq nr
            request.setSession(session);
            long sessId = session.getId();
            long seqNr = session.nextSequenceNumber();
            byte[] keyreq = session.getKeyReq();
            Key encryptionKey = keyreq != null ? new AesKey(keyreq, 128) : null;
            SecureConversationTokenHandler.appendSessIdAndSeqNrToDocument(soapmsg, sessId, seqNr);
            ElementSecurity[] elements = xmlRequestSecurity.getElements();
            SecurityProcessor signer = SecurityProcessor.getSigner(session, si, encryptionKey, elements);
            signer.processInPlace(soapmsg);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (SecurityProcessorException e) {
            throw new RuntimeException(e);
        } catch ( SoapUtil.MessageNotSoapException e ) {
            throw new RuntimeException(e);
        }
//        XmlUtil.documentToOutputStream(soapmsg, System.out);
        request.setSoapEnvelope(soapmsg);

        if (!request.isSslRequired() || request.isSslForbidden()) {
            log.info("Using client cert to sign request without using SSL.  Will send nonce.");
            request.setNonceRequired(true);
        }

        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) {
        // no action on response
        return AssertionStatus.NONE;
    }

    public String getName() {
        return "XML Request Security - " + (xmlRequestSecurity.hasEncryptionElement() ? "sign and encrypt" : "sign only");
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }

    protected XmlRequestSecurity xmlRequestSecurity;
}
