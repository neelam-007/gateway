package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.XmlResponseSecurity;
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
import com.l7tech.common.security.AesKey;
import com.l7tech.xmlenc.Session;
import com.l7tech.xmlenc.XmlMangler;
import com.l7tech.xmlsig.InvalidSignatureException;
import com.l7tech.xmlsig.SecureConversationTokenHandler;
import com.l7tech.xmlsig.SignatureNotFoundException;
import com.l7tech.xmlsig.SoapMsgSigner;
import com.l7tech.xmlsig.XMLSecurityElementNotFoundException;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * User: flascell
 * Date: Aug 26, 2003
 * Time: 2:54:01 PM
 * $Id$
 *
 * XML Digital signature on the soap response sent from the ssg server to the requestor (probably proxy). May also enforce
 * xml encryption for the body element of the response.
 *
 * On the server side, this decorates a response with an xml d-sig and maybe xml-enc the response's body
 * On the proxy side, this verifies that the Soap Response contains a valid xml d-sig for the entire envelope and decrypts
 * the response's body if necessary.
 *
 * @author flascell
 */
public class ClientXmlResponseSecurity extends ClientAssertion {

    public ClientXmlResponseSecurity(XmlResponseSecurity data) {
        this.data = data;
    }

    /**
     * If this assertion includes xml-enc, the proxy will add a header to the request that tells the server
     * which xml-enc session to use.
     *
     * @param request might receive a header containing the xml-enc session
     * @return AssertionStatus.NONE if we are prepared to handle the eventual response
     * @throws PolicyAssertionException if we had trouble establishing a session
     * @throws ServerCertificateUntrustedException if an updated SSG cert is needed
     * @throws OperationCanceledException if the user cancels the logon dialog
     * @throws BadCredentialsException if the SSG rejects the SSG username/password when establishing the session
     */
    public AssertionStatus decorateRequest(PendingRequest request)
            throws PolicyAssertionException, ServerCertificateUntrustedException,
                   OperationCanceledException, BadCredentialsException
    {
        Ssg ssg = request.getSsg();

        // We'll need to know the server cert in order to check the signature
        if (SsgKeyStoreManager.getServerCert(ssg) == null)
            throw new ServerCertificateUntrustedException("Server cert is needed to check signatures, but has not yet been discovered");

        log.info("According to policy, we're expecting a signed reply.  Will send nonce.");
        request.setNonceRequired(true);

        // If the response will be encrypted, we'll need to ensure that there's a session open
        if (data.isEncryption()) {
            log.info("According to policy, we're expecting an encrypted reply.  Verifying session.");
            try {
                request.setSession(SsgSessionManager.getOrCreateSession(ssg));
            } catch (IOException e) {
                throw new PolicyAssertionException("Unable to establish session with Gateway", e);
            }
        }

        return AssertionStatus.NONE;
    }

    /**
     * validate the signature of the response by the ssg server
     * @param request
     * @param response
     * @return
     * @throws PolicyAssertionException
     */
    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException, ServerCertificateUntrustedException {
        Document doc;
        try {
            doc = response.getResponseAsDocument();
        } catch (IOException e) {
            throw new PolicyAssertionException("Problem while reading response from SSG", e);
        } catch (SAXException e) {
            throw new PolicyAssertionException("SSG sent invalid XML", e);
        }

        // LOOK FOR NONCE IN WSSC TOKEN
        try {
            long responsenonce = SecureConversationTokenHandler.readNonceFromDocument(doc);
            if (responsenonce != request.getNonce())
                throw new PolicyAssertionException("Response from SSG contained the wrong nonce value");
        } catch (XMLSecurityElementNotFoundException e) {
            // if the nonce is not there, we should note that this is subject to repeat attack
            throw new PolicyAssertionException("Response from SSG did not contain a nonce", e);
        }

        // VERIFY SIGNATURE OF ENVELOPE
        SoapMsgSigner dsigHelper = new SoapMsgSigner();

        X509Certificate serverCert = null;
        X509Certificate caCert = SsgKeyStoreManager.getServerCert(request.getSsg());
        if (caCert == null)
            throw new IllegalStateException("Request processing failed to verify presence of server cert");  // can't happen

        try {
            // verify that this cert is signed with the root cert of this ssg
            serverCert = dsigHelper.validateSignature(doc);
            serverCert.verify(caCert.getPublicKey());
        } catch (SignatureNotFoundException e) {
            throw new PolicyAssertionException("Response from SSG did not contain a signature as required by policy", e);
        } catch (InvalidSignatureException e) {
            throw new PolicyAssertionException("Response from SSG contained an invalid signature", e);
        } catch (SignatureException e) {
            throw new PolicyAssertionException("Response from SSG was signed, but not by the SSG CA key we expected", e);
        } catch (CertificateException e) {
            throw new PolicyAssertionException("Signature on response from SSG contained an invalid certificate", e);
        } catch (NoSuchAlgorithmException e) {
            throw new PolicyAssertionException("Signature on response from SSG required an unsupported algorithm", e);
        } catch (InvalidKeyException e) {
            throw new PolicyAssertionException("Our copy of the SSG public key is corrupt", e); // can't happen
        } catch (NoSuchProviderException e) {
            throw new PolicyAssertionException("VM is misconfigured", e); // can't happen
        }

        // must we also decrypt the body?
        if (data.isEncryption()) {
            Session session = request.getSession();
            if (session == null)
                // can't happen; a reference to the session is saved in request during decorateRequest()
                throw new IllegalStateException("PendingRequest session is null");

            try {
                Key keyres = new AesKey(session.getKeyRes());
                XmlMangler.decryptXml(doc, keyres);
            } catch (GeneralSecurityException e) {
                throw new PolicyAssertionException("failure decrypting document", e);
            } catch (ParserConfigurationException e) {
                throw new PolicyAssertionException("failure decrypting document", e);
            } catch (IOException e) {
                throw new PolicyAssertionException("failure decrypting document", e);
            } catch (SAXException e) {
                throw new PolicyAssertionException("failure decrypting document", e);
            } catch (XMLSecurityElementNotFoundException e) {
                throw new PolicyAssertionException("failure decrypting document", e);
            }
            log.info("message decrypted");
        }

        return AssertionStatus.NONE;
    }

    private XmlResponseSecurity data = null;
    private static final Category log = Category.getInstance(ClientHttpClientCert.class);
}
