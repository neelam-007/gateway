package com.l7tech.server.policy.assertion.xmlsec;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.common.security.xml.*;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.XmlResponse;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ElementSecurity;
import com.l7tech.policy.assertion.xmlsec.XmlResponseSecurity;
import com.l7tech.server.SessionManager;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.ServerSoapUtil;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * XML Digital signature on the soap response sent from the ssg server to the requestor (probably proxy). Also does
 * XML Encryption of the response's body if the assertion's property dictates it.
 * <p/>
 * On the server side, this decorates a response with an xml d-sig and maybe signs the body.
 * On the proxy side, this verifies that the Soap Response contains a valid xml d-sig for the entire envelope and maybe
 * decyphers the body.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ServerXmlResponseSecurity implements ServerAssertion {

    public ServerXmlResponseSecurity(XmlResponseSecurity data) {
        this.data = data.getElements();
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

        String nonceValue = (String)request.getParameter(Request.PARAM_HTTP_XML_NONCE);

        // (this is optional)
        if (nonceValue != null && nonceValue.length() > 0) {
            SecureConversationTokenHandler.appendNonceToDocument(soapmsg, Long.parseLong(nonceValue));
        } else {
            logger.finest("request did not include a nonce value to use for response's signature");
        }

        String encReferenceId = "encref";
        int encReferenceSuffix = 1;
        String signReferenceId = "signref";
        int signReferenceSuffix = 1;

        for (int i = 0; i < data.length; i++) {
            ElementSecurity elementSecurity = data[i];
            // XPath match?
            List nodes = null;
            try {
                XpathExpression xpath = elementSecurity.getXpathExpression();
                nodes = XpathEvaluator.newEvaluator(soapmsg, xpath.getNamespaces()).select(xpath.getExpression());
            } catch (JaxenException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                return AssertionStatus.FALSIFIED;
            }

            if (nodes.isEmpty()) continue; // nothing selected
            Object o = nodes.get(0);
            if (!(o instanceof Element)) {
                logger.log(Level.SEVERE, "Unexpected type returned by XPath expression " + o.getClass());
                return AssertionStatus.FALSIFIED;
            }
            Element element = (Element)o;

            if (elementSecurity.isEncryption()) {
                checkEncryptionProperties(elementSecurity);
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
                        response.setParameter(Response.PARAM_HTTP_SESSION_STATUS, "invalid");
                        logger.log(Level.SEVERE, msg, e);
                        return AssertionStatus.FALSIFIED;
                    } catch (NumberFormatException e) {
                        String msg = "Session id is not long value : " + sessionIDHeaderValue;
                        response.setParameter(Response.PARAM_HTTP_SESSION_STATUS, "invalid");
                        logger.log(Level.SEVERE, msg, e);
                        return AssertionStatus.FALSIFIED;
                    }
                }

                // encrypt the message
                try {
                    String id = encReferenceId + encReferenceSuffix;
                    XmlMangler.encryptXml(element, xmlsession.getKeyRes(), Long.toString(xmlsession.getId()), id);
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

                logger.fine("Response document was encrypted.");
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

            SoapMsgSigner dsigHelper = new SoapMsgSigner();
            try {
                String id = signReferenceId + signReferenceSuffix;
                dsigHelper.signElement(soapmsg, element, id, serverPrivateKey, serverCert);
            } catch (SignatureStructureException e) {
                String msg = "error signing response";
                logger.log(Level.SEVERE, msg, e);
                return AssertionStatus.FALSIFIED;
            } catch (XSignatureException e) {
                String msg = "error signing response";
                logger.log(Level.SEVERE, msg, e);
                return AssertionStatus.FALSIFIED;
            }
            logger.fine("Response document signed successfully");
        }
        ((XmlResponse)response).setDocument(soapmsg);

        return AssertionStatus.NONE;
    }

    /**
     * Check whether the encryption properties are supported
     *
     * @param elementSecurity the security element specifying the security properties
     */
    private static void checkEncryptionProperties(ElementSecurity elementSecurity)
      throws PolicyAssertionException {
        if (!"AES".equals(elementSecurity.getCipher()))
            throw new PolicyAssertionException("Unable to decrypt request: unsupported cipher: " + elementSecurity.getCipher());
        if (128 != elementSecurity.getKeyLength())
            throw new PolicyAssertionException("Unable to decrypt request: unsupported key length: " + elementSecurity.getKeyLength());
    }

    private Logger logger = LogManager.getInstance().getSystemLogger();
    private ElementSecurity[] data = null;
}
