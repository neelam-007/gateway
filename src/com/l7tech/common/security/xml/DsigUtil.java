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
import java.util.List;
import java.util.Iterator;

/**
 * Utility class to help with XML digital signatures.
 *
 * TODO [WS-I BSP] Check requirement that Xpath sigs use XPath Filter 2.0 Transform [ref: section 8.2.2]
 */
public class DsigUtil {
    /** @noinspection UNUSED_SYMBOL*/
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
        String signaturemethod;
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
            keyId.setAttribute("EncodingType", SoapUtil.ENCODINGTYPE_BASE64BINARY);
            keyId.setAttribute("ValueType", SoapUtil.VALUETYPE_SKI);
            XmlUtil.setTextContent(keyId, CertUtils.getSki(senderSigningCert));
            keyInfo.setUnknownChildren(new Element[] { str });
        } else {
            //keyInfo.setKeyValue(senderSigningCert.getPublicKey());
            KeyInfo.X509Data x5data = new KeyInfo.X509Data();
            x5data.setCertificate(senderSigningCert);
            x5data.setParameters(senderSigningCert, false, false, false);
            keyInfo.setX509Data(new KeyInfo.X509Data[] { x5data });
        }
        keyInfo.insertTo(sigElement, "ds", template);

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
        return sigContext.sign(sigElement, senderSigningKey);
    }

    /**
     * Add a c14n:InclusiveNamespaces child element to the specified element with an empty PrefixList.
     */
    public static void addInclusiveNamespacesToElement(Element element) {
        Element inclusiveNamespaces = XmlUtil.createAndAppendElementNS(element,
                                                                       "InclusiveNamespaces",
                                                                       Transform.C14N_EXCLUSIVE,
                                                                       "c14n");
        // omit prefix list attribute if empty (WS-I BSP R5410)
        //inclusiveNamespaces.setAttribute("PrefixList", "");
    }

    /**
     * Check a simple signature for validity.  A signature is sufficiently "simple" to be checked by this method
     * if all of the following is true: <br>
     *  - it does not use the STR-Transform; <br>
     *  - all ID references can be resolved by looking for wsu:Id, saml:Assertion, or Id attributes; <br>
     *  - caller has no special needs (such as keeping track of signed elements, the last signature value seen,
     *                                 or other complex behaviour needed by [for example] WssProcessorImpl)<br>
     *  - it is an enveloped signature that covers the root element of sigElement's owner document.
     *    it may additionally sign other things, but it will be accepted as long as it signs the root element.
     *
     * @param sigElement            the signature to check.  Must point to a non-null ds:Signature element.
     * @param securityTokenResolver   resolver for KeyInfos containing thumbprints, SKIs, or KeyNames.
     * @return                      the certificate that was used to successfully verify the signature.  Never null.
     *                              <b>NOTE: This cert will NOT necessarily be known to the securityTokenResolver --
     *                                 it may have come from the signature itself as X509Data -- so a successful
     *                                 return from this method does NOT guarantee that the signature should be
     *                                 TRUSTED, just that it was VALID.</b>
     * @throws SignatureException   if the signature could not be validated.
     */
    public static X509Certificate checkSimpleEnvelopedSignature(Element sigElement, SecurityTokenResolver securityTokenResolver) throws SignatureException {
        Element keyInfoElement = KeyInfo.searchForKeyInfo(sigElement);
        if (keyInfoElement == null) throw new SignatureException("No KeyInfo found in signature");

        final X509Certificate signingCert;

        try {
            KeyInfoElement parsedKeyInfo = KeyInfoElement.parse(keyInfoElement, securityTokenResolver);
            signingCert = parsedKeyInfo.getCertificate();
            if (signingCert == null) throw new SignatureException("Unable to resolve signing cert");
        } catch (SAXException e) {
            throw new SignatureException(e);
        } catch (KeyInfoElement.MissingResolverException e) {
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
            for (int i = 0; i < validity.getNumberOfReferences(); i++)
                msg.append("\n\tElement ")
                   .append(validity.getReferenceURI(i))
                   .append(": ")
                   .append(validity.getReferenceMessage(i));
            throw new SignatureException(msg.toString());
        }

        // Make sure the root element was covered by signature
        boolean rootWasSigned = false;
        final int numberOfReferences = validity.getNumberOfReferences();
        for (int i = 0; i < numberOfReferences; i++) {
            // Resolve each elements one by one.
            String elementId = validity.getReferenceURI(i);
            if (elementId.charAt(0) == '#')
                elementId = elementId.substring(1);
            if (elementId.equals(sigElement.getOwnerDocument().getDocumentElement().getAttribute("Id"))) {
                rootWasSigned = true;
                break;
            }
        }

        if (!rootWasSigned)
            throw new SignatureException("This signature did not cover the root of the document.");

        // Success.  Signature looks good.
        return signingCert;
    }

    /**
     * Strips all ds:Signature elements that are immediate children of the specified element.
     * Note that, if a signature is removed, the resulting DOM tree will almost certainly contain multiple
     * consecutive text nodes.
     *
     * @param parent  the element whose Signature children should be eliminated.
     */
    public static void stripSignatures(Element parent) {
        List sigs = XmlUtil.findChildElementsByName(parent, SoapUtil.DIGSIG_URI, "Signature");
        for (Iterator i = sigs.iterator(); i.hasNext();) {
            Element sigEl = (Element)i.next();
            sigEl.getParentNode().removeChild(sigEl);
        }
    }
}
