package com.l7tech.external.assertions.saml2attributequery.server;

import com.ibm.dom.util.IndentConfig;
import com.ibm.xml.dsig.*;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.DsigUtil;
import com.l7tech.security.xml.KeyInfoDetails;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.NamespaceFactory;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.TooManyChildElementsException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

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

    public static void signSamlpRequest(final Document samlpRequest,
                                        final Element samlpRootElement,
                                        final PrivateKey signingKey,
                                        final X509Certificate[] signingCertChain,
                                        final KeyInfoInclusionType keyInfoType)
            throws SignatureException
    {
        TemplateGenerator template =
                new TemplateGenerator(samlpRequest, XSignature.SHA1, Canonicalizer.EXCLUSIVE, SignatureMethod.RSA);

        String idAttr = "ID";

        final String id = samlpRootElement.getAttribute(idAttr);
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
                    return samlpRootElement;
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

        // Insert before saml2:Subject if possible, otherwise before the first child element
        Element sigSibling = null;
        try {
            sigSibling = XmlUtil.findOnlyOneChildElementByName(samlpRootElement,
                    SamlConstants.NS_SAML2,
                    "Subject");
        }
        catch(TooManyChildElementsException tmcee) {
            throw new IllegalArgumentException("Invalid SAML Assertion (multiple Issuers)");
        }

        if(sigSibling == null) {
            Node n = samlpRootElement.getFirstChild();
            while(n != null && n.getNodeType() != Node.ELEMENT_NODE) {
                n = n.getNextSibling();
            }
            sigSibling = (Element)n;
            //throw new IllegalArgumentException("Invalid SAML Assertion (no Issuer)");
        }

        // create the signature key info
        signatureElement.appendChild(createKeyInfo(samlpRequest, signingCertChain, keyInfoType));

        try {
            context.sign(signatureElement, signingKey);
        } catch (XSignatureException e) {
            throw new SignatureException(e.getMessage());
        }

        samlpRootElement.insertBefore(signatureElement, sigSibling);
    }


    private static Element createKeyInfo(final Document docSigned,
                                           final X509Certificate[] signingCertChain,
                                           final KeyInfoInclusionType keyInfoType)
        throws SignatureException
    {
        Element keyInfoElement = null;

        if(keyInfoType == KeyInfoInclusionType.STR_SKI) {
            keyInfoElement = docSigned.createElementNS(SoapUtil.DIGSIG_URI, "KeyInfo");
            Element x509Data = docSigned.createElementNS(SoapUtil.DIGSIG_URI, "X509Data");
            keyInfoElement.appendChild(x509Data);

            Element skiElement = docSigned.createElementNS(SoapUtil.DIGSIG_URI, "X509SKI");
            skiElement.setTextContent(CertUtils.getSki(signingCertChain[0]));
            x509Data.appendChild(skiElement);
        } else if(keyInfoType == KeyInfoInclusionType.CERT) {
            KeyInfo.X509Data x509 = new KeyInfo.X509Data();
            x509.setCertificate(signingCertChain[0]);
            x509.setParameters(signingCertChain[0], false, true, false);
            KeyInfo keyInfo = new KeyInfo();
            keyInfo.setX509Data(new KeyInfo.X509Data[] {x509});
            keyInfoElement = keyInfo.getKeyInfoElement(docSigned, nullIndentConfig);
        } else if(keyInfoType == KeyInfoInclusionType.STR_THUMBPRINT) {
            KeyInfo keyInfo = new KeyInfo();
            keyInfoElement = keyInfo.getKeyInfoElement(docSigned, nullIndentConfig);
            // Replace cert with STR?
            try {
                String thumb = CertUtils.getThumbprintSHA1(signingCertChain[0]);
                KeyInfoDetails.makeKeyId(thumb, true, SoapUtil.VALUETYPE_X509_THUMB_SHA1).
                        populateExistingKeyInfoElement(new NamespaceFactory(), keyInfoElement);
            } catch (Exception e) {
                throw new SignatureException(e);
            }
        }

        keyInfoElement.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns", SoapUtil.DIGSIG_URI);

        return keyInfoElement;
    }

}