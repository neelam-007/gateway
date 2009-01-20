package com.l7tech.external.assertions.samlpassertion.server;

import com.ibm.dom.util.IndentConfig;
import com.ibm.xml.dsig.*;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.io.CertUtils;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.DsigUtil;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.KeyInfoDetails;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.TooManyChildElementsException;
import com.l7tech.util.NamespaceFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;

/**
 * User: vchan
 */
public class RequestSigner {

    private static final IndentConfig nullIndentConfig = new IndentConfig() {
        public boolean doIndentation() {
            return false;
        }

        public int getUnit() {
            return 0;
        }
    };

    public static void signSamlpRequest(final Integer samlVersion,
                                        final Document samlpRequest,
                                        final PrivateKey signingKey,
                                        final X509Certificate[] signingCertChain,
                                        final KeyInfoInclusionType keyInfoType)
            throws SignatureException
    {
        TemplateGenerator template =
                new TemplateGenerator(samlpRequest, XSignature.SHA1, Canonicalizer.EXCLUSIVE, SignatureMethod.RSA);

        String idAttr = (samlVersion == 1 ? "RequestID" : "ID");

        final String id = samlpRequest.getDocumentElement().getAttribute(idAttr);
        template.setPrefix("ds");
        template.setIndentation(false);
        Reference ref = template.createReference("#" + id);
        ref.addTransform(Transform.ENVELOPED);
        ref.addTransform(Transform.C14N_EXCLUSIVE);
        template.addReference(ref);

        SignatureContext context = new SignatureContext();
        context.setEntityResolver(XmlUtil.getXss4jEntityResolver());
        context.setIDResolver(new IDResolver() {
            public Element resolveID(Document document, String s) {
                if (id.equals(s))
                    return samlpRequest.getDocumentElement();
                else
                    throw new IllegalArgumentException("I don't know how to find " + s);
            }
        });

        final Element signatureElement = template.getSignatureElement();
        // Ensure that CanonicalizationMethod has required c14n subelement
        Element signedInfoElement = template.getSignedInfoElement();
        Element c14nMethod = XmlUtil.findFirstChildElementByName(signedInfoElement,
                                                                 SoapUtil.DIGSIG_URI,
                                                                 "CanonicalizationMethod");
        DsigUtil.addInclusiveNamespacesToElement(c14nMethod);

        // Ensure that any Transform has required c14n subelement
        NodeList transforms = signedInfoElement.getElementsByTagNameNS(signedInfoElement.getNamespaceURI(), "Transform");
        for (int i = 0; i < transforms.getLength(); ++i)
            if (Transform.C14N_EXCLUSIVE.equals(((Element)transforms.item(i)).getAttribute("Algorithm")))
                DsigUtil.addInclusiveNamespacesToElement((Element)transforms.item(i));

        if (samlVersion == 1) {
            samlpRequest.getDocumentElement().insertBefore(
                    signatureElement,
                    XmlUtil.findFirstChildElement(samlpRequest.getDocumentElement()));

        } else {
            try {
                Element docElement = samlpRequest.getDocumentElement();
                Element sigSibling = XmlUtil.findOnlyOneChildElementByName(docElement,
                        SamlConstants.NS_SAML2,
                        SamlConstants.ELEMENT_ISSUER);
                if (sigSibling == null)
                    throw new IllegalArgumentException("Invalid SAML Assertion (no Issuer)");

                docElement.insertBefore(signatureElement, XmlUtil.findNextElementSibling(sigSibling));
            }
            catch(TooManyChildElementsException tmcee) {
                throw new IllegalArgumentException("Invalid SAML Assertion (multiple Issuers)");
            }
        }

        // create the signature key info
        signatureElement.appendChild(createKeyInfo(samlpRequest, signingCertChain, keyInfoType));

        try {
            context.sign(signatureElement, signingKey);
        } catch (XSignatureException e) {
            throw new SignatureException(e.getMessage());
        }
    }


    private static Element createKeyInfo(final Document docSigned,
                                           final X509Certificate[] signingCertChain,
                                           final KeyInfoInclusionType keyInfoType)
        throws SignatureException
    {
        Element keyInfoElement;
        KeyInfo keyInfo = new KeyInfo();
        switch(keyInfoType) {
            case STR_THUMBPRINT:
                keyInfoElement = keyInfo.getKeyInfoElement(docSigned, nullIndentConfig);
                // Replace cert with STR?
                try {
                    String thumb = CertUtils.getThumbprintSHA1(signingCertChain[0]);
                    KeyInfoDetails.makeKeyId(thumb, true, SoapUtil.VALUETYPE_X509_THUMB_SHA1).
                            populateExistingKeyInfoElement(new NamespaceFactory(), keyInfoElement);
                } catch (Exception e) {
                    throw new SignatureException(e);
                }
                break;
            case STR_SKI:
                keyInfoElement = keyInfo.getKeyInfoElement(docSigned, nullIndentConfig);
                // Replace cert with STR?
                try {
                    String thumb = CertUtils.getSki(signingCertChain[0]);
                    KeyInfoDetails.makeKeyId(thumb, true, SoapUtil.VALUETYPE_SKI).
                            populateExistingKeyInfoElement(new NamespaceFactory(), keyInfoElement);
                } catch (Exception e) {
                    throw new SignatureException(e);
                }
                break;
            case CERT:
                KeyInfo.X509Data x509 = new KeyInfo.X509Data();
                x509.setCertificate(signingCertChain[0]);
                x509.setParameters(signingCertChain[0], false, false, true);
                keyInfo.setX509Data(new KeyInfo.X509Data[]{x509});
                keyInfoElement = keyInfo.getKeyInfoElement(docSigned, nullIndentConfig);
                break;
            case NONE:
            default:
                throw new IllegalArgumentException("KeyInfoType must be CERT, STR_THUMBPRINT or STR_SKI");
        }

        keyInfoElement.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns", SoapUtil.DIGSIG_URI);

        return keyInfoElement;
    }

}
