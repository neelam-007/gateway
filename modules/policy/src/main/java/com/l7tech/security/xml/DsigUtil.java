/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.security.xml;

import com.ibm.xml.dsig.*;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.processor.WssProcessorAlgorithmFactory;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.l7tech.security.xml.SupportedDigestMethods.*;

/**
 * Utility class to help with XML digital signatures.
 */
public class DsigUtil {
    public static final String DIGSIG_URI = "http://www.w3.org/2000/09/xmldsig#";

    /**
     * Get the identifier attribute for the given element.
     *
     * <p>Note that the identifier attribute does not need to be present on the
     * element.</p>
     *
     * @param element The element to process
     * @return The identifier attribute name.
     */
    public static String getIdAttribute( final Element element ) {
        String idAttr = "Id";

        if ( element != null && SamlConstants.ELEMENT_ASSERTION.equals(element.getLocalName()) ) {
            if ( SamlConstants.NS_SAML.equals(element.getNamespaceURI()) ) {
                idAttr = SamlConstants.ATTR_ASSERTION_ID;
            } else if ( SamlConstants.NS_SAML2.equals(element.getNamespaceURI()) ) {
                idAttr = SamlConstants.ATTR_SAML2_ASSERTION_ID;
            }
        }

        return idAttr;
    }

    /**
     * Create a new SignatureContext with reasonable defaults for validation.
     *
     * @return The new signature context.
     */
    @NotNull
    public static SignatureContext createSignatureContextForValidation() {
        final SignatureContext sigContext = new SignatureContext(){
            @Override
            protected Document parse( final InputSource src ) throws IOException, SAXException, ParserConfigurationException {
                return XmlUtil.parse( src, false );
            }
        };

        sigContext.setEntityResolver( XmlUtil.getXss4jEntityResolver() );

        return sigContext;
    }

    /**
     * Delegates to  {@link #createEnvelopedSignature(org.w3c.dom.Element, String, java.security.cert.X509Certificate, java.security.PrivateKey, org.w3c.dom.Element, String, String)}
     * with the xsdIdAttribute set to the result of {@link #getIdAttribute(org.w3c.dom.Element)} 
     *
     */
    public static Element createEnvelopedSignature(final Element elementToSign,
                                                   final X509Certificate senderSigningCert,
                                                   final PrivateKey senderSigningKey,
                                                   final Element keyInfoChildElement,
                                                   final String keyName,
                                                   final String messageDigestAlgorithm)
            throws SignatureException, SignatureStructureException, XSignatureException
    {
        final String idAttr = getIdAttribute(elementToSign);
        String idValue = elementToSign.getAttribute(idAttr);
        if (idValue == null || idValue.length() < 1) {
            idValue = SoapUtil.generateUniqueId( "root", 0 );
            elementToSign.setAttributeNS( null, idAttr, idValue );
        }

        return createEnvelopedSignature(elementToSign, idValue, senderSigningCert, senderSigningKey,
                keyInfoChildElement, keyName, messageDigestAlgorithm);
    }

    /**
     * Digitally sign the specified element, using the specified key and including the specified cert inline
     * in the KeyInfo.
     *
     * @param elementToSign         the element to sign
     * @param targetElementIdValue  the value of the xsd:ID attribute on the element being signed, to reference from the Signature using a shorthand XPointer.  Required.
     * @param senderSigningCert     certificate to sign it with.  will be included inline in keyinfo
     * @param senderSigningKey      private key to sign it with.
     * @param keyInfoChildElement   Custom key info child element to use
     * @param keyName               if specified, KeyInfo will use a keyName instead of an STR or a literal cert.
     * @param messageDigestAlgorithm the message digest algorithm to use for the signature, or null to use the default behavior, which is:
     *                               SHA-1 for everything except an EC private key, in which case SHA-256
     * @return the new dsig:Signature element, as a standalone element not yet attached into the document.
     * @throws SignatureException   if there is a problem creating the signature or if the xsdIdAttribute is not found on the Element to sign
     * @throws SignatureStructureException if there is a problem creating the signature
     * @throws XSignatureException  if there is a problem creating the signature
     */
    public static Element createEnvelopedSignature(final Element elementToSign,
                                                   final String targetElementIdValue,
                                                   X509Certificate senderSigningCert,
                                                   PrivateKey senderSigningKey,
                                                   Element keyInfoChildElement,
                                                   String keyName,
                                                   String messageDigestAlgorithm)
            throws SignatureException, SignatureStructureException, XSignatureException
    {
        final Map<String, Element> elementsToSignWithIDs = new HashMap<String, Element>();
        elementsToSignWithIDs.put(targetElementIdValue, elementToSign);
        return createSignature(elementsToSignWithIDs, elementToSign.getOwnerDocument(), senderSigningCert, senderSigningKey, keyInfoChildElement, keyName, messageDigestAlgorithm, null, true, false);
    }

    /**
     * Digitally signs the specified elements and returns a signature element that can be inserted into this document or a different one.
     *
     * @param elementsToSignWithIDs  the elements to sign, along with their corresponding reference values.  Required.  May be empty, in which case a signature with no References will be created.
     * @param document              Document to use as a factory for the new signature element.  Required.
     * @param senderSigningCert    certificate to sign it with.  cert will be included inline in KeyInfo as X509Certificate element if no keyinfo or key name specified
     * @param senderSigningKey      private key to sign it with.
     * @param keyInfoChildElement   Custom key info child element to use
     * @param keyName               if specified, KeyInfo will use a keyName instead of an STR or a literal cert.
     * @param signatureMethodDigestAlg the message digest algorithm to use for the signature, or null to use the default behavior, which is:
     *                               SHA-1 for everything except an EC private key, in which case SHA-256
     * @param referenceDigestAlg message digest algorithm for References, if different from that of the signature method, or null to use the same as the signature method.
     * @param includeEnvelopedTransform  if true, the created signature will include the Enveloped transform, so it can be added as a descendant of one of the signed elements.
     * @param enableImplicitEmptyUriRef  if true, a Reference to an ID consisting of the empty string will be recognized as a reference to the document root.
     * @return the new dsig:Signature element, as a standalone element not yet attached into the document.
     * @throws SignatureException   if there is a problem creating the signature or if the xsdIdAttribute is not found on the Element to sign
     * @throws SignatureStructureException if there is a problem creating the signature
     * @throws XSignatureException  if there is a problem creating the signature
     */
    public static Element createSignature(final Map<String, Element> elementsToSignWithIDs,
                                          final Document document,
                                          X509Certificate senderSigningCert,
                                          PrivateKey senderSigningKey,
                                          Element keyInfoChildElement,
                                          String keyName,
                                          String signatureMethodDigestAlg,
                                          @Nullable String referenceDigestAlg,
                                          boolean includeEnvelopedTransform,
                                          final boolean enableImplicitEmptyUriRef)
            throws SignatureException, SignatureStructureException, XSignatureException
    {
        return createSignature( elementsToSignWithIDs, document, new X509Certificate[] { senderSigningCert },
                senderSigningKey, keyInfoChildElement, keyName, signatureMethodDigestAlg, referenceDigestAlg, includeEnvelopedTransform,
                enableImplicitEmptyUriRef );
    }

    /**
     * Digitally signs the specified elements and returns a signature element that can be inserted into this document or a different one.
     *
     * @param elementsToSignWithIDs  the elements to sign, along with their corresponding reference values.  Required.  May be empty, in which case a signature with no References will be created.
     * @param document              Document to use as a factory for the new signature element.  Required.
     * @param senderSigningCertChain     certificate chain to sign it with.  zeroth element must be signer cert.  full chain will be included inline in KeyInfo as X509Certificate elements if no keyinfo or key name specified
     * @param senderSigningKey      private key to sign it with.
     * @param keyInfoChildElement   Custom key info child element to use
     * @param keyName               if specified, KeyInfo will use a keyName instead of an STR or a literal cert.
     * @param signatureMethodDigestAlg the message digest algorithm to use for the signature, or null to use the default behavior, which is:
     *                               SHA-1 for everything except an EC private key, in which case SHA-256
     * @param referenceDigestAlg message digest algorithm for References, if different from that of the signature method, or null to use the same as the signature method.
     * @param includeEnvelopedTransform  if true, the created signature will include the Enveloped transform, so it can be added as a descendant of one of the signed elements.
     * @param enableImplicitEmptyUriRef  if true, a Reference to an ID consisting of the empty string will be recognized as a reference to the document root.
     * @return the new dsig:Signature element, as a standalone element not yet attached into the document.
     * @throws SignatureException   if there is a problem creating the signature or if the xsdIdAttribute is not found on the Element to sign
     * @throws SignatureStructureException if there is a problem creating the signature
     * @throws XSignatureException  if there is a problem creating the signature
     */
    public static Element createSignature(final Map<String, Element> elementsToSignWithIDs,
                                          final Document document,
                                          X509Certificate[] senderSigningCertChain,
                                          PrivateKey senderSigningKey,
                                          Element keyInfoChildElement,
                                          String keyName,
                                          String signatureMethodDigestAlg,
                                          @Nullable String referenceDigestAlg,
                                          boolean includeEnvelopedTransform,
                                          final boolean enableImplicitEmptyUriRef)
            throws SignatureException, SignatureStructureException, XSignatureException
    {
        if (elementsToSignWithIDs == null) throw new NullPointerException("elementsToSignWithIDs must be provided");
        if (document == null) throw new NullPointerException("document must be provided");

        SupportedSignatureMethods signaturemethod = getSignatureMethodForSignerPrivateKey(senderSigningKey, signatureMethodDigestAlg, true);
        SupportedDigestMethods digestMethod = referenceDigestAlg == null ? SupportedDigestMethods.fromAlias(signaturemethod.getDigestAlgorithmName()) : SupportedDigestMethods.fromAlias(referenceDigestAlg);

        // Create signature template and populate with appropriate transforms. Reference is to SOAP Envelope
        TemplateGenerator template = new TemplateGenerator(document,
                                                           digestMethod.getIdentifier(),
                                                           Canonicalizer.EXCLUSIVE,
                                                           signaturemethod.getAlgorithmIdentifier());
        template.setIndentation(false);
        template.setPrefix("ds");

        for (String targetId : elementsToSignWithIDs.keySet()) {
            final String ref = (targetId == null || "".equals(targetId)) ? "" : "#" + targetId;
            Reference rootRef = template.createReference(ref);
            if (includeEnvelopedTransform)
                rootRef.addTransform(Transform.ENVELOPED);
            rootRef.addTransform(Transform.C14N_EXCLUSIVE);
            template.addReference(rootRef);
        }

        // Get the signature element
        Element sigElement = template.getSignatureElement();

        // Include KeyInfo element in signature and embed cert into subordinate X509Data element

        KeyInfo keyInfo = new KeyInfo();
        if (keyName != null && keyName.length() > 0) {
            keyInfo.setKeyNames(new String[] { keyName });
        } else if (keyInfoChildElement != null) {
            keyInfo.setUnknownChildren(new Element[] { keyInfoChildElement });
        } else {
            List<KeyInfo.X509Data> datas = new ArrayList<>();
            for ( X509Certificate cert : senderSigningCertChain ) {
                KeyInfo.X509Data data = new KeyInfo.X509Data();
                data.setCertificate( cert );
                data.setParameters( cert, false, false, false );
                datas.add( data );
            }
            keyInfo.setX509Data( datas.toArray( new KeyInfo.X509Data[datas.size()] ) );
        }
        keyInfo.insertTo(sigElement, "ds", template);

        SignatureContext sigContext = new SignatureContext();
        sigContext.setIDResolver(new IDResolver() {
            @Override
            public Element resolveID(Document document, String s) {
                return ("".equals(s) && enableImplicitEmptyUriRef) ? document.getDocumentElement() : elementsToSignWithIDs.get(s);
            }
        });
        sigContext.setEntityResolver( XmlUtil.getXss4jEntityResolver());
        sigContext.setResourceShower(new ResourceShower() {
            @Override
            public void showSignedResource(Element element, int i, String s, String s1, byte[] bytes, String s2) {

            }
        });
        try {
            KeyUsageChecker.requireActivityForKey(KeyUsageActivity.signXml, senderSigningCertChain[0], senderSigningKey);
        } catch (CertificateException e) {
            throw new SignatureException(e);
        }
        sigContext.setAlgorithmFactory(createSignatureAlgorithmFactory(senderSigningKey, null));
        try {
            return sigContext.sign(sigElement, senderSigningKey);
        } catch (XSignatureException e) {
            repairXSignatureException(e);
            throw e;
        }
    }

    /**
     * Create an algorithm factory for peforming an XML (or WSS) signature using the specified signing key.
     * <p/>
     * This will hardwire a security Provider for signing if indicated by the current JceProvider for this key type.
     * 
     * @param signingKey the secret or private key we will be signing with.  Required.
     * @param strToTarget a map of SecurityTokenReference -> target nodes.  If null, STR-Transform will not be supported.
     * @return an appropriately-configured algorithm factory.  Never null.
     */
    public static WssProcessorAlgorithmFactory createSignatureAlgorithmFactory( @NotNull Key signingKey, @Nullable final Map<Node, Node> strToTarget ) {
        if ("RSA".equalsIgnoreCase(signingKey.getAlgorithm())) {
            return new WssProcessorAlgorithmFactory(strToTarget, JceProvider.getInstance().getProviderFor(JceProvider.SERVICE_SIGNATURE_RSA_PRIVATE_KEY), null);
        } else if ("EC".equals(signingKey.getAlgorithm()) || "ECDSA".equals(signingKey.getAlgorithm())) {
            return new WssProcessorAlgorithmFactory(strToTarget, null, JceProvider.getInstance().getProviderFor(JceProvider.SERVICE_SIGNATURE_ECDSA));
        } else {        
            return new WssProcessorAlgorithmFactory(strToTarget);
        }
    }

    /**
     * Repair broken exception chaining for an XSignatureException.
     *
     * @param e XSignatureException to repair.  If it has a wrapped exception but no cause, we'll init its cause to its wrapped exception.
     */
    public static void repairXSignatureException(XSignatureException e) {
        // Repair broken exception chaining mechanism
        if (e.getCause() == null && e.getException() != null) {
            e.initCause(e.getException());
        }
    }

    /**
     * Given a signer private key and a digest algorithm, returns the appropriate signature method enum.
     *
     * @param senderSigningKey  the private key that is to be used to create the signature.  Required.
     * @param messageDigestAlgorithm  the message digest algorithm name, ie "SHA-1".  Required.
     * @param allowDigestFallback if true, and no signature method is available with the specified key type and digest, we will fall back to SHA-1 as the digest if possible.
     *                            if false, we will fail and throw SignatureException rather than falling back to SHA-1.
     * @return an instance of SupportedSignatureMethods encoding the signature method to use.  Never null.
     * @throws SignatureException if the combination of private key type and message digest is not supported.
     */
    public static SupportedSignatureMethods getSignatureMethodForSignerPrivateKey(Key senderSigningKey, String messageDigestAlgorithm, boolean allowDigestFallback) throws SignatureException {
        SupportedDigestMethods messageDigest;
        try {
            messageDigest = SupportedDigestMethods.fromAlias(messageDigestAlgorithm);
        } catch (IllegalArgumentException e) {
            messageDigest = getDefaultMessageDigest(senderSigningKey.getAlgorithm());
        }

        String keyAlg;
        if (senderSigningKey instanceof PublicKey || senderSigningKey instanceof PrivateKey)
            keyAlg = senderSigningKey.getAlgorithm();
        else if (senderSigningKey instanceof SecretKey)
            keyAlg = "SecretKey";
        else if (senderSigningKey != null)
            throw new SignatureException("Signing Key type not recognized: " + senderSigningKey.getAlgorithm() + " / " +
                    senderSigningKey.getClass().getName());
        else
            throw new NullPointerException("senderSigningKey is required");

        SupportedSignatureMethods sigMethod = SupportedSignatureMethods.fromKeyAndMessageDigest(keyAlg, messageDigest.getCanonicalName());
        if (sigMethod != null)
            return sigMethod;

        if (allowDigestFallback) {
            // Couldn't find that exact combination -- find the one we can use with this key type
            sigMethod = SupportedSignatureMethods.fromKeyAlg(keyAlg);
            if (sigMethod != null)
                return sigMethod;
        }

        throw new SignatureException("No signature method available for key type " + keyAlg + " and message digest " + messageDigest.getCanonicalName());
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
                cert = securityTokenResolver.lookupByKeyName( CertUtils.formatDN(keyNames[0]) );
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
            @Override
            public Element resolveID(Document document, String id) {
                if (id.equals(sigElement.getOwnerDocument().getDocumentElement().getAttribute("Id"))) {
                    return sigElement.getOwnerDocument().getDocumentElement();
                } else {
                    return null;
                }
            }
        });

        sigContext.setAlgorithmFactory(new AlgorithmFactoryExtn() {
            @Override
            public Transform getTransform(String transform) throws NoSuchAlgorithmException {
                if ( Transform.XSLT.equals(transform) ||
                        Transform.XPATH.equals(transform) ||
                        Transform.XPATH2.equals(transform) ) {
                    throw new NoSuchAlgorithmException(transform);
                }
                return super.getTransform(transform);
            }

            @Override
            public MessageDigest getDigestMethod(String s) throws NoSuchAlgorithmException, NoSuchProviderException {
                MessageDigest dig = super.getDigestMethod(s);
                if ("MD5".equalsIgnoreCase(dig.getAlgorithm()))
                    throw new NoSuchAlgorithmException("MD5 not supported for digests for license signing");
                return dig;
            }

            @Override
            public SignatureMethod getSignatureMethod(String s, Object o) throws NoSuchAlgorithmException, NoSuchProviderException {
                if ("MD5".equalsIgnoreCase(SupportedSignatureMethods.fromSignatureAlgorithm(s).getDigestAlgorithmName()))
                    throw new NoSuchAlgorithmException("MD5 not supported for signature method for license signing");
                return super.getSignatureMethod(s, o);
            }
        });

        Validity validity = DsigUtil.verify(sigContext, sigElement, signingKey);

        if (!validity.getCoreValidity()) {
            throw new SignatureException(getInvalidSignatureMessage(validity));
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

    private static final Pattern FAKE_STACK_TRACE_STRIPPER = Pattern.compile("$\\s+^\\tat .*", Pattern.DOTALL | Pattern.MULTILINE);

    /**
     * Make an error message for an XSS4J signature validation failure that does not embed a full stack trace
     * into the error message text unless exception debug mode is enabled.
     *
     * @param validity a Validity that returns false from getCoreValidity().  Required.
     * @return an error message that will not include the expanded stack trace unless exception debug mode is on.  Never null.
     */
    public static String getInvalidSignatureMessage(@NotNull Validity validity) {
        String infoMess = validity.getSignedInfoMessage();
        if (!JdkLoggerConfigurator.debugState() && infoMess != null) {
            // Strip stack trace "helpfully" folded into the signed info message by XSS4J (Bug #12199)
            infoMess = FAKE_STACK_TRACE_STRIPPER.matcher(infoMess).replaceAll("");
        }

        StringBuilder msg = new StringBuilder("Signature not valid. " + infoMess);
        for (int i = 0; i < validity.getNumberOfReferences(); i++)
            msg.append("\n\tElement ")
               .append(validity.getReferenceURI(i))
               .append(": ")
               .append(validity.getReferenceMessage(i));
        return msg.toString();
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
     * @throws SignatureException if the signature cannot be validated
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
     * @throws java.security.SignatureException if there is something unsavory about the signature element
     */
    public static void precheckSigElement(Element sigElement, Key verificationKey) throws SignatureException {
        try {
            Element sigMethod = findSigMethod(sigElement);
            checkForHmacOutputLength(sigMethod);
            checkForHmacWithNonSecretKey(sigMethod, verificationKey);
        } catch (InvalidDocumentFormatException e) {
            throw new SignatureException(e);
        }
    }

    public static String findSigAlgorithm(Element sigElement) throws SignatureException {
        try {
            Element sigMethod = findSigMethod(sigElement);
            String sigAlg = sigMethod.getAttribute("Algorithm");
            if (sigAlg == null) throw new SignatureException("Algorithm attribute not specified for SignatureMethod");
            return sigAlg;
        } catch (InvalidDocumentFormatException e) {
            throw new SignatureException(e);
        }
    }

    public static String[] findDigestAlgorithms(Element sigElement) throws SignatureException {
        List<String> result = new ArrayList<String>();
        result.add(SupportedDigestMethods.fromAlias(SupportedSignatureMethods.fromSignatureAlgorithm(findSigAlgorithm(sigElement)).getDigestAlgorithmName()).getIdentifier());

        try {
            Element signedInfo = DomUtils.findExactlyOneChildElementByName(sigElement, SoapUtil.DIGSIG_URI, "SignedInfo");
            for(Element reference : DomUtils.findChildElementsByName(signedInfo, SoapUtil.DIGSIG_URI, SoapConstants.REFERENCE_EL_NAME)) {
                Element digestMethod = DomUtils.findExactlyOneChildElementByName(reference, SoapUtil.DIGSIG_URI, SoapConstants.REFERENCE_DIGEST_METHOD_EL_NAME);
                String digestAlg = digestMethod.getAttribute("Algorithm");
                if (digestAlg == null) throw new SignatureException("Algorithm attribute not specified for reference DigestMethod");
                result.add(digestAlg);
            }
        } catch (InvalidDocumentFormatException e) {
            throw new SecurityException(e);
        }

        return result.toArray(new String[result.size()]);
    }

    private static Element findSigMethod(Element sigElement) throws InvalidDocumentFormatException {
        if (sigElement == null) throw new IllegalArgumentException("need sigElement");
        Element signedInfo = DomUtils.findExactlyOneChildElementByName(sigElement, SoapUtil.DIGSIG_URI, "SignedInfo");
        return DomUtils.findExactlyOneChildElementByName(signedInfo, SoapUtil.DIGSIG_URI, "SignatureMethod");
    }

    private static void checkForHmacOutputLength(Element sigMethod) throws SignatureException {
        Element hmacOutputLen = DomUtils.findFirstChildElementByName(sigMethod, SoapUtil.DIGSIG_URI, "HMACOutputLength");
        if (hmacOutputLen != null)
            throw new SignatureException("Unsupported Signature: Signature contains HMACOutputLength parameter");
    }

    private static void checkForHmacWithNonSecretKey(Element sigMethod, Key verificationKey) throws SignatureException {
        if (usesHmacSignatureMethod(sigMethod) && !(verificationKey instanceof SecretKey))
            throw new SignatureException("Unable to verify HMAC signature with key of type " + verificationKey.getAlgorithm() + " (" + verificationKey.getClass() + ")");
    }

    private static boolean usesHmacSignatureMethod(Element sigMethod) throws SignatureException {
        String alg = sigMethod.getAttribute("Algorithm");
        return alg != null && alg.indexOf("#hmac-") >= 0;
    }

    /**
     * Strips all ds:Signature elements that are immediate children of the specified element.
     * Note that, if a signature is removed, the resulting DOM tree may contain multiple
     * consecutive text nodes.
     *
     * @param parent  the element whose Signature children should be eliminated.
     */
    public static void stripSignatures(Element parent) {
        List<Element> sigs = DomUtils.findChildElementsByName(parent, DIGSIG_URI, "Signature");
        for (Element sigEl : sigs) {
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
        final String keyAlg = signingKey.getAlgorithm();
        final boolean usingEcc = signingKey instanceof ECPrivateKey || "EC".equals(keyAlg);
        final SupportedDigestMethods md = getDefaultMessageDigest(usingEcc ? "EC" : keyAlg);

        SupportedSignatureMethods signaturemethod;
        if (signingKey instanceof RSAPrivateKey || "RSA".equals(keyAlg)) {
            if (SHA256 == md)
                signaturemethod = SupportedSignatureMethods.RSA_SHA256;
            else
                signaturemethod = SupportedSignatureMethods.RSA_SHA1;

        } else if (usingEcc) {
            if (SHA256 == md)
                signaturemethod = SupportedSignatureMethods.ECDSA_SHA256;
            else if (SHA512 == md)
                signaturemethod = SupportedSignatureMethods.ECDSA_SHA512;
            else
                // SHA-384 is the chosen default for GD
                signaturemethod = SupportedSignatureMethods.ECDSA_SHA384;

        } else if (signingKey instanceof DSAPrivateKey) {
            signaturemethod = SupportedSignatureMethods.DSA_SHA1;
        } else if (signingKey instanceof SecretKey) {
            signaturemethod = SupportedSignatureMethods.HMAC_SHA1;
        } else {
            throw new SignatureException("No signature method available for signing key type: " + signingKey.getAlgorithm() + " / " +
                    signingKey.getClass().getName());
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
    public static SupportedDigestMethods getDefaultMessageDigest() {
        return getDefaultMessageDigest("RSA");
    }

    static SupportedDigestMethods getDefaultMessageDigest(String keyAlg) {
        if ("DSA".equals(keyAlg))
            return SHA1; // Currently the only supported DSA signature method is SHA1withDSA, so it's always the default digest for this key type
        try {
            return SupportedDigestMethods.fromAlias(ConfigFactory.getProperty( "com.l7tech.security.xml.decorator.digsig.messagedigest", null ));
        } catch (IllegalArgumentException e) {
            return "EC".equals(keyAlg) ? SHA384 : SHA1;
        }
    }
}
