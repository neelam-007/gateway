package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.xml.*;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.common.security.xml.ElementSecurity;
import com.l7tech.common.security.xml.SecurityProcessor;
import com.l7tech.policy.assertion.xmlsec.XmlResponseSecurity;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpClientCert;
import com.l7tech.proxy.util.ClientLogger;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * XML Digital signature on the soap response sent from the ssg server to the requestor (probably proxy). May also enforce
 * xml encryption for the body element of the response.
 * <p/>
 * On the server side, this decorates a response with an xml d-sig and maybe xml-enc the response's body
 * On the proxy side, this verifies that the Soap Response contains a valid xml d-sig for the entire envelope and decrypts
 * the response's body if necessary.
 * <p/>
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ClientXmlResponseSecurity extends ClientAssertion {
    private static final ClientLogger log = ClientLogger.getInstance(ClientHttpClientCert.class);

    public ClientXmlResponseSecurity(XmlResponseSecurity data) {
        xmlResponseSecurity = data;
        if (data == null) {
            throw new IllegalArgumentException("security elements is null");
        }
    }

    /**
     * If this assertion includes xml-enc, the proxy will add a header to the request that tells the server
     * which xml-enc session to use.
     *
     * @param request might receive a header containing the xml-enc session
     * @return AssertionStatus.NONE if we are prepared to handle the eventual response
     * @throws ServerCertificateUntrustedException
     *                                    if an updated SSG cert is needed
     * @throws OperationCanceledException if the user cancels the logon dialog
     * @throws BadCredentialsException    if the SSG rejects the SSG username/password when establishing the session
     */
    public AssertionStatus decorateRequest(PendingRequest request)
      throws ServerCertificateUntrustedException,
      OperationCanceledException, BadCredentialsException, IOException, KeyStoreCorruptException {
        Ssg ssg = request.getSsg();

        // We'll need to know the server cert in order to check the signature
        if (SsgKeyStoreManager.getServerCert(ssg) == null)
            throw new ServerCertificateUntrustedException("Server cert is needed to check signatures, but has not yet been discovered");

        log.info("According to policy, we're expecting a signed reply.  Will send nonce.");
        request.setNonceRequired(true);

        // If the response will be encrypted, we'll need to ensure that there's a session open
        if (xmlResponseSecurity.hasEncryptionElement()) {
            log.info("According to policy, we're expecting an encrypted reply.  Verifying session.");
            request.getOrCreateSession();
        }

        return AssertionStatus.NONE;
    }

    /**
     * validate the signature of the response by the ssg server
     *
     * @param request
     * @param response
     * @return
     */
    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response)
      throws ServerCertificateUntrustedException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException {
        Document doc = response.getResponseAsDocument();

        Session session = request.getSession();
        Key decryptionKey = null;
        if (session != null)
            decryptionKey = new AesKey(session.getKeyRes(), 128);
        ElementSecurity[] elements = xmlResponseSecurity.getElements();
        SecurityProcessor verifier = SecurityProcessor.getVerifier(request.getSession(), decryptionKey, elements);
        try {
            X509Certificate caCert = SsgKeyStoreManager.getServerCert(request.getSsg());
            SecurityProcessor.Result result = verifier.processInPlace(doc);

            // If this assertion doesn't apply to this reply, we are done
            if (result.getType() == SecurityProcessor.Result.Type.NOT_APPLICABLE)
                return AssertionStatus.NONE;

            Long responsenonce = SecureConversationTokenHandler.takeNonceFromDocument(doc);
            if (responsenonce == null)
                throw new ResponseValidationException("Response from Gateway did not contain a nonce");
            if (responsenonce.longValue() != request.getNonce())
                throw new ResponseValidationException("Response from Gateway contained the wrong nonce value");

            X509Certificate[] certificate = result.getCertificateChain();
            if (certificate == null || certificate[0] == null)
                throw new ResponseValidationException("Response from gateway did not contain a certificate chain");
            certificate[0].verify(caCert.getPublicKey());
        } catch (Exception e) {
            handleResponseThrowable(e);
        }

        // clean empty security element and header if necessary
        SoapUtil.cleanEmptyRefList(doc);
        SoapUtil.cleanEmptySecurityElement(doc);
        SoapUtil.cleanEmptyHeaderElement(doc);
        response.setResponse(doc);
        return AssertionStatus.NONE;
    }

    private void handleResponseThrowable(Throwable e) throws ResponseValidationException {
        Throwable cause = ExceptionUtils.unnestToRoot(e);
        if (cause instanceof SignatureNotFoundException) {
            throw new ResponseValidationException("Response from Gateway did not contain a signature as required by policy", e);
        } else if (cause instanceof InvalidSignatureException) {
            throw new ResponseValidationException("Response from Gateway contained an invalid signature", e);
        } else if (cause instanceof SignatureException) {
            throw new ResponseValidationException("Response from Gateway was signed, but not by the Gateway CA key we expected", e);
        } else if (cause instanceof CertificateException) {
            throw new ResponseValidationException("Signature on response from Gateway contained an invalid certificate", e);
        } else if (cause instanceof NoSuchAlgorithmException) {
            throw new ResponseValidationException("Signature on response from Gateway required an unsupported algorithm", e);
        } else if (cause instanceof InvalidKeyException) {
            throw new ResponseValidationException("Our copy of the Gateway public key is corrupt", e);
        } else if (cause instanceof NoSuchProviderException) {
            throw new RuntimeException("VM is misconfigured", e);
        }
        throw new RuntimeException("Response processing error", e);
    }

    public String getName() {
        return "XML Response Security - " + (xmlResponseSecurity.hasEncryptionElement() ? "sign and encrypt" : "sign only");
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }


    private XmlResponseSecurity xmlResponseSecurity;
}
