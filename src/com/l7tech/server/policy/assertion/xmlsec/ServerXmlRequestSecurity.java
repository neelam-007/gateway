package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.HttpTransportMetadata;
import com.l7tech.xmlsig.*;
import com.l7tech.identity.User;
import com.l7tech.xmlenc.*;
import com.ibm.xml.dsig.XSignatureException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.cert.X509Certificate;
import java.security.GeneralSecurityException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import sun.security.x509.X500Name;

import javax.xml.parsers.ParserConfigurationException;

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
        } catch (PolicyAssertionException e) {
            response.setAuthenticationMissing(true);
            response.setPolicyViolated(true);
            throw e;
        }

        // check validity of the session
        if (!checkSeqNrValidity(soapmsg, xmlsecSession)) {
            // when the session is no longer valid we must inform the client proxy so that he generates another session
            HttpTransportMetadata metadata = (HttpTransportMetadata)response.getTransportMetadata();
            metadata.getResponse().addHeader(XmlRequestSecurity.SESSION_STATUS_HTTP_HEADER, "invalid");
            // todo, control what the soap fault look like
            // in this case the policy is not violated
            // response.setPolicyViolated(true);
            return AssertionStatus.FALSIFIED;
        }

        // increment the seq number so that it is not used again
        xmlsecSession.incrementRequestsUsed();

        // validate signature
        X509Certificate cert;
        try {
            cert = validateSignature(soapmsg);
        } catch (SignatureNotFoundException e) {
            // no digital signature, return null
            response.setAuthenticationMissing(true);
            response.setPolicyViolated(true);
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new PolicyAssertionException(e.getMessage(), e);
        } catch (InvalidSignatureException e) {
            // bad signature !
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new PolicyAssertionException(e.getMessage(), e);
        }

        // upload cert as credentials
        String certCN = null;
        try {
            X500Name x500name = new X500Name(cert.getSubjectX500Principal().getName());
            certCN = x500name.getCommonName();
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new PolicyAssertionException("cannot extract name from cert", e);
        }
        logger.log(Level.INFO, "cert extracted from digital signature for user " + certCN);
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
                response.setPolicyViolated(true);
                throw new PolicyAssertionException(msg, e);
            } catch (ParserConfigurationException e) {
                String msg = "Error decrypting request";
                logger.log(Level.SEVERE, msg, e);
                response.setPolicyViolated(true);
                throw new PolicyAssertionException(msg, e);
            } catch (IOException e) {
                String msg = "Error decrypting request";
                logger.log(Level.SEVERE, msg, e);
                response.setPolicyViolated(true);
                throw new PolicyAssertionException(msg, e);
            } catch (SAXException e) {
                String msg = "Error decrypting request";
                logger.log(Level.SEVERE, msg, e);
                response.setPolicyViolated(true);
                throw new PolicyAssertionException(msg, e);
            } catch (Throwable e) {
                String msg = "Unhandled exception from mangler: ";
                logger.log(Level.SEVERE, msg, e);
                response.setPolicyViolated(true);
                throw new PolicyAssertionException(msg, e);
            }
            logger.info("Decrypted request successfully.");
        }

        return AssertionStatus.NONE;
    }

    private X509Certificate validateSignature(Document soapmsg) throws SignatureNotFoundException, InvalidSignatureException {
        SoapMsgSigner dsigHelper = new SoapMsgSigner();
        X509Certificate clientCert = null;
        clientCert = dsigHelper.validateSignature(soapmsg);
        return clientCert;
    }

    private boolean checkSeqNrValidity(Document soapmsg, Session session) {
        long seqNr;
        try {
            seqNr = SecureConversationTokenHandler.readSeqNrFromDocument(soapmsg);
        } catch (XMLSecurityElementNotFoundException e) {
            logger.severe("request contains no sequence number");
            return false;
        }
        if (seqNr < session.getHighestSeq()) {
            logger.severe("sequence number too low (" + seqNr + "). someone is trying replay attack?");
            return false;
        }
        return true;
    }

    private Session getXmlSecSession(Document soapmsg) throws PolicyAssertionException {
        // get the session id from the security context
        long sessionID = 0;
        try {
            sessionID = SecureConversationTokenHandler.readSessIdFromDocument(soapmsg);
        } catch (XMLSecurityElementNotFoundException e) {
            String msg = "could not extract session id from msg.";
            logger.log(Level.SEVERE, msg, e);
            throw new PolicyAssertionException(msg);
        }

        // retrieve the session
        Session xmlsession = null;
        try {
            xmlsession = SessionManager.getInstance().getSession(sessionID);
        } catch (SessionNotFoundException e) {
            String msg = "Exception finding session with id=" + sessionID + ". session could reside on other cluster member or is no longer valid.";
            logger.log(Level.SEVERE, msg, e);
            throw new PolicyAssertionException(msg, e);
        } catch (NumberFormatException e) {
            String msg = "Session id is not long value : " + sessionID;
            logger.log(Level.SEVERE, msg, e);
            throw new PolicyAssertionException(msg, e);
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
