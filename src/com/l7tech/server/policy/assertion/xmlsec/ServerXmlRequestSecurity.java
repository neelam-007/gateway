package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.server.policy.assertion.credential.wss.ServerWssCredentialSource;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.xmlsig.SoapMsgSigner;
import com.l7tech.xmlsig.SignatureNotFoundException;
import com.l7tech.xmlsig.InvalidSignatureException;
import com.l7tech.identity.User;
import com.ibm.xml.dsig.XSignatureException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.cert.X509Certificate;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import sun.security.x509.X500Name;

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
public class ServerXmlRequestSecurity extends ServerWssCredentialSource implements ServerAssertion {
    public ServerXmlRequestSecurity(XmlRequestSecurity data ) {
        super(null);
        _data = data;
        logger = LogManager.getInstance().getSystemLogger();
    }

    /**
     * validates the signature and
     * @param request
     * @param response
     * @return
     * @throws CredentialFinderException
     */
    protected AssertionStatus checkCredentials(Request request, Response response) throws CredentialFinderException {
        // nothing to check, if the credentials were extracted successfully, then there is nothing to check
        return AssertionStatus.NONE;
    }

    /**
     *
     * @param request
     * @return
     * @throws IOException
     * @throws CredentialFinderException
     */
    protected PrincipalCredentials findCredentials(Request request, Response response) throws IOException, CredentialFinderException {

        // todo, xml-enc part

        // try to get credentials out of the digital signature
        Document soapmsg = null;
        try {
            soapmsg = ((SoapRequest)request).getDocument();
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "could not get request's xml document", e);
            throw new CredentialFinderException("cannot extract name from cert", e, AssertionStatus.AUTH_FAILED);
        }
        SoapMsgSigner dsigHelper = new SoapMsgSigner();
        X509Certificate clientCert = null;
        try {
            clientCert = dsigHelper.validateSignature(soapmsg);
        } catch (SignatureNotFoundException e) {
            // no digital signature, return null
            logger.log(Level.WARNING, e.getMessage(), e);
            logger.info(((SoapRequest)request).getRequestXml());
            throw new CredentialFinderException(e.getMessage(), e, AssertionStatus.AUTH_REQUIRED);
        } catch (InvalidSignatureException e) {
            // bad signature !
            logger.log(Level.SEVERE, e.getMessage(), e);
            logger.info(((SoapRequest)request).getRequestXml());
            throw new CredentialFinderException(e.getMessage(), e, AssertionStatus.AUTH_FAILED);
        } catch (XSignatureException e) {
            throw new CredentialFinderException(e.getMessage(), e, AssertionStatus.AUTH_FAILED);
        }

        // Get DN from cert, ie "CN=testuser, OU=ssg.example.com"
        // String certCN = clientCert.getSubjectDN().getName();
        // fla changed this to:
        String certCN = null;
        try {
            X500Name x500name = new X500Name( clientCert.getSubjectX500Principal().getName() );
            certCN = x500name.getCommonName();
        } catch (IOException e) {
            _log.log(Level.SEVERE, e.getMessage(), e);
            throw new CredentialFinderException("cannot extract name from cert", e, AssertionStatus.AUTH_FAILED);
        }

        logger.log(Level.INFO, "cert extracted from digital signature for user " + certCN);

        User u = new User();
        u.setLogin(certCN);
        return new PrincipalCredentials( u, null, CredentialFormat.CLIENTCERT, null, clientCert );
    }

    protected XmlRequestSecurity _data;
    private Logger logger = null;
}
