/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.wss.WssClientCert;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.xmlsig.SoapMsgSigner;
import com.l7tech.xmlsig.SignatureNotFoundException;
import com.l7tech.xmlsig.InvalidSignatureException;
import com.l7tech.logging.LogManager;
import com.l7tech.identity.User;
import com.ibm.xml.dsig.XSignatureException;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import sun.security.x509.X500Name;

/**
 * This assertion verifies that the soap envelope of a request is digitally signed by a user.
 * The signature is validated and the X509 cert is extracted in and wrapped in a PrincipalCredentials
 * object.
 *
 * @author alex, flascell
 */
public class ServerWssClientCert extends ServerWssCredentialSource implements ServerAssertion {
    public ServerWssClientCert( WssClientCert data ) {
        super(data);
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

    protected WssClientCert _data;
    private Logger logger = null;
}
