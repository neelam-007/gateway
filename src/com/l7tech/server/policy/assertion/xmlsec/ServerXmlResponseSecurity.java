package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.XmlResponseSecurity;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.HttpTransportMetadata;
import com.l7tech.util.SoapUtil;
import com.l7tech.util.KeystoreUtils;
import com.l7tech.logging.LogManager;
import com.l7tech.xmlsig.SoapMsgSigner;
import com.l7tech.xmlsig.SecureConversationTokenHandler;
import com.l7tech.xmlenc.SessionManager;
import com.l7tech.xmlenc.Session;
import com.l7tech.xmlenc.SessionNotFoundException;
import com.l7tech.xmlenc.XmlMangler;
import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.PrivateKey;
import java.security.KeyStoreException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 26, 2003
 * Time: 3:15:30 PM
 * $Id$
 *
 * XML Digital signature on the soap response sent from the ssg server to the requestor (probably proxy). Also does
 * XML Encryption of the response's body if the assertion's property dictates it.
 *
 * On the server side, this decorates a response with an xml d-sig and maybe signs the body.
 * On the proxy side, this verifies that the Soap Response contains a valid xml d-sig for the entire envelope and maybe
 * decyphers the body.
 */
public class ServerXmlResponseSecurity implements ServerAssertion {

    public ServerXmlResponseSecurity(XmlResponseSecurity data) {
        this.data = data;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        // GET THE DOCUMENT
        Document soapmsg = null;
        try {
            soapmsg = SoapUtil.getDocument(response);
        } catch (SAXException e) {
            String msg = "cannot get an xml document from the response to sign";
            logger.severe(msg);
            throw new PolicyAssertionException(msg, e);
        }
        if (soapmsg == null) {
            String msg = "cannot get an xml document from the response to sign";
            logger.severe(msg);
            throw new PolicyAssertionException(msg);
        }
        HttpTransportMetadata meta = (HttpTransportMetadata)request.getTransportMetadata();

        // DECORATE WITH NONCE IN A WSSC TOKEN
        String nonceValue = meta.getRequest().getHeader(XmlResponseSecurity.XML_NONCE_HEADER_NAME);
        // (this is optional)
        if (nonceValue != null && nonceValue.length() > 0) {
            SecureConversationTokenHandler.appendNonceToDocument(soapmsg, Long.parseLong(nonceValue));
        }

        // ENCRYPTION (optional)
        if (data.isEncryption()) {
            // RETREIVE SESSION ID
            // get the header containing the xml session id
            String sessionIDHeaderValue = meta.getRequest().getHeader(XmlResponseSecurity.XML_SESSID_HEADER_NAME);
            if (sessionIDHeaderValue == null || sessionIDHeaderValue.length() < 1) {
                String msg = "Could not encrypt response because xml session id was not provided by requestor.";
                logger.severe(msg);
                throw new PolicyAssertionException(msg);
            }
            // retrieve the session
            Session xmlsession = null;
            try {
                xmlsession = SessionManager.getInstance().getSession(Long.parseLong(sessionIDHeaderValue));
            } catch (SessionNotFoundException e) {
                String msg = "Exception finding session with id=" + sessionIDHeaderValue;
                logger.log(Level.SEVERE, msg, e);
                throw new PolicyAssertionException(msg, e);
            } catch (NumberFormatException e) {
                String msg = "Session id is not long value : " + sessionIDHeaderValue;
                logger.log(Level.SEVERE, msg, e);
                throw new PolicyAssertionException(msg, e);
            }

            // encrypt the message
            try {
                XmlMangler.encryptXml(soapmsg, xmlsession.getKeyRes(), sessionIDHeaderValue);
            } catch (GeneralSecurityException e) {
                String msg = "Exception trying to encrypt response";
                logger.log(Level.SEVERE, msg, e);
                throw new PolicyAssertionException(msg, e);
            } catch (IOException e) {
                String msg = "Exception trying to encrypt response";
                logger.log(Level.SEVERE, msg, e);
                throw new PolicyAssertionException(msg, e);
            } catch (IllegalArgumentException e) {
                String msg = "Exception trying to encrypt response";
                logger.log(Level.SEVERE, msg, e);
                throw new PolicyAssertionException(msg, e);
            }

            logger.info("Response document was encrypted.");
        }

        // GET THE SIGNING CERT
        X509Certificate serverCert = null;
        byte[] buf = KeystoreUtils.getInstance().readSSLCert();
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        try {
            serverCert = (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(bais);
        } catch (CertificateException e) {
            String msg = "cannot generate cert from cert file";
            logger.severe(msg);
            throw new PolicyAssertionException(msg);
        }

        // GET THE SIGNING KEY
        PrivateKey serverPrivateKey = null;
        try {
            serverPrivateKey = KeystoreUtils.getInstance().getSSLPrivateKey();
        } catch (KeyStoreException e) {
            String msg = "cannot get ssl private key";
            logger.severe(msg);
            throw new PolicyAssertionException(msg);
        }

        // XML SIGNATURE
        SoapMsgSigner dsigHelper = new SoapMsgSigner();
        try {
            dsigHelper.signEnvelope(soapmsg, serverPrivateKey, serverCert);
        } catch (SignatureStructureException e) {
            String msg = "error signing response";
            logger.log(Level.SEVERE, msg, e);
            throw new PolicyAssertionException(msg, e);
        } catch (XSignatureException e) {
            String msg = "error signing response";
            logger.log(Level.SEVERE, msg, e);
            throw new PolicyAssertionException(msg, e);
        }
        logger.info("Response document signed successfully");
        return AssertionStatus.NONE;
    }

    private Logger logger = LogManager.getInstance().getSystemLogger();
    private XmlResponseSecurity data = null;
}
