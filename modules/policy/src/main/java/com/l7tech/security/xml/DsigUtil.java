/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.xml;

import com.ibm.xml.dsig.*;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.xml.soap.SoapUtil;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.SecretKey;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class to help with XML digital signatures.
 */
public class DsigUtil {
    /** @noinspection UNUSED_SYMBOL*/
    private static final Logger logger = Logger.getLogger(DsigUtil.class.getName());

    public static final String DIGSIG_URI = "http://www.w3.org/2000/09/xmldsig#";

    /**
     * Digitally sign the specified element, using the specified key and including the specified cert inline
     * in the KeyInfo.
     *
     * @param elementToSign         the element to sign
     * @param senderSigningCert     certificate to sign it with.  will be included inline in keyinfo
     * @param senderSigningKey      private key to sign it with.
     * @param keyInfoChildElement   Custom key info child element to use
     * @param keyName               if specified, KeyInfo will use a keyName instead of an STR or a literal cert.
     * @return the new dsig:Signature element, as a standalone element not yet attached into the document.
     * @throws SignatureException   if there is a problem creating the signature
     * @throws SignatureStructureException if there is a problem creating the signature
     * @throws XSignatureException  if there is a problem creating the signature
     */
    public static Element createEnvelopedSignature(Element elementToSign,
                                                   X509Certificate senderSigningCert,
                                                   PrivateKey senderSigningKey,
                                                   Element keyInfoChildElement,
                                                   String keyName)
            throws SignatureException, SignatureStructureException, XSignatureException
    {
        /* begin: signature selection block -- taken from WssDecoratorImpl.addSignature */
        final String messageDigestAlgorithm = "SHA-1";
        SupportedSignatureMethods signaturemethod;
        if (senderSigningKey instanceof RSAPrivateKey || "RSA".equals(senderSigningKey.getAlgorithm())) {
            if ("SHA-256".equals(messageDigestAlgorithm))
                signaturemethod = SupportedSignatureMethods.RSA_SHA256;
            else
                signaturemethod = SupportedSignatureMethods.RSA_SHA1;

        } else if (senderSigningKey instanceof ECPrivateKey || "EC".equals(senderSigningKey.getAlgorithm())) {
            if ("SHA-384".equals(messageDigestAlgorithm))
                signaturemethod = SupportedSignatureMethods.ECDSA_SHA384;
            else
                signaturemethod = SupportedSignatureMethods.ECDSA_SHA256;

        } else if (senderSigningKey instanceof DSAPrivateKey) {
            signaturemethod = SupportedSignatureMethods.DSA_SHA1;
        } else if (senderSigningKey instanceof SecretKey) {
            signaturemethod = SupportedSignatureMethods.HMAC_SHA1;
        } else {
            throw new SignatureException("Private Key type not supported " +
                    senderSigningKey.getClass().getName());
        }

        // Create signature template and populate with appropriate transforms. Reference is to SOAP Envelope
        TemplateGenerator template = new TemplateGenerator(elementToSign.getOwnerDocument(),
                                                           signaturemethod.getMessageDigestIdentifier(),
                                                           Canonicalizer.EXCLUSIVE,
                                                           signaturemethod.getAlgorithmIdentifier());
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
        } else if (keyInfoChildElement != null) {
            keyInfo.setUnknownChildren(new Element[] { keyInfoChildElement });
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
        sigContext.setEntityResolver( XmlUtil.getXss4jEntityResolver());
        sigContext.setResourceShower(new ResourceShower() {
            public void showSignedResource(Element element, int i, String s, String s1, byte[] bytes, String s2) {

            }
        });
        try {
            KeyUsageChecker.requireActivityForKey(KeyUsageActivity.signXml, senderSigningCert, senderSigningKey);
        } catch (CertificateException e) {
            throw new SignatureException(e);
        }
        return sigContext.sign(sigElement, senderSigningKey);
    }

    /**
     * Add a c14n:InclusiveNamespaces child element to the specified element with an empty PrefixList.
     */
    public static void addInclusiveNamespacesToElement(Element element) {
        //
        // NOTE: Since we have no PrefixList attribute we should not have any inlclusive namespaces
        // element (See the DTD http://www.w3.org/TR/xml-exc-c14n/exc-c14n.dtd)
        //
        //Element inclusiveNamespaces = XmlUtil.createAndAppendElementNS(element,
        //                                                               "InclusiveNamespaces",
        //                                                               Transform.C14N_EXCLUSIVE,
        //                                                               "c14n");
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
     * @throws SignatureException   if the signature could not be validated; or,
     *   (caused by {@link KeyUsageException}) if the key usage enforcement policy currently in effect does not permit the specified
     *                            senderSigningCert to be used with the {@link KeyUsageActivity#verifyXml} activity.
     */
    public static X509Certificate checkSimpleEnvelopedSignature(final Element sigElement, SecurityTokenResolver securityTokenResolver) throws SignatureException {
        Element keyInfoElement = KeyInfo.searchForKeyInfo(sigElement);
        if (keyInfoElement == null) throw new SignatureException("No KeyInfo found in signature");

        final X509Certificate signingCert;

        try {
            X509Certificate cert = null;
            KeyInfo keyInfo = new KeyInfo(keyInfoElement);
            String[] keyNames = keyInfo.getKeyNames();
            if (keyNames != null && keyNames.length > 0) {
                cert = securityTokenResolver.lookupByKeyName(keyNames[0]);
            }

            if (cert == null)
                throw new SignatureException("Could not find certificate.");

            signingCert = cert;
        }
        catch(XSignatureException xse) {
            throw new SignatureException(xse);
        }

        PublicKey signingKey = signingCert.getPublicKey();
        if (signingKey == null) throw new SignatureException("Unable to find signing key"); // can't happen

        try {
            KeyUsageChecker.requireActivity(KeyUsageActivity.verifyXml, signingCert);
            signingCert.checkValidity();
        } catch (CertificateException e) {
            throw new SignatureException(e);
        }

        SignatureContext sigContext = new SignatureContext();
        sigContext.setEntityResolver(XmlUtil.getXss4jEntityResolver());
        sigContext.setIDResolver(new IDResolver(){
            public Element resolveID(Document document, String id) {
                if (id.equals(sigElement.getOwnerDocument().getDocumentElement().getAttribute("Id"))) {
                    return sigElement.getOwnerDocument().getDocumentElement();
                } else {
                    return null;
                }
            }
        });

        sigContext.setAlgorithmFactory(new AlgorithmFactoryExtn() {
            public Transform getTransform(String transform) throws NoSuchAlgorithmException {
                if ( Transform.XSLT.equals(transform) ||
                        Transform.XPATH.equals(transform) ||
                        Transform.XPATH2.equals(transform) ) {
                    throw new NoSuchAlgorithmException(transform);
                }
                return super.getTransform(transform);
            }
        });

        Validity validity = DsigUtil.verify(sigContext, sigElement, signingKey);

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
     * Wraps XSS4J SignatureContext.verify() with a version that does some extra sanity checks on the context and key.
     * <p/>
     * Specifically this will throw a SignatureException if an HMACOutputLength element is present anywhere within
     * the Signature element.
     *
     * @param sigContext  the signature context.  Required.
     * @param signatureElement  the ds:Signature DOM Element.  Required.
     * @param verificationKey   the key to use to verify the signature.  Required.
     * @return the result of SignatureContext.verify().
     * @throws SignatureException
     */
    public static Validity verify(SignatureContext sigContext, Element signatureElement, Key verificationKey) throws SignatureException {
        precheckSigElement(signatureElement, verificationKey);
        return sigContext.verify(signatureElement, verificationKey);
    }

    /**
     * Perform sanity checks on a Signature element before verification.
     * <p/>
     * The primary purpose of this method is to check for and reject HMACOutputLength elements
     * in order to prevent vulnerability to CVE-2009-0217 (Bug #7526)
     *
     * @param sigElement the ds:Signature element to precheck
     * @param verificationKey the key that will be used to verify it
     */
    public static void precheckSigElement(Element sigElement, Key verificationKey) throws SignatureException {
        checkForHmacOutputLength(sigElement);
        checkForHmacWithNonSecretKey(sigElement, verificationKey);
    }

    private static final DOMXPath hmacOutputLengthFinder;
    private static final DOMXPath hmacSigMethodFinder;
    static {
        try {
            hmacOutputLengthFinder = new DOMXPath("ds:SignedInfo/ds:SignatureMethod/ds:HMACOutputLength");
            hmacOutputLengthFinder.addNamespace("ds", SoapUtil.DIGSIG_URI);
            hmacSigMethodFinder = new DOMXPath("ds:SignedInfo/ds:SignatureMethod[contains(string(@Algorithm), \"#hmac-\")]");
            hmacSigMethodFinder.addNamespace("ds", SoapUtil.DIGSIG_URI);
        } catch (JaxenException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private static void checkForHmacOutputLength(Element sigElement) throws SignatureException {
        try {
            List results = hmacOutputLengthFinder.selectNodes(sigElement);
            if (results != null && results.size() > 0)
                throw new SignatureException("Unsupported Signature: Signature contains HMACOutputLength parameter");
        } catch (JaxenException e) {
            throw new SignatureException("Unable to check for HMACOutputLength elements within Signature: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private static void checkForHmacWithNonSecretKey(Element sigElement, Key verificationKey) throws SignatureException {
        if (usesHmacSignatureMethod(sigElement) && !(verificationKey instanceof SecretKey))
            throw new SignatureException("Unable to verify HMAC signature with key of type " + verificationKey.getAlgorithm() + " (" + verificationKey.getClass() + ")");
    }

    private static boolean usesHmacSignatureMethod(Element sigElement) throws SignatureException {
        try {
            List results = hmacSigMethodFinder.selectNodes(sigElement);
            return results != null && results.size() > 0;
        } catch (JaxenException e) {
            throw new SignatureException("Unable to check for HMAC signature method: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Strips all ds:Signature elements that are immediate children of the specified element.
     * Note that, if a signature is removed, the resulting DOM tree will almost certainly contain multiple
     * consecutive text nodes.
     *
     * @param parent  the element whose Signature children should be eliminated.
     */
    public static void stripSignatures(Element parent) {
        List sigs = DomUtils.findChildElementsByName(parent, DIGSIG_URI, "Signature");
        for (Iterator i = sigs.iterator(); i.hasNext();) {
            Element sigEl = (Element)i.next();
            sigEl.getParentNode().removeChild(sigEl);
        }
    }

    /**
     * Examine the specified private key to determine what type of signature method it should use, based on
     * its type and the currently in-effect default signature message digest algorithm (which defaults to "SHA-1"
     * if not configured).
     * <p/>
     * This method reads the system property "com.l7tech.security.xml.decorator.digsig.messagedigest" to check
     * the default digest algorithm.
     *
     * @param signingKey the key you are about to sign something with.  Required.
     * @return the signature method to use.  Never null.
     * @throws SignatureException if no signature method is known for the supplied private key type.
     */
    public static SupportedSignatureMethods getSignatureMethod(final PrivateKey signingKey) throws SignatureException {

        final String md = getDefaultMessageDigest();

        SupportedSignatureMethods signaturemethod;
        if (signingKey instanceof RSAPrivateKey || "RSA".equals(signingKey.getAlgorithm())) {
            if ("SHA-256".equals(md))
                signaturemethod = SupportedSignatureMethods.RSA_SHA256;
            else
                signaturemethod = SupportedSignatureMethods.RSA_SHA1;

        } else if (signingKey instanceof ECPrivateKey || "EC".equals(signingKey.getAlgorithm())) {
            if ("SHA-256".equals(md))
                signaturemethod = SupportedSignatureMethods.ECDSA_SHA256;
            else if ("SHA-512".equals(md))
                signaturemethod = SupportedSignatureMethods.ECDSA_SHA512;
            else
                // SHA-384 is the chosen default for GD
                signaturemethod = SupportedSignatureMethods.ECDSA_SHA384;

        } else if (signingKey instanceof DSAPrivateKey) {
            signaturemethod = SupportedSignatureMethods.DSA_SHA1;
        } else if (signingKey instanceof SecretKey) {
            signaturemethod = SupportedSignatureMethods.HMAC_SHA1;
        } else {
            throw new SignatureException("Private Key type not supported " + signingKey.getClass().getName());
        }
        return signaturemethod;
    }

    /**
     * Get the default message digest algorithm to use for signatures on this system.
     * <p/>
     * This method reads the system property "com.l7tech.security.xml.decorator.digsig.messagedigest" to check
     * the default digest algorithm.
     *
     * @return the default digest algorithm, defaulting to "SHA-1".  Never null.
     */
    public static String getDefaultMessageDigest() {
        return SyspropUtil.getString("com.l7tech.security.xml.decorator.digsig.messagedigest", "SHA-1");
    }
}
