package com.l7tech.server.policy.assertion.xmlsec;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.XmlResponse;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.XmlResponseSecurity;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.ServerSoapUtil;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.security.xml.Session;
import com.l7tech.server.SessionManager;
import com.l7tech.common.security.xml.SessionNotFoundException;
import com.l7tech.common.security.xml.XmlMangler;
import com.l7tech.common.security.xml.SecureConversationTokenHandler;
import com.l7tech.common.security.xml.SoapMsgSigner;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    /**
     * despite the name of this method, i'm actually working on the response document here
     */
    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        // GET THE DOCUMENT
        Document soapmsg = null;
        try {
            soapmsg = ServerSoapUtil.getDocument(response);
        } catch (SAXException e) {
            String msg = "cannot get an xml document from the response to sign";
            logger.severe(msg);
            return AssertionStatus.FALSIFIED;
        }
        if (soapmsg == null) {
            String msg = "cannot get an xml document from the response to sign";
            logger.severe(msg);
            return AssertionStatus.FALSIFIED;
        }

        // DECORATE WITH NONCE IN A WSSC TOKEN
        String nonceValue = (String)request.getParameter( Request.PARAM_HTTP_XML_NONCE );

        // (this is optional)
        if (nonceValue != null && nonceValue.length() > 0) {
            SecureConversationTokenHandler.appendNonceToDocument(soapmsg, Long.parseLong(nonceValue));
        }

        // ENCRYPTION (optional)
        if (data.isEncryption()) {
            // RETRIEVE SESSION
            Session xmlsession = null;
            Object sessionObj = request.getParameter(Request.PARAM_HTTP_XML_SESSID);
            if (sessionObj == null) {
                String msg = "Could not encrypt response because xml session id was not provided by requestor.";
                logger.severe(msg);
                return AssertionStatus.FALSIFIED;
            }
            // construct Session object based on the type of the context's object
            if (sessionObj instanceof Session) {
                xmlsession = (Session)sessionObj;
            } else if (sessionObj instanceof String) {
                String sessionIDHeaderValue = (String)sessionObj;
                // retrieve the session
                try {
                    xmlsession = SessionManager.getInstance().getSession(Long.parseLong(sessionIDHeaderValue));
                } catch (SessionNotFoundException e) {
                    String msg = "Exception finding session with id=" + sessionIDHeaderValue;
                    response.setParameter( Response.PARAM_HTTP_SESSION_STATUS, "invalid" );
                    logger.log(Level.SEVERE, msg, e);
                    return AssertionStatus.FALSIFIED;
                } catch (NumberFormatException e) {
                    String msg = "Session id is not long value : " + sessionIDHeaderValue;
                    response.setParameter( Response.PARAM_HTTP_SESSION_STATUS, "invalid" );
                    logger.log(Level.SEVERE, msg, e);
                    return AssertionStatus.FALSIFIED;
                }
            }

            // encrypt the message
            try {
                XmlMangler.encryptXml(soapmsg, xmlsession.getKeyRes(), Long.toString(xmlsession.getId()));
            } catch (GeneralSecurityException e) {
                String msg = "Exception trying to encrypt response";
                logger.log(Level.SEVERE, msg, e);
                return AssertionStatus.FALSIFIED;
            } catch (IOException e) {
                String msg = "Exception trying to encrypt response";
                logger.log(Level.SEVERE, msg, e);
                return AssertionStatus.FALSIFIED;
            } catch (IllegalArgumentException e) {
                String msg = "Exception trying to encrypt response";
                logger.log(Level.SEVERE, msg, e);
                return AssertionStatus.FALSIFIED;
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
            throw new IOException(msg);
        }

        // GET THE SIGNING KEY
        PrivateKey serverPrivateKey = null;
        try {
            serverPrivateKey = KeystoreUtils.getInstance().getSSLPrivateKey();
        } catch (KeyStoreException e) {
            String msg = "cannot get ssl private key";
            logger.severe(msg);
            throw new IOException(msg);
        }

        // XML SIGNATURE
        SoapMsgSigner dsigHelper = new SoapMsgSigner();
        try {
            dsigHelper.signEnvelope(soapmsg, serverPrivateKey, serverCert);
        } catch (SignatureStructureException e) {
            String msg = "error signing response";
            logger.log(Level.SEVERE, msg, e);
            return AssertionStatus.FALSIFIED;
        } catch (XSignatureException e) {
            String msg = "error signing response";
            logger.log(Level.SEVERE, msg, e);
            return AssertionStatus.FALSIFIED;
        }
        logger.info("Response document signed successfully");

        ((XmlResponse)response).setDocument(soapmsg);

        return AssertionStatus.NONE;
    }

    private Logger logger = LogManager.getInstance().getSystemLogger();
    private XmlResponseSecurity data = null;
}
