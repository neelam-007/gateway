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
import com.l7tech.xmlsig.SoapMsgSigner;
import com.l7tech.xmlsig.SignatureNotFoundException;
import com.l7tech.xmlsig.InvalidSignatureException;
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
        // try to get credentials out of the digital signature
        Document soapmsg = null;
        try {
            soapmsg = ((SoapRequest)request).getDocument();
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "could not get request's xml document", e);
            response.setAuthenticationMissing(true);
            throw new PolicyAssertionException("cannot extract name from cert", e);
        }
        SoapMsgSigner dsigHelper = new SoapMsgSigner();
        X509Certificate clientCert = null;
        try {
            clientCert = dsigHelper.validateSignature(soapmsg);
        } catch (SignatureNotFoundException e) {
            // no digital signature, return null
            logger.log(Level.WARNING, e.getMessage(), e);
            logger.info(((SoapRequest)request).getRequestXml());
            response.setAuthenticationMissing(true);
            throw new PolicyAssertionException(e.getMessage(), e);
        } catch (InvalidSignatureException e) {
            // bad signature !
            logger.log(Level.SEVERE, e.getMessage(), e);
            logger.info(((SoapRequest)request).getRequestXml());
            response.setAuthenticationMissing(true);
            throw new PolicyAssertionException(e.getMessage(), e);
        } catch (XSignatureException e) {
            response.setAuthenticationMissing(true);
            throw new PolicyAssertionException(e.getMessage(), e);
        }

        // Get DN from cert, ie "CN=testuser, OU=ssg.example.com"
        // String certCN = clientCert.getSubjectDN().getName();
        // fla changed this to:
        String certCN = null;
        try {
            X500Name x500name = new X500Name( clientCert.getSubjectX500Principal().getName() );
            certCN = x500name.getCommonName();
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            response.setAuthenticationMissing(true);
            throw new PolicyAssertionException("cannot extract name from cert", e);
        }

        logger.log(Level.INFO, "cert extracted from digital signature for user " + certCN);

        User u = new User();
        u.setLogin(certCN);
        request.setPrincipalCredentials(new PrincipalCredentials(u, null, CredentialFormat.CLIENTCERT, null, clientCert));

        // if we must also do xml-encryption,
        if (data.isEncryption()) {

            // look for the "keyname" which should contain the xml session id
            String keyname = null;
            try {
                keyname = XmlMangler.getKeyName(soapmsg);
            } catch (NullPointerException e) {
                // eat this, the following statement will catch it
            }
            if (keyname == null || keyname.length() < 1) {
                String msg = "Could not extract key info from document's header";
                logger.severe(msg);
                throw new PolicyAssertionException(msg);
            }

            // retrieve the session id
            Session xmlsession = null;
            try {
                xmlsession = SessionManager.getInstance().getSession(Long.parseLong(keyname));
            } catch (SessionNotFoundException e) {
                String msg = "Exception finding session with id=" + keyname;
                logger.log(Level.SEVERE, msg, e);
                throw new PolicyAssertionException(msg, e);
            } catch (NumberFormatException e) {
                String msg = "Session id is not long value : " + keyname;
                logger.log(Level.SEVERE, msg, e);
                throw new PolicyAssertionException(msg, e);
            }

            try {
                XmlMangler.decryptXml(soapmsg, new AesKey(xmlsession.getKeyReq()));
            } catch (GeneralSecurityException e) {
                String msg = "Error decrypting request";
                logger.log(Level.SEVERE, msg, e);
                throw new PolicyAssertionException(msg, e);
            } catch (ParserConfigurationException e) {
                String msg = "Error decrypting request";
                logger.log(Level.SEVERE, msg, e);
                throw new PolicyAssertionException(msg, e);
            } catch (IOException e) {
                String msg = "Error decrypting request";
                logger.log(Level.SEVERE, msg, e);
                throw new PolicyAssertionException(msg, e);
            } catch (SAXException e) {
                String msg = "Error decrypting request";
                logger.log(Level.SEVERE, msg, e);
                throw new PolicyAssertionException(msg, e);
            }
            logger.info("Decrypted request successfully.");
        }

        return AssertionStatus.NONE;
    }

    protected XmlRequestSecurity data;
    private Logger logger = null;


}
