package com.l7tech.common.security.xml;

import com.ibm.xml.dsig.*;
import com.ibm.xml.dsig.util.AdHocIDResolver;
import com.l7tech.common.util.SoapUtil;
import org.w3c.dom.*;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPrivateKey;

/**
 * User: flascell
 * Date: Aug 19, 2003
 * Time: 2:44:50 PM
 * $Id$
 * <p/>
 * Signs soap messages.
 * Meant to be used by both server-side and proxy-side dsig assertions
 */
public class SoapMsgSigner {

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
     * @param cert       the signer's cert
     * @throws com.ibm.xml.dsig.SignatureStructureException
     *          
     * @throws com.ibm.xml.dsig.XSignatureException
     *          
     */
    public void signEnvelope(Document soapMsg, PrivateKey privateKey, X509Certificate cert)
      throws SignatureStructureException, XSignatureException {
        // is the envelope already ided?
        String id = soapMsg.getDocumentElement().getAttribute("Id");

        if (id == null || id.length() < 1) {
            id = DEF_ENV_TAG;
        }
        signElement(soapMsg, soapMsg.getDocumentElement(), id, privateKey, cert);
    }

    /**
     * Sign the document element using the private key and Embed the <code>X509Certificate</code> into
     * the XML document.
     * 
     * @param document    the xml document containing the element to sign.
     * @param elem        the document element
     * @param referenceId the signature reference ID attreibute value
     * @param privateKey  the private key of the signer if imlpements RSAPrivateKey signature method will be
     *                    http://www.w3.org/2000/09/xmldsig#rsa-sha1, if privateKey implements DSAPrivateKey, signature method will be
     *                    http://www.w3.org/2000/09/xmldsig#dsa-sha1.
     * @param cert        the signer's cert
     * @throws com.ibm.xml.dsig.SignatureStructureException
     *                                  
     * @throws com.ibm.xml.dsig.XSignatureException
     *                                  
     * @throws IllegalArgumentException if any of the parameters i <b>null</b>
     */
    public void signElement(Document document, Element elem, String referenceId, PrivateKey privateKey, X509Certificate cert)
      throws SignatureStructureException, XSignatureException {

        if (document == null || elem == null | referenceId == null ||
          privateKey == null || cert == null) {
            throw new IllegalArgumentException();
        }

        String id = elem.getAttribute(referenceId);
        if (id == null || "".equals(id)) {
            id = referenceId;
            elem.setAttribute("Id", referenceId);
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
        x509Data.setCertificate(cert);
        x509Data.setParameters(cert, true, true, true);
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
       * @return the cert used as part of the message's signature (not checked against any authority) never null
       * @throws com.l7tech.common.security.xml.SignatureNotFoundException
       *          if no signature is found in document
       * @throws com.l7tech.common.security.xml.InvalidSignatureException
       *          if the signature is invalid, not in an expected format or is missing information
       */
      public X509Certificate validateSignature(Document soapMsg)
        throws SignatureNotFoundException, InvalidSignatureException {
        return validateSignature(soapMsg, soapMsg.getDocumentElement());
    }

    /**
     * Verify that a valid signature is included and that the element is signed.
     * The validity of the signer's cert is NOT verified against the local root authority.
     * 
     * @param soapMsg the soap message that potentially contains a digital signature
     * @param element the signed element
     * @return the cert used as part of the message's signature (not checked against any authority) never null
     * @throws com.l7tech.common.security.xml.SignatureNotFoundException
     *          if no signature is found in document
     * @throws com.l7tech.common.security.xml.InvalidSignatureException
     *          if the signature is invalid, not in an expected format or is missing information
     */
    public X509Certificate validateSignature(Document soapMsg, Element element)
      throws SignatureNotFoundException, InvalidSignatureException {
        normalizeDoc(soapMsg);

        // find signature element
        Element sigElement = getSignatureHeaderElement(soapMsg);
        if (sigElement == null) {
            throw new SignatureNotFoundException("No signature element in this document");
        }

        SignatureContext sigContext = new SignatureContext();
        AdHocIDResolver idResolver = new AdHocIDResolver(soapMsg);
        sigContext.setIDResolver(idResolver);

        // Find KeyInfo element, and extract certificate from this
        Element keyInfoElement = KeyInfo.searchForKeyInfo(sigElement);
        if (keyInfoElement == null) {
            throw new SignatureNotFoundException("KeyInfo element not found in " + sigElement.toString());
        }
        KeyInfo keyInfo = null;
        try {
            keyInfo = new KeyInfo(keyInfoElement);
        } catch (XSignatureException e) {
            throw new InvalidSignatureException("Unable to extract KeyInfo from signature", e);
        }

        // Assume a single X509 certificate
        KeyInfo.X509Data[] x509DataArray = keyInfo.getX509Data();
        // according to javadoc, this can be null
        if (x509DataArray == null || x509DataArray.length < 1) {
            throw new InvalidSignatureException("No x509 data found in KeyInfo element");
        }
        KeyInfo.X509Data x509Data = x509DataArray[0];
        X509Certificate[] certs = x509Data.getCertificates();
        // according to javadoc, this can be null
        if (certs == null || certs.length < 1) {
            throw new InvalidSignatureException("Could not get X509 cert");
        }
        X509Certificate cert = certs[0];

        // validate signature
        PublicKey pubKey = cert.getPublicKey();
        Validity validity = sigContext.verify(sigElement, pubKey);

        if (!validity.getCoreValidity()) {
            throw new InvalidSignatureException("Validity not achieved: " + validity.getSignedInfoMessage());
        }

        // verify that the entire envelope is signed
        String refid = element.getAttribute("Id");
        if (refid == null || refid.length() < 1) {
            throw new InvalidSignatureException("No reference id on envelope");
        }
        String envelopeURI = "#" + refid;
        for (int i = 0; i < validity.getNumberOfReferences(); i++) {
            if (!validity.getReferenceValidity(i)) {
                throw new InvalidSignatureException("Validity not achieved for element " + validity.getReferenceURI(i));
            }
            if (envelopeURI.equals(validity.getReferenceURI(i))) {
                // SUCCESS, RETURN THE CERT
                // first, consume the signature element by removing it
                sigElement.getParentNode().removeChild(sigElement);
                return cert;
            }
        }
        // if we get here, the envelope uri reference was not verified
        throw new InvalidSignatureException("No reference to envelope was verified.");
    }


    public Element getSignatureHeaderElement(Document doc) {
        // find signature element
        NodeList tmpNodeList = doc.getElementsByTagNameNS(XSignature.XMLDSIG_NAMESPACE, "Signature");
        if (tmpNodeList.getLength() < 1) {
            return null;
        }
        return (Element)tmpNodeList.item(0);
    }

    private String getSignatureMethod(PrivateKey privateKey) {
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
    private void normalizeDoc(Document doc) {
        // fla note, IBM's Normalizer.normalize is useless
        filterOutEmptyTextNodesAndNormalizeStrings(doc.getDocumentElement());
    }

    /**
     * See normalizeDoc
     */
    private void filterOutEmptyTextNodesAndNormalizeStrings(Element el) {
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
    private void normalizeTextNodeValue(Text node) {
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

    private boolean isCharSpace(char c) {
        if (c == ' ' || c == '\n' || c == '\t' || c == '\r') return true;
        return false;
    }

    private static final String DS_PREFIX = "ds";
    private static final String DEF_ENV_TAG = "envId";
}
