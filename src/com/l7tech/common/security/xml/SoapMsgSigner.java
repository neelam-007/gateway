package com.l7tech.common.security.xml;

import com.ibm.xml.dsig.*;
import com.ibm.xml.dsig.util.AdHocIDResolver;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.*;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import sun.misc.BASE64Decoder;

/**
 * Signs soap messages.
 *
 * Meant to be used by both server-side and proxy-side dsig assertions
 *  
 * <br/><br/>
 * Layer 7 technologies, inc.<br/>
 * User: flascell<br/>
 * Date: Aug 19, 2003<br/>
 * $Id$
 */
public final class SoapMsgSigner {
    private static final String DS_PREFIX = "ds";
    private static final String DEF_ENV_TAG = "envId";
    private static final String SEC_TOK_REF_NAME = "SecurityTokenReference";
    private static final String WSSE_REF_NAME = "Reference";
    private static final String BINSECTOKEN_NAME = "BinarySecurityToken";

    private SoapMsgSigner() { }

    /**
     * Appends a soap message with a digital signature of it's entire envelope.
     * <p/>
     * If the envelope already has as Id attribute, it's value will be used to refer to the envelope within the
     * SignedInfo element. Otherwise, an Id of value DEF_ENV_TAG will be used.
     * 
     * @param soapMsg    the xml document containing the soap message expected to contain at least a soapenvelope element.
     *                   this document contains the signature when at return time.
     * @param privateKey the private key of the signer if imlpements RSAPrivateKey signature method will be
     *                   http://www.w3.org/2000/09/xmldsig#rsa-sha1, if privateKey implements DSAPrivateKey, signature method will be
     *                   http://www.w3.org/2000/09/xmldsig#dsa-sha1.
     * @param certChain  the signer's cert chain
     * @throws com.ibm.xml.dsig.SignatureStructureException
     *          
     * @throws com.ibm.xml.dsig.XSignatureException
     *          
     */
    public static void signEnvelope(Document soapMsg, PrivateKey privateKey, X509Certificate[] certChain)
      throws SignatureStructureException, XSignatureException {
        // is the envelope already ided?
        String id = SoapUtil.getElementId(soapMsg.getDocumentElement());

        if (id == null || id.length() < 1) {
            id = DEF_ENV_TAG;
        }
        signElement(soapMsg, soapMsg.getDocumentElement(), id, privateKey, certChain);
    }

    /**
     * Sign the document element using the private key and Embed the <code>X509Certificate</code> into
     * the XML document.
     * 
     * @param document    the xml document containing the element to sign.
     * @param messagePart        the document element
     * @param referenceId the signature reference ID attreibute value
     * @param privateKey  the private key of the signer if imlpements RSAPrivateKey signature method will be
     *                    http://www.w3.org/2000/09/xmldsig#rsa-sha1, if privateKey implements DSAPrivateKey, signature method will be
     *                    http://www.w3.org/2000/09/xmldsig#dsa-sha1.
     * @param certChain        the signer's cert chain
     * @throws com.ibm.xml.dsig.SignatureStructureException
     *                                  
     * @throws com.ibm.xml.dsig.XSignatureException
     *                                  
     * @throws IllegalArgumentException if any of the parameters i <b>null</b>
     */
    public static void signElement(Document document, final Element messagePart, String referenceId, PrivateKey privateKey, X509Certificate[] certChain)
      throws SignatureStructureException, XSignatureException {

        if (document == null || messagePart == null | referenceId == null ||
          privateKey == null || certChain == null || certChain.length == 0) {
            throw new IllegalArgumentException();
        }

        String id = messagePart.getAttribute(referenceId);
        if (id == null || "".equals(id)) {
            id = referenceId;
            messagePart.setAttribute(SoapUtil.ID_ATTRIBUTE_NAME, referenceId);
        }

        // set the appropriate signature method
        String signaturemethod = getSignatureMethod(privateKey);

        // Create signature template and populate with appropriate transforms. Reference is to SOAP Envelope
        TemplateGenerator template = new TemplateGenerator(document, XSignature.SHA1, Canonicalizer.W3C2, signaturemethod);
        template.setPrefix(DS_PREFIX);
        Reference ref = template.createReference("#" + id);
        ref.addTransform(Transform.ENVELOPED);
        ref.addTransform(Transform.W3CC14N2);
        template.addReference(ref);
        Element emptySignatureElement = template.getSignatureElement();

        // Signature is inserted in Header/Security, as per WS-S
        Element securityHeaderElement = SoapUtil.getOrMakeSecurityElement(document);

        Element signatureElement = (Element)securityHeaderElement.appendChild(emptySignatureElement);

        // Include KeyInfo element in signature and embed cert into subordinate X509Data element
        KeyInfo keyInfo = new KeyInfo();
        KeyInfo.X509Data x509Data = new KeyInfo.X509Data();
        x509Data.setCertificate(certChain[0]);
        x509Data.setParameters(certChain[0], true, true, true);
        keyInfo.setX509Data(new KeyInfo.X509Data[]{x509Data});
        keyInfo.insertTo(signatureElement, DS_PREFIX);

        normalizeDoc(document);

        // Setup context and sign document
        SignatureContext sigContext = new SignatureContext();
        AdHocIDResolver idResolver = new AdHocIDResolver(document);
        sigContext.setIDResolver(idResolver);

        sigContext.sign(signatureElement, privateKey);
    }

    /**
       * Verify that a valid signature is included and that the entire envelope is signed.
       * The validity of the signer's cert is NOT verified against the local root authority.
       *
       * @param soapMsg the soap message that potentially contains a digital signature
       * @return the cert chain used as part of the message's signature (not checked against any authority) never null
       * @throws com.l7tech.common.security.xml.SignatureNotFoundException
       *          if no signature is found in document
       * @throws com.l7tech.common.security.xml.InvalidSignatureException
       *          if the signature is invalid, not in an expected format or is missing information
       */
      public static X509Certificate[] validateSignature(Document soapMsg)
        throws SignatureNotFoundException, InvalidSignatureException {
        return validateSignature(soapMsg, soapMsg.getDocumentElement());
    }

    /**
     * Verify that a valid signature is included and that the bodyElement is signed.
     * The validity of the signer's cert is NOT verified against the local root authority.
     *
     * @param soapMsg the soap message that potentially contains a digital signature
     * @param bodyElement the signed bodyElement
     * @return the cert chain used as part of the message's signature (not checked against any authority) never null
     * @throws com.l7tech.common.security.xml.SignatureNotFoundException
     *          if no signature is found in document
     * @throws com.l7tech.common.security.xml.InvalidSignatureException
     *          if the signature is invalid, not in an expected format or is missing information
     */
    public static X509Certificate[] validateSignature(Document soapMsg, final Element bodyElement )
      throws SignatureNotFoundException, InvalidSignatureException {
        normalizeDoc(soapMsg);
        
        // find signature bodyElement
        final Element sigElement = getSignatureHeaderElement(soapMsg, bodyElement);
        if (sigElement == null) {
            throw new SignatureNotFoundException("No signature bodyElement in this document");
        }

        return validateSignature( soapMsg, bodyElement, sigElement );
    }

    /**
     * Will extract the X509 certificates from the KeyInfo element.
     */
    private static X509Certificate[] getCertFromKeyInfo(Element keyInfoElement) throws InvalidSignatureException{
        KeyInfo keyInfo = null;
        try {
            keyInfo = new KeyInfo(keyInfoElement);
        } catch (XSignatureException e) {
            throw new InvalidSignatureException("Unable to extract KeyInfo from signature", e);
        }

        // THE CERT CAN BE DIRECTLY IN THE KEY INFO ELEMENT OR THE KEY INFO CAN CONTAIN
        // A REFERENCE TO A wsse:BinarySecurityToken. WE MUST SUPPORT BOTH CASES
        KeyInfo.X509Data[] x509DataArray = keyInfo.getX509Data();
        // according to javadoc, this can be null
        if (x509DataArray != null && x509DataArray.length > 0) {
            KeyInfo.X509Data x509Data = x509DataArray[0];
            X509Certificate[] certs = x509Data.getCertificates();
            // according to javadoc, this can be null
            if (certs == null || certs.length < 1) {
                throw new InvalidSignatureException("Could not get X509 cert");
            }
            return certs;
        } else {
            // Looking for reference to a wsse:BinarySecurityToken
            // 1. look for a wsse:SecurityTokenReference element
            List secTokReferences = XmlUtil.findChildElementsByName(keyInfoElement,
                                                              new String[] {SoapUtil.SECURITY_NAMESPACE,
                                                                            SoapUtil.SECURITY_NAMESPACE2,
                                                                            SoapUtil.SECURITY_NAMESPACE3},
                                                              SEC_TOK_REF_NAME);
            if (secTokReferences.size() > 0) {
                // 2. Resolve the child reference
                Element securityTokenReference = (Element)secTokReferences.get(0);
                List references = XmlUtil.findChildElementsByName(securityTokenReference,
                                                                  new String[] {SoapUtil.SECURITY_NAMESPACE,
                                                                                SoapUtil.SECURITY_NAMESPACE2,
                                                                                SoapUtil.SECURITY_NAMESPACE3},
                                                                  WSSE_REF_NAME);
                if (references.size() > 0) {
                    // get the URI
                    Element reference = (Element)references.get(0);
                    String uriAttr = reference.getAttribute("URI");
                    if (uriAttr == null || uriAttr.length() < 1) {
                        // not the food additive
                        String msg = "The Key info contains a reference but the URI attribute cannot be obtained";
                        logger.warning(msg);
                        throw new InvalidSignatureException(msg);
                    }
                    if (uriAttr.charAt(0) == '#') {
                        uriAttr = uriAttr.substring(1);
                    }
                    // resolve the element based on the URI
                    Element referencedElement = SoapUtil.getElementById(keyInfoElement.getOwnerDocument(), uriAttr);
                    if (referencedElement == null) {
                        // not the food additive
                        String msg = "The reference could not be resolved using the URI:" + uriAttr;
                        logger.warning(msg);
                        throw new InvalidSignatureException(msg);
                    }
                    // we got the referenced element. see if we support it
                    if (referencedElement.getLocalName().equals(BINSECTOKEN_NAME)) {
                        // assume that this is a b64ed binary x509 cert, get the value
                        String value = XmlUtil.getTextValue(referencedElement);
                        if (value == null || value.length() < 1) {
                            String msg = "The " + BINSECTOKEN_NAME + " does not contain a value.";
                            logger.warning(msg);
                            throw new InvalidSignatureException(msg);
                        }
                        BASE64Decoder decoder = new BASE64Decoder();
                        byte[] decodedValue = null;
                        try {
                            decodedValue = decoder.decodeBuffer(value);
                        } catch (IOException e) {
                            throw new InvalidSignatureException("could not decode value in the " + BINSECTOKEN_NAME +
                                                                " element", e);
                        }
                        // create the x509 binary cert based on it
                        X509Certificate referencedCert = null;
                        try {
                            CertificateFactory factory = CertificateFactory.getInstance("X.509");
                            InputStream is = new ByteArrayInputStream(decodedValue);
                            referencedCert = (X509Certificate)factory.generateCertificate(is);
                        } catch (CertificateException e) {
                            throw new InvalidSignatureException("could not produce a cert from value in " +
                                                                BINSECTOKEN_NAME + " element", e);
                        }
                        return new X509Certificate[] {referencedCert};
                    } else {
                        logger.warning("A reference was resolved from the KeyInfo but the " +
                                       "element type is not supported" + referencedElement.getNodeName());
                    }
                }
            }
        }
        throw new InvalidSignatureException("No cert found in key info.");
    }

    /**
     * Verify that a valid signature is included and that the bodyElement is signed.
     * The signature is verified using the shared key. Signature method is HMAC-SHA1.
     *
     * @param soapMsg the soap message that potentially contains a digital signature
     * @param bodyElement the signed bodyElement
     * @throws com.l7tech.common.security.xml.SignatureNotFoundException
     *          if no signature is found in document
     * @throws com.l7tech.common.security.xml.InvalidSignatureException
     *          if the signature is invalid, not in an expected format or is missing information
     */
    public static void validateSignature(Document soapMsg, final Element bodyElement, Key key)
            throws SignatureNotFoundException, InvalidSignatureException {
        normalizeDoc(soapMsg);

        // find signature bodyElement
        final Element sigElement = getSignatureHeaderElement(soapMsg, bodyElement);
        if (sigElement == null) {
            throw new SignatureNotFoundException("No signature bodyElement in this document");
        }

        validateSignature(soapMsg, bodyElement, sigElement, key);
    }

    /**
     * Verify that a valid signature is included and that the bodyElement is signed.
     * The signature is verified using the shared key. Signature method is HMAC-SHA1.
     * Caller is expected to have already called {@link #normalizeDoc(org.w3c.dom.Document)}.
     *
     * @param soapMsg the soap message that potentially contains a digital signature
     * @param bodyElement the signed bodyElement
     * @param sigElement the Signature element from the SOAP Security Header
     * @throws com.l7tech.common.security.xml.SignatureNotFoundException
     *          if no signature is found in document
     * @throws com.l7tech.common.security.xml.InvalidSignatureException
     *          if the signature is invalid, not in an expected format or is missing information
     */
    public static void validateSignature(Document soapMsg,
                                         final Element bodyElement,
                                         final Element sigElement,
                                         Key key)
            throws SignatureNotFoundException, InvalidSignatureException {

        SignatureContext sigContext = new SignatureContext();

        sigContext.setIDResolver(new IDResolver() {
            public Element resolveID(Document doc, String s) {
                return SoapUtil.getElementById(doc, s);
            }
        });

        // validate signature
        Validity validity = sigContext.verify(sigElement, key);

        if (!validity.getCoreValidity()) {
            throw new InvalidSignatureException("Validity not achieved: " + validity.getSignedInfoMessage() +
                    ": " + validity.getReferenceMessage(0));
        }

        // verify that the entire envelope is signed
        String refid = SoapUtil.getElementId(bodyElement);
        if (refid == null || refid.length() < 1) {
            throw new InvalidSignatureException("No reference id on envelope");
        }
        String envelopeURI = "#" + refid;
        for (int i = 0; i < validity.getNumberOfReferences(); i++) {
            if (!validity.getReferenceValidity(i)) {
                throw new InvalidSignatureException("Validity not achieved for bodyElement " + validity.getReferenceURI(i));
            }
            if (envelopeURI.equals(validity.getReferenceURI(i))) {
                // SUCCESS, RETURN THE CERT
                // first, consume the signature bodyElement by removing it
                sigElement.getParentNode().removeChild(sigElement);
                return;
            }
        }
        // if we get here, the envelope uri reference was not verified
        throw new InvalidSignatureException("No reference to envelope was verified.");
    }

    /**
     * Verify that a valid signature is included and that the bodyElement is signed.
     * The validity of the signer's cert is NOT verified against the local root authority.
     * Caller is expected to have already called {@link #normalizeDoc(org.w3c.dom.Document)}.
     *
     * @param soapMsg the soap message that potentially contains a digital signature
     * @param bodyElement the signed bodyElement
     * @param sigElement the Signature element from the SOAP Security Header
     * @return the cert chain used as part of the message's signature (not checked against any authority) never null
     * @throws com.l7tech.common.security.xml.SignatureNotFoundException
     *          if no signature is found in document
     * @throws com.l7tech.common.security.xml.InvalidSignatureException
     *          if the signature is invalid, not in an expected format or is missing information
     */
    public static X509Certificate[] validateSignature(Document soapMsg,
                                                      final Element bodyElement,
                                                      final Element sigElement)
      throws SignatureNotFoundException, InvalidSignatureException {
        SignatureContext sigContext = new SignatureContext();
        sigContext.setIDResolver(new IDResolver() {
                                                       public Element resolveID(Document doc, String s) {
                                                           return SoapUtil.getElementById(doc, s);
                                                       }
                                                   });


        // Find KeyInfo element, and extract certificate from this
        Element keyInfoElement = KeyInfo.searchForKeyInfo(sigElement);
        if (keyInfoElement == null) {
            throw new SignatureNotFoundException("KeyInfo bodyElement not found in " + sigElement.toString());
        }
        X509Certificate[] certs = getCertFromKeyInfo(keyInfoElement);
        // validate signature
        PublicKey pubKey = certs[0].getPublicKey();
        Validity validity = sigContext.verify(sigElement, pubKey);

        if (!validity.getCoreValidity()) {
            throw new InvalidSignatureException("Validity not achieved: " + validity.getSignedInfoMessage() +
                                                ": " + validity.getReferenceMessage( 0 ) );
        }

        // verify that the entire envelope is signed
        String refid = SoapUtil.getElementId(bodyElement);
        if (refid == null || refid.length() < 1) {
            throw new InvalidSignatureException("No reference id on envelope");
        }
        String envelopeURI = "#" + refid;
        for (int i = 0; i < validity.getNumberOfReferences(); i++) {
            if (!validity.getReferenceValidity(i)) {
                throw new InvalidSignatureException("Validity not achieved for bodyElement " + validity.getReferenceURI(i));
            }
            if (envelopeURI.equals(validity.getReferenceURI(i))) {
                // SUCCESS, RETURN THE CERT
                // first, consume the signature bodyElement by removing it
                sigElement.getParentNode().removeChild(sigElement);
                return certs;
            }
        }
        // if we get here, the envelope uri reference was not verified
        throw new InvalidSignatureException("No reference to envelope was verified.");
    }

    private static Element getSignatureHeaderElement(Document doc, Element bodyElement) {
        String bodyId = SoapUtil.getElementId(bodyElement);
        if (bodyId == null) {
            logger.info("ID attribute not found in supposedly signed body element " + bodyElement.getNodeName());
            return null;
        }

        try {
            Element security = SoapUtil.getSecurityElement(doc);
            if ( security == null ) {
                logger.info( SoapUtil.SECURITY_EL_NAME + " header not found" );
                return null;
            }

            // find signature element(s)
            List signatureElements = XmlUtil.findChildElementsByName( security, SoapUtil.DIGSIG_URI, SoapUtil.SIGNATURE_EL_NAME );
            if (signatureElements.size() < 1) {
                logger.info( "No " + SoapUtil.SIGNATURE_EL_NAME + " elements were found in " + SoapUtil.SECURITY_EL_NAME + " header" );
                return null;
            }

            // Find signature element matching the specified bodyElement
            for ( Iterator i = signatureElements.iterator(); i.hasNext(); ) {
                Element signature = (Element) i.next();
                Element signedInfo = XmlUtil.findOnlyOneChildElementByName( signature,
                                                                            SoapUtil.DIGSIG_URI,
                                                                            SoapUtil.SIGNED_INFO_EL_NAME );
                List references = XmlUtil.findChildElementsByName(signedInfo, SoapUtil.DIGSIG_URI,
                                                                  SoapUtil.REFERENCE_EL_NAME);

                /*Element reference = XmlUtil.findOnlyOneChildElementByName( signedInfo,
                                                                           SoapUtil.DIGSIG_URI,
                                                                           SoapUtil.REFERENCE_EL_NAME );*/

                for (Iterator j = references.iterator(); j.hasNext();) {
                    Element reference = (Element)j.next();
                    String uri = reference.getAttribute( SoapUtil.REFERENCE_URI_ATTR_NAME );
                    if ( uri == null || !uri.startsWith("#") || uri.length() < 2 ) {
                        logger.warning( "SignedInfo/Reference/URI is missing or points to non-local body part" );
                        return null;
                    }

                    if ( uri.substring(1).equals(bodyId) )
                        return signature;
                }
            }

            logger.finest( "Did not find any matching Signature element" );
            return null;
        } catch ( XmlUtil.MultipleChildElementsException e ) {
            logger.warning( "Found multiple " + e.getName() + " elements where only one was expected" );
            return null;
        }
    }

    private static String getSignatureMethod(PrivateKey privateKey) {
        String signaturemethod;
        if (privateKey instanceof RSAPrivateKey)
            signaturemethod = SignatureMethod.RSA;
        else if (privateKey instanceof DSAPrivateKey)
            signaturemethod = SignatureMethod.DSA;
        else {
            throw new IllegalArgumentException("Unsupported private key type: " + privateKey.getClass().getName());
        }
        return signaturemethod;
    }


    /**
     * Document normalization is necessary because the serialization / deserialization process between the
     * ssg and the CP causes \n characters to be inserted in certain text nodes that contain space char
     * as well as empty text nodes being created all over the place.
     */
    public static void normalizeDoc(Document doc) {
        // fla note, IBM's Normalizer.normalize is useless
        filterOutEmptyTextNodesAndNormalizeStrings(doc.getDocumentElement());
    }

    /**
     * See normalizeDoc
     */
    private static void filterOutEmptyTextNodesAndNormalizeStrings(Element el) {
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            // remove empty text nodes
            if (node.getNodeType() == Node.TEXT_NODE) {
                String val = node.getNodeValue();
                boolean legitNode = false;
                for (int j = 0; j < val.length(); j++) {
                    char c = val.charAt(j);
                    if (isCharSpace(c)) continue;
                    // a non-empty character was found, leave this node alone (should we trim the value?)
                    legitNode = true;
                    break;
                }
                if (!legitNode) {
                    el.removeChild(node);
                    filterOutEmptyTextNodesAndNormalizeStrings(el);
                } else {
                    normalizeTextNodeValue((Text)node);
                }
            } else if (node.getNodeType() == Node.ELEMENT_NODE) {
                filterOutEmptyTextNodesAndNormalizeStrings((Element)node);
            }
        }
    }

    /**
     * See normalizeDoc
     */
    private static void normalizeTextNodeValue(Text node) {
        String originalVal = node.getNodeValue();
        for (int i = 0; i < originalVal.length(); i++) {
            char c = originalVal.charAt(i);
            if (isCharSpace(c)) {
                int begin = i;
                int end = -1; // the first non space char
                for (int j = i + 1; j < originalVal.length(); j++) {
                    char c2 = originalVal.charAt(j);
                    if (!isCharSpace(c2)) {
                        end = j;
                        break;
                    }
                }
                // trailing spaces at end of string
                if (end == -1) {
                    originalVal = originalVal.substring(0, begin);
                    // we're done
                    break;
                } else if ((end - begin) > 1) {
                    // either a space prefix
                    if (begin == 0) {
                        originalVal = originalVal.substring(end);
                    } else { // or a space gap
                        String newVal = originalVal.substring(0, begin) + ' ' + originalVal.substring(end, originalVal.length());
                        originalVal = newVal;
                    }
                }
                // this is a lonely space
                // check if it's first thing
                if (begin == 0)
                    originalVal = originalVal.substring(1);
                // make sure it hasn't been replaced by a \n or something like that
                else if (c != ' ') originalVal = originalVal.replace(c, ' ');
            }
        }
        node.setNodeValue(originalVal);
    }

    private static boolean isCharSpace(char c) {
        if (c == ' ' || c == '\n' || c == '\t' || c == '\r') return true;
        return false;
    }

    // Use a logger that will work inside either the Agent or the Gateway.
    private static final Logger logger = Logger.getLogger(SoapMsgSigner.class.getName());
}
