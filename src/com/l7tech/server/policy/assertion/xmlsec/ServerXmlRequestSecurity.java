package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.xml.*;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.identity.User;
import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.XmlRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
import com.l7tech.server.SessionManager;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import sun.security.x509.X500Name;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 26, 2003
 * Time: 3:15:22 PM
 * $Id$
 *
 * XML Digital signature on the soap request sent from a requestor (probably proxy) to the ssg server. Also does XML
 * Encryption of the request's body if the assertion's property dictates it.
 *
 * On the server side, this must verify that the SoapRequest contains a valid xml d-sig for the entire envelope and
 * maybe decyphers the body.
 * On the proxy side, this must decorate a request with an xml d-sig and maybe encrypt the body.
 *
 * This extends ServerWssCredentialSource because once the validity of the signature if confirmed, the cert is used
 * as credentials.
 */
public class ServerXmlRequestSecurity implements ServerAssertion {
    public ServerXmlRequestSecurity(XmlRequestSecurity data ) {
        this.data = data;
        logger = LogManager.getInstance().getSystemLogger();
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        // get the document
        Document soapmsg = extractDocumentFromRequest(request);

        // get the session
        Session xmlsecSession = null;
        try {
            xmlsecSession = getXmlSecSession(soapmsg);
        } catch (SessionInvalidException e) {
            // when the session is no longer valid we must inform the client proxy so that he generates another session
            response.setParameter( Response.PARAM_HTTP_SESSION_STATUS, "invalid" );
            return AssertionStatus.FALSIFIED;
        }
        
        if (xmlsecSession == null) {
            response.setPolicyViolated(true);
            response.setAuthenticationMissing(true);
            return AssertionStatus.FALSIFIED;
        }

        // check validity of the session
        long gotSeq;
        try {
            gotSeq = checkSeqNrValidity(soapmsg, xmlsecSession);
        } catch (InvalidSequenceNumberException e) {
            // when the session is no longer valid we must inform the client proxy so that he generates another session
            response.setParameter( Response.PARAM_HTTP_SESSION_STATUS, "invalid" );
            // todo, control what the soap fault look like
            // in this case the policy is not violated
            // response.setPolicyViolated(true);
            return AssertionStatus.FALSIFIED;
        }

        // validate signature
        X509Certificate cert;
        try {
            cert = validateSignature(soapmsg);
        } catch (SignatureNotFoundException e) {
            // no digital signature, return null
            response.setAuthenticationMissing(true);
            response.setPolicyViolated(true);
            logger.log(Level.WARNING, e.getMessage(), e);
            return AssertionStatus.FALSIFIED;
        } catch (InvalidSignatureException e) {
            // bad signature !
            logger.log(Level.SEVERE, e.getMessage(), e);
            return AssertionStatus.FALSIFIED;
        }

        // Signature validated, so mark this sequence number as used up
        xmlsecSession.hitSequenceNumber(gotSeq);

        // clean the session id from the security header
        SecureConversationTokenHandler.consumeSessionInfoFromDocument(soapmsg);

        // upload cert as credentials
        String certCN = null;
        try {
            X500Name x500name = new X500Name(cert.getSubjectX500Principal().getName());
            certCN = x500name.getCommonName();
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return AssertionStatus.FALSIFIED;
        }
        logger.info("cert extracted from digital signature for user " + certCN);
        User u = new User();
        u.setLogin(certCN);
        request.setPrincipalCredentials(new PrincipalCredentials(u, null, CredentialFormat.CLIENTCERT, null, cert));

        // if we must also do xml-encryption,
        if (data.isEncryption()) {
            try {
                XmlMangler.decryptXml(soapmsg, new AesKey(xmlsecSession.getKeyReq()));
            } catch (GeneralSecurityException e) {
                String msg = "Error decrypting request";
                logger.log(Level.SEVERE, msg, e);
                return AssertionStatus.FALSIFIED;
            } catch (ParserConfigurationException e) {
                String msg = "Error decrypting request";
                logger.log(Level.SEVERE, msg, e);
                throw new PolicyAssertionException(msg, e);
            } catch (IOException e) {
                String msg = "Error decrypting request";
                logger.log(Level.SEVERE, msg, e);
                return AssertionStatus.FALSIFIED;
            } catch (SAXException e) {
                String msg = "Error decrypting request";
                logger.log(Level.SEVERE, msg, e);
                return AssertionStatus.FALSIFIED;
            } catch (XMLSecurityElementNotFoundException e) {
                String msg = "Request does not contain security element";
                logger.log(Level.SEVERE, msg, e);
                response.setPolicyViolated(true);
                return AssertionStatus.FALSIFIED;
            } catch (Throwable e) {
                String msg = "Unhandled exception from mangler: ";
                logger.log(Level.SEVERE, msg, e);
                throw new PolicyAssertionException(msg, e);
            }
            logger.info("Decrypted request successfully.");
        }

        // clean empty security element and header if necessary
        SoapUtil.cleanEmptySecurityElement(soapmsg);
        SoapUtil.cleanEmptyHeaderElement(soapmsg);

        // note, the routing should no longer use the non parsed payload
        ((XmlRequest)request).setDocument(soapmsg);

        return AssertionStatus.NONE;
    }

    private X509Certificate validateSignature(Document soapmsg) throws SignatureNotFoundException, InvalidSignatureException {
        SoapMsgSigner dsigHelper = new SoapMsgSigner();
        X509Certificate clientCert = null;
        clientCert = dsigHelper.validateSignature(soapmsg);
        return clientCert;
    }

    private static class InvalidSequenceNumberException extends Exception {
        public InvalidSequenceNumberException(String message) {
            super(message);
        }
    }

    private long checkSeqNrValidity(Document soapmsg, Session session) throws InvalidSequenceNumberException {
        long seqNr;
        try {
            seqNr = SecureConversationTokenHandler.readSeqNrFromDocument(soapmsg);
        } catch (XMLSecurityElementNotFoundException e) {
            logger.severe("request contains no sequence number");
            throw new InvalidSequenceNumberException("request contains no sequence number");
        }
        if (seqNr < session.getHighestSeq()) {
            logger.severe("sequence number too low (" + seqNr + "). someone is trying replay attack?");
            throw new InvalidSequenceNumberException("request contains a sequence number which is too low");
        }
        return seqNr;
    }

    private Session getXmlSecSession(Document soapmsg) throws SessionInvalidException {
        // get the session id from the security context
        long sessionID = 0;
        try {
            sessionID = SecureConversationTokenHandler.readSessIdFromDocument(soapmsg);
        } catch (XMLSecurityElementNotFoundException e) {
            String msg = "could not extract session id from msg.";
            logger.log(Level.WARNING, msg, e);
            return null;
        }

        // retrieve the session
        Session xmlsession = null;
        try {
            xmlsession = SessionManager.getInstance().getSession(sessionID);
        } catch (SessionNotFoundException e) {
            String msg = "Exception finding session with id=" + sessionID + ". session could reside on other cluster member or is no longer valid.";
            logger.log(Level.WARNING, msg, e);
            throw new SessionInvalidException(msg, e);
        } catch (NumberFormatException e) {
            String msg = "Session id is not long value : " + sessionID;
            logger.log(Level.SEVERE, msg, e);
            throw new SessionInvalidException(msg, e);
        }

        return xmlsession;
    }

    private Document extractDocumentFromRequest(Request req) throws PolicyAssertionException {
        // try to get credentials out of the digital signature
        Document soapmsg = null;
        try {
            soapmsg = ((SoapRequest)req).getDocument();
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "could not get request's xml document", e);
            throw new PolicyAssertionException("cannot extract name from cert", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "could not get request's xml document", e);
            throw new PolicyAssertionException("cannot extract name from cert", e);
        }

        return soapmsg;
    }

    protected XmlRequestSecurity data;
    private Logger logger = null;


}
