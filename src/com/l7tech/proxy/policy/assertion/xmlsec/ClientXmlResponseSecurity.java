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
            if (!result.isPreconditionMatched())
                return AssertionStatus.NONE;

            Long responsenonce = SecureConversationTokenHandler.readNonceFromDocument(doc);
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


    /**
     * validate the signature of the response by the ssg server (do not use, this is old version for reference)
     *
     * @param request
     * @param response
     * @return
     */
    private AssertionStatus unDecorateReplyOld(PendingRequest request, SsgResponse response) throws ServerCertificateUntrustedException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException {
        Document doc = response.getResponseAsDocument();

        // LOOK FOR NONCE IN WSSC TOKEN
        Long responsenonce = SecureConversationTokenHandler.readNonceFromDocument(doc);
        if (responsenonce == null)
            throw new ResponseValidationException("Response from Gateway did not contain a nonce");

        if (responsenonce.longValue() != request.getNonce())
            throw new ResponseValidationException("Response from Gateway contained the wrong nonce value");

        ElementSecurity[] elements = xmlResponseSecurity.getElements();
        for (int i = 0; i < elements.length; i++) {
            ElementSecurity elementSecurity = elements[i];

            try {
                // XPath match?
                XpathExpression xpath = elementSecurity.getElementXpath();
                List nodes = XpathEvaluator.newEvaluator(doc, xpath.getNamespaces()).select(xpath.getExpression());
                if (nodes.isEmpty()) continue; // nothing selected
                Object o = nodes.get(0);
                if (!(o instanceof Element)) {
                    throw new ResponseValidationException("Unexpected type returned by XPath expression " + o.getClass());
                }
                Element element = (Element)o;
                // verifiy element signature
                X509Certificate[] serverCertChain = null;
                X509Certificate caCert = SsgKeyStoreManager.getServerCert(request.getSsg());

                if (caCert == null)
                    throw new IllegalStateException("Request processing failed to verify presence of server cert");  // can't happen

                try {
                    // verify that this cert is signed with the root cert of this ssg
                    serverCertChain = SoapMsgSigner.validateSignature(doc, element);
                    serverCertChain[0].verify(caCert.getPublicKey());
                } catch (SignatureNotFoundException e) {
                    throw new ResponseValidationException("Response from Gateway did not contain a signature as required by policy", e);
                } catch (InvalidSignatureException e) {
                    throw new ResponseValidationException("Response from Gateway contained an invalid signature", e);
                } catch (SignatureException e) {
                    throw new ResponseValidationException("Response from Gateway was signed, but not by the Gateway CA key we expected", e);
                } catch (CertificateException e) {
                    throw new ResponseValidationException("Signature on response from Gateway contained an invalid certificate", e);
                } catch (NoSuchAlgorithmException e) {
                    throw new ResponseValidationException("Signature on response from Gateway required an unsupported algorithm", e);
                } catch (InvalidKeyException e) {
                    throw new ResponseValidationException("Our copy of the Gateway public key is corrupt", e); // can't happen
                } catch (NoSuchProviderException e) {
                    throw new RuntimeException("VM is misconfigured", e);
                }
                log.info("signature of response message verified");


                if (elementSecurity.isEncryption()) { //element security is required
                    checkEncryptionProperties(elementSecurity);
                    Session session = request.getSession();
                    if (session == null) {
                        // can't happen; a reference to the session is saved in request during decorateRequest()
                        throw new IllegalStateException("PendingRequest session is null");
                    }
                    Key keyres = new AesKey(session.getKeyRes(), 128);
                    XmlMangler.decryptXml(doc, keyres);
                }
            } catch (GeneralSecurityException e) {
                throw new ResponseValidationException("failure decrypting document", e);
            } catch (ParserConfigurationException e) {
                throw new RuntimeException("failure decrypting document", e); // can't happen
            } catch (XMLSecurityElementNotFoundException e) {
                throw new ResponseValidationException("failure decrypting document", e);
            } catch (JaxenException e) {
                throw new ResponseValidationException("failure on XPath select", e);
            }
            log.info("response message element decrypted");
        }

        // clean empty security element and header if necessary
        SoapUtil.cleanEmptySecurityElement(doc);
        SoapUtil.cleanEmptyHeaderElement(doc);

        response.setResponse(doc);

        return AssertionStatus.NONE;
    }

    /**
     * Check whether the encryption properties are supported
     *
     * @param elementSecurity the security element specifying the security properties
     * @throws ResponseValidationException on unsupported properties
     */
    private static void checkEncryptionProperties(ElementSecurity elementSecurity) throws ResponseValidationException {
        if (!"AES".equals(elementSecurity.getCipher()))
            throw new ResponseValidationException("Unable to decrypt response: unsupported cipher: " + elementSecurity.getCipher());
        if (128 != elementSecurity.getKeyLength())
            throw new ResponseValidationException("Unable to decrypt response: unsupported key length: " + elementSecurity.getKeyLength());
    }

    public String getName() {
        return "XML Response Security - " + (xmlResponseSecurity.hasEncryptionElement() ? "sign and encrypt" : "sign only");
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }


    private XmlResponseSecurity xmlResponseSecurity;
}
