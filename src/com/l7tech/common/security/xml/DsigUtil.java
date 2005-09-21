/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.ibm.xml.dsig.*;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.logging.Logger;

/**
 * Utility class to help with XML digital signatures.
 */
public class DsigUtil {
    private static final Logger logger = Logger.getLogger(DsigUtil.class.getName());

    /**
     * Digitally sign the specified element, using the specified key and including the specified cert inline
     * in the KeyInfo.
     *
     * @param elementToSign         the element to sign
     * @param senderSigningCert     certificate to sign it with.  will be included inline in keyinfo
     * @param senderSigningKey      private key to sign it with.
     * @param useKeyInfoTumbprint   if true, KeyInfo will use SecurityTokenReference/KeyId with cert thumbprint.
     *                              if false, KeyInfo will use X509Data containing a b64 copy of the entire cert.
     * @param keyName               if specified, KeyInfo will use a keyName instead of an STR or a literal cert.
     * @return the new dsig:Signature element, as a standalone element not yet attached into the document.
     * @throws SignatureException   if there is a problem creating the signature
     * @throws SignatureStructureException if there is a problem creating the signature
     * @throws XSignatureException  if there is a problem creating the signature
     * @throws CertificateEncodingException if senderSigningCert is no good
     */
    public static Element createEnvelopedSignature(Element elementToSign,
                                                   X509Certificate senderSigningCert,
                                                   PrivateKey senderSigningKey,
                                                   boolean useKeyInfoTumbprint,
                                                   String keyName)
            throws SignatureException, SignatureStructureException, XSignatureException, CertificateEncodingException
    {
        String signaturemethod = null;
        if (senderSigningKey instanceof RSAPrivateKey)
            signaturemethod = SignatureMethod.RSA;
        else if (senderSigningKey instanceof DSAPrivateKey)
            signaturemethod = SignatureMethod.DSA;
        else if (senderSigningKey instanceof SecretKey)
            signaturemethod = SignatureMethod.HMAC;
        else {
            throw new SignatureException("PrivateKey type not supported " +
                                               senderSigningKey.getClass().getName());
        }

        // Create signature template and populate with appropriate transforms. Reference is to SOAP Envelope
        TemplateGenerator template = new TemplateGenerator(elementToSign.getOwnerDocument(),
                                                           XSignature.SHA1, Canonicalizer.EXCLUSIVE, signaturemethod);
        template.setIndentation(false);
        template.setPrefix("ds");

        // Add enveloped signature of entire document
        final Element root = elementToSign;
        String idAttr = "Id";
        if ("Assertion".equals(root.getLocalName()) &&
                root.getNamespaceURI() != null &&
                root.getNamespaceURI().indexOf("urn:oasis:names:tc:SAML") == 0)
            idAttr = "AssertionID";
        String rootId = root.getAttribute(idAttr);
        if (rootId == null || rootId.length() < 1) {
            rootId = "root";
            root.setAttribute(idAttr, rootId);
        }
        Reference rootRef = template.createReference("#" + rootId);
        rootRef.addTransform(Transform.ENVELOPED);
        rootRef.addTransform(Transform.C14N_EXCLUSIVE);
        template.addReference(rootRef);

        // Get the signature element
        Element sigElement = template.getSignatureElement();

        // Include KeyInfo element in signature and embed cert into subordinate X509Data element

        KeyInfo keyInfo = new KeyInfo();
        if (keyName != null && keyName.length() > 0) {
            keyInfo.setKeyNames(new String[] { keyName });
        } else if (useKeyInfoTumbprint) {
            final Document factory = elementToSign.getOwnerDocument();
            final String wsseNs = SoapUtil.SECURITY_NAMESPACE;
            Element str = factory.createElementNS(wsseNs, "wsse:SecurityTokenReference");
            str.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns:wsse", wsseNs);
            Element keyId = XmlUtil.createAndAppendElementNS(str, "KeyIdentifier", wsseNs, "wsse");
            keyId.setAttribute("ValueType", SoapUtil.VALUETYPE_X509_THUMB_SHA1);
            XmlUtil.setTextContent(keyId, CertUtils.getThumbprintSHA1(senderSigningCert));
            keyInfo.setUnknownChildren(new Element[] { str });
        } else {
            //keyInfo.setKeyValue(senderSigningCert.getPublicKey());
            KeyInfo.X509Data x5data = new KeyInfo.X509Data();
            x5data.setCertificate(senderSigningCert);
            x5data.setParameters(senderSigningCert, false, false, false);
            keyInfo.setX509Data(new KeyInfo.X509Data[] { x5data });
        }
        keyInfo.insertTo(sigElement);

        SignatureContext sigContext = new SignatureContext();
        final String finalRootId = rootId;
        sigContext.setIDResolver(new IDResolver() {
            public Element resolveID(Document document, String s) {
                return s.equals(finalRootId) ? root : null;
            }
        });
        sigContext.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws IOException {
                throw new FileNotFoundException("No external ref should have been present");
            }
        });
        sigContext.setResourceShower(new ResourceShower() {
            public void showSignedResource(Element element, int i, String s, String s1, byte[] bytes, String s2) {

            }
        });
        Element signedSig = sigContext.sign(sigElement, senderSigningKey);
        return signedSig;
    }

    /**
     * Add a c14n:InclusiveNamespaces child element to the specified element with an empty PrefixList.
     */
    public static void addInclusiveNamespacesToElement(Element element) {
        Element inclusiveNamespaces = XmlUtil.createAndAppendElementNS(element,
            "InclusiveNamespaces",
            Transform.C14N_EXCLUSIVE,
            "c14n");
        inclusiveNamespaces.setAttribute("PrefixList", "");
    }

    /**
     * Check a simple signature for validity.  A signature is sufficiently "simple" to be checked by this method
     * if all of the following is true: <br>
     *  - it does not use the STR-Transform; <br>
     *  - all ID references can be resolved by looking for wsu:Id, saml:Assertion, or Id attributes; <br>
     *  - caller has no special needs (such as keeping track of signed elements, the last signature value seen,
     *                                 or other complex behaviour needed by [for example] WssProcessorImpl)<br>
     *
     * @param sigElement
     * @param certificateResolver
     * @throws SignatureException
     */
    public static void checkSimpleSignature(Element sigElement, CertificateResolver certificateResolver) throws SignatureException {
        Element keyInfoElement = KeyInfo.searchForKeyInfo(sigElement);
        if (keyInfoElement == null) throw new SignatureException("No KeyInfo found in signature");

        final X509Certificate signingCert;

        try {
            KeyInfoElement parsedKeyInfo = KeyInfoElement.parse(keyInfoElement, certificateResolver);
            signingCert = parsedKeyInfo.getCertificate();
            if (signingCert == null) throw new SignatureException("Unable to resolve signing cert");
        } catch (SAXException e) {
            throw new SignatureException(e);
        } catch (KeyInfoElement.KeyInfoElementException e) {
            throw new SignatureException(e);
        }

        PublicKey signingKey = signingCert.getPublicKey();
        if (signingKey == null) throw new SignatureException("Unable to find signing key"); // can't happen

        try {
            signingCert.checkValidity();
        } catch (CertificateExpiredException e) {
            throw new SignatureException(e);
        } catch (CertificateNotYetValidException e) {
            throw new SignatureException(e);
        }

        SignatureContext sigContext = new SignatureContext();
        sigContext.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws IOException {
                // this works but SAXException doesn't... I guess XSS4J uses SAXException internally to signal some normal condition.
                throw new IOException("References to external resources are not permitted");
            }
        });

        sigContext.setIDResolver(new IDResolver() {
            public Element resolveID(Document doc, String s) {
                return SoapUtil.getElementByWsuId(doc, s);
            }
        });

        Validity validity = sigContext.verify(sigElement, signingKey);

        if (!validity.getCoreValidity()) {
            StringBuffer msg = new StringBuffer("Signature not valid. " + validity.getSignedInfoMessage());
            for (int i = 0; i < validity.getNumberOfReferences(); i++) {
                msg.append("\n\tElement " + validity.getReferenceURI(i) + ": " + validity.getReferenceMessage(i));
            }
            throw new SignatureException(msg.toString());
        }

        // Success.  Signature looks good.
        return;
    }
}
