package com.l7tech.xmlsig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.cert.X509Certificate;
import com.ibm.xml.dsig.util.AdHocIDResolver;
import com.ibm.xml.dsig.*;
import com.l7tech.util.SoapUtil;

/**
 * User: flascell
 * Date: Aug 19, 2003
 * Time: 2:44:50 PM
 * $Id$
 *
 * Signs soap messages.
 * Meant to be used by both server-side and proxy-side dsig assertions
 */
public class SoapMsgSigner {

    /**
     * Appends a soap message with a digital signature of it's entire envelope.
     *
     * If the envelope already has as Id attribute, it's value will be used to refer to the envelope within the
     * SignedInfo element. Otherwise, an Id of value DEF_ENV_TAG will be used.
     *
     * @param soapMsg the xml document containing the soap message expected to contain at least a soapenvelope element.
     * this document contains the signature when at return time.
     *
     * @param privateKey the private key of the signer if imlpements RSAPrivateKey signature method will be
     * http://www.w3.org/2000/09/xmldsig#rsa-sha1, if privateKey implements DSAPrivateKey, signature method will be
     * http://www.w3.org/2000/09/xmldsig#dsa-sha1.
     *
     * @param cert the signer's cert
     *
     * @throws com.ibm.xml.dsig.SignatureStructureException
     * @throws com.ibm.xml.dsig.XSignatureException
     */
    public void signEnvelope(Document soapMsg, PrivateKey privateKey, X509Certificate cert) throws SignatureStructureException, XSignatureException {
        // is the envelope already ided?
        String id = soapMsg.getDocumentElement().getAttribute("Id");
        if (id == null || id.length() < 1) {
            id = DEF_ENV_TAG;
            soapMsg.getDocumentElement().setAttribute("Id", id);
        }

        // set the appropriate signature method
        String signaturemethod = "";
        if (privateKey instanceof RSAPrivateKey) signaturemethod = SignatureMethod.RSA;
        else if (privateKey instanceof DSAPrivateKey) signaturemethod = SignatureMethod.DSA;
        else {
            throw new IllegalArgumentException("Unsupported private key type: " + privateKey.getClass().getName());
        }

        // Create signature template and populate with appropriate transforms. Reference is to SOAP Envelope
        TemplateGenerator template = new TemplateGenerator(soapMsg, XSignature.SHA1, Canonicalizer.W3C2, signaturemethod);
        template.setPrefix(DS_PREFIX);
        Reference ref = template.createReference("#" + id);
        ref.addTransform(Transform.ENVELOPED);
        ref.addTransform(Transform.W3CC14N2);
        template.addReference(ref);
        Element sigEl = template.getSignatureElement();

        // Signature is inserted in Header/Security, as per WS-S
        Element securityEl = SoapUtil.getOrMakeSecurityElement(soapMsg);
        Element envelopedSigEl = (Element) securityEl.appendChild(sigEl);

        // Include KeyInfo element in signature and embed cert into subordinate X509Data element
        KeyInfo keyInfo = new KeyInfo();
        KeyInfo.X509Data x509Data = new KeyInfo.X509Data();
        x509Data.setCertificate(cert);
        x509Data.setParameters(cert, true, true, true);
        keyInfo.setX509Data(new KeyInfo.X509Data[]{x509Data});
        keyInfo.insertTo(envelopedSigEl, DS_PREFIX);

        filterOutEmptyTextNodes(soapMsg.getDocumentElement());

        // Setup context and sign document
        SignatureContext sigContext = new SignatureContext();
        AdHocIDResolver idResolver = new AdHocIDResolver(soapMsg);
        sigContext.setIDResolver(idResolver);

        sigContext.sign(envelopedSigEl, privateKey);
    }

    /**
     * Verify that a valid signature is included and that the entire envelope is signed.
     * The validity of the signer's cert is NOT verified against the local root authority.
     *
     * @param soapMsg the soap message that potentially contains a digital signature
     * @return the cert used as part of the message's signature (not checked against any authority) never null
     * @throws SignatureNotFoundException if no signature is found in document
     * @throws InvalidSignatureException if the signature is invalid, not in an expected format or is missing information
     */
    public X509Certificate validateSignature(Document soapMsg) throws SignatureNotFoundException, InvalidSignatureException, XSignatureException {

        filterOutEmptyTextNodes(soapMsg.getDocumentElement());

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
        KeyInfo keyInfo = new KeyInfo(keyInfoElement);

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
        String refid = soapMsg.getDocumentElement().getAttribute("Id");
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

    private void filterOutEmptyTextNodes(Element el) {
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            // remove empty text nodes
            if (node.getNodeType() == Node.TEXT_NODE) {
                String val = node.getNodeValue();
                boolean legitNode = false;
                for (int j = 0; j < val.length(); j++) {
                    char c = val.charAt(j);
                    if (c == ' ' || c == '\n' || c == '\t' || c == '\r') continue;
                    // a non-empty character was found, leave this node alone (should we trim the value?)
                    legitNode = true;
                    break;
                }
                if (!legitNode) {
                    el.removeChild(node);
                    filterOutEmptyTextNodes(el);
                }
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE) {
                filterOutEmptyTextNodes((Element)node);
            }
        }
    }

    private static final String DS_PREFIX = "ds";
    private static final String DEF_ENV_TAG = "envId";
    private static final String HEADER_EL_NAME = "Header";
    private static final String BODY_EL_NAME = "Body";
}
