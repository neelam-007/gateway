package com.l7tech.xmlsig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.cert.X509Certificate;
import com.ibm.xml.dsig.util.AdHocIDResolver;
import com.ibm.xml.dsig.*;

/**
 * User: flascell
 * Date: Aug 19, 2003
 * Time: 2:44:50 PM
 * $Id$
 *
 * Signs soap messages.
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

        // Signature is applied in header, as per WS-S
        Element headerEl = getOrMakeHeader(soapMsg);
        Element envelopedSigEl = (Element) headerEl.appendChild(sigEl);

        // Include KeyInfo element in signature and embed cert into subordinate X509Data element
        KeyInfo keyInfo = new KeyInfo();
        KeyInfo.X509Data x509Data = new KeyInfo.X509Data();
        x509Data.setCertificate(cert);
        x509Data.setParameters(cert, true, true, true);
        keyInfo.setX509Data(new KeyInfo.X509Data[]{x509Data});
        keyInfo.insertTo(envelopedSigEl, DS_PREFIX);

        // Setup context and sign document
        SignatureContext sigContext = new SignatureContext();
        AdHocIDResolver idResolver = new AdHocIDResolver(soapMsg);
        sigContext.setIDResolver(idResolver);

        sigContext.sign(envelopedSigEl, privateKey);
    }

    // todo, move this to util class
    private Element getOrMakeHeader(Document soapMsg) {
        // use the soap flavor of this document
        String soapEnvNS = soapMsg.getDocumentElement().getNamespaceURI();
        NodeList list = soapMsg.getElementsByTagNameNS(soapEnvNS, HEADER_EL_NAME);
        if (list.getLength() < 1) {
            String soapEnvNamespacePrefix = soapMsg.getDocumentElement().getPrefix();
            Element header = soapMsg.createElementNS(soapEnvNS, HEADER_EL_NAME);
            Element body = getOrMakeBody(soapMsg);
            header.setPrefix(soapEnvNamespacePrefix);
            soapMsg.getDocumentElement().insertBefore(header, body);
            return header;
        }
        else return (Element)list.item(0);
    }

    // todo, move this to util class
    private Element getOrMakeBody(Document soapMsg) {
        // use the soap flavor of this document
        String soapEnvNS = soapMsg.getDocumentElement().getNamespaceURI();
        NodeList list = soapMsg.getElementsByTagNameNS(soapEnvNS, BODY_EL_NAME);
        if (list.getLength() < 1) {
            String soapEnvNamespacePrefix = soapMsg.getDocumentElement().getPrefix();
            Element body = soapMsg.createElementNS(soapEnvNS, BODY_EL_NAME);
            body.setPrefix(soapEnvNamespacePrefix);
            soapMsg.getDocumentElement().appendChild(body);
            return body;
        }
        else return (Element)list.item(0);
    }

    private static final String DS_PREFIX = "ds";
    private static final String DEF_ENV_TAG = "envId";
    private static final String HEADER_EL_NAME = "Header";
    private static final String BODY_EL_NAME = "Body";
}
