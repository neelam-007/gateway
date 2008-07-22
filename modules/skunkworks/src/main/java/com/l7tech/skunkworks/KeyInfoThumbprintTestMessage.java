/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.skunkworks;

import com.ibm.xml.dsig.*;
import com.ibm.xml.dsig.transform.ExclusiveC11r;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.DsigUtil;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ISO8601Date;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class KeyInfoThumbprintTestMessage extends TestCase {
    private static final Logger log = Logger.getLogger(KeyInfoThumbprintTestMessage.class.getName());

    private static final class MsgInfo {
        final Document doc;
        final String encryptedKeySha1;
        final String signatureConf;
        final byte[] keyBytes;

        public MsgInfo(Document doc, String encryptedKeySha1, String signatureConf, byte[] keyBytes) {
            this.doc = doc;
            this.encryptedKeySha1 = encryptedKeySha1;
            this.signatureConf = signatureConf;
            this.keyBytes = keyBytes;
        }
    }

    public void testSki() throws Exception {
        X509Certificate cert = null;//TestDocuments.getWssInteropAliceCert();
        System.out.println(CertUtils.getSki(cert));
    }

    public MsgInfo makeTestMessage() throws Exception {

        // Take the sample request from WSS interop and turn it into one that uses certifictes that are known.

        // Complications:
        // - The sample document does signing before encryption -- that is, it has the EncryptedKey in front of
        //   the Signature in the Security header.  So we'll need to replace all the encrypted bits with plaintext,
        //   replace the signature with a new version, and then re-encrypt all the encrypted bits with the new key.

        // Start with the WSS example request
        Document d = null;//TestDocuments.getTestDocument(TestDocuments.WSS2005JUL_REQUEST_ORIG);
        Element env = d.getDocumentElement();
        Element header = DomUtils.findFirstChildElementByName(env, (String)null, "Header");
        Element secHeader = DomUtils.findFirstChildElementByName(header, (String)null, "Security");
        Element body = DomUtils.findFirstChildElementByName(env, (String)null, "Body");

        // Get the certs we're gonna use for the recipient
        X509Certificate senderCert = null;//TestDocuments.getWssInteropAliceCert();
        PrivateKey senderPrivateKey = null;//TestDocuments.getWssInteropAliceKey();

        // Remove the EncryptedKey
        Element encryptedKeyEl = DomUtils.findFirstChildElementByName(secHeader, (String)null, "EncryptedKey");
        encryptedKeyEl.getParentNode().removeChild(encryptedKeyEl);

        // Replace the encrypted payload with a plaintext one
        Element oldPayloadEl = DomUtils.findFirstChildElement(body);
        String newPayloadStr = "<tns:Ping xmlns:tns=\"http://xmlsoap.org/Ping\">Layer 7 Technologies - Scenario #8</tns:Ping>";
        Element newPayloadEl = XmlUtil.stringToDocument(newPayloadStr).getDocumentElement();
        newPayloadEl = (Element)d.importNode(newPayloadEl, true);
        body.replaceChild(newPayloadEl, oldPayloadEl);

        // Remove the UsernameToken
        Element oldUtok = DomUtils.findFirstChildElementByName(secHeader, (String)null, "EncryptedData");
        secHeader.removeChild(oldUtok);

        // Remove the EncryptedHeader
        Element encPingHdr = DomUtils.findFirstChildElementByName(header, (String)null, "EncryptedHeader");
        header.removeChild(encPingHdr);

        // Replace the Signature header with a new one made with the proper cert and using a thumbprint sha1 ref
        Element timestampEl = DomUtils.findFirstChildElementByName(secHeader, (String)null, "Timestamp");
        Element created = DomUtils.findFirstChildElementByName(timestampEl, (String)null, "Created");
        Element expires = DomUtils.findFirstChildElementByName(timestampEl, (String)null, "Expires");
        Calendar cal = Calendar.getInstance();
        created.setTextContent(ISO8601Date.format(cal.getTime())); // XXX java 1.5 DOM feature
        cal.add(Calendar.DAY_OF_MONTH, 1);
        expires.setTextContent(ISO8601Date.format(cal.getTime())); // XXX java 1.5 DOM feature

        Element[] signedEl = new Element[] {
            timestampEl,
            body,
        };
        Element newSig = makeSignature(senderPrivateKey, signedEl, secHeader);
        addKeyInfo(secHeader, newSig, CertUtils.getThumbprintSHA1(senderCert));
        newSig = (Element)d.importNode(newSig, true);
        Element oldSig = DomUtils.findFirstChildElementByName(secHeader, (String)null, "Signature");
        secHeader.replaceChild(newSig, oldSig);
        NodeList sigvals = secHeader.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "SignatureValue");
        if (sigvals.getLength() != 1) throw new IllegalStateException("Found " + sigvals.getLength() + " SignatureValue elements; expected 1");
        Element sigval = (Element)sigvals.item(0);
        final String signatureValue = DomUtils.getTextValue(sigval);

        final String newStr = XmlUtil.nodeToFormattedString(env);
        log.info("Final message (reformatted): " + newStr);
        //assertEquals(oldStr, newStr); // uncomment to see a visual comparison in IDEA of our changes

        return new MsgInfo(env.getOwnerDocument(), null, signatureValue, null);
    }

    public void testMakeTestMessage() throws Exception {
        final Document doc = makeTestMessage().doc;
        final String mstr = XmlUtil.nodeToString(doc);
        //XmlUtil.nodeToOutputStream(doc, new FileOutputStream("C:/thumbsig.xml"));
        log.info("Test message:\n" + mstr);
    }

    public void testSendTestMessage() throws Exception {
        //doSendTestMessage("http://131.107.72.15/wss11/ping", "Alice", "abcd!1234"); // Microsoft
        //doSendTestMessage("http://liberty.oracle.com:8446/webservice/webservice", "alice", "fiodi");   // Oracle
        //doSendTestMessage("http://192.35.232.217:9080/wsse11/Scenario8", "alice", "password"); // IBM
        //doSendTestMessage("http://locutus.l7tech.com:8080/xml/ping", "alice", "password"); // Layer 7
    }

    private static Element makeSignature(Key senderSigningKey,
                                  Element[] elementsToSign,
                                  Element securityHeader)
            throws DecoratorException, InvalidDocumentFormatException
    {
        if (elementsToSign == null || elementsToSign.length < 1) return null;

        // make sure all elements already have an id
        String[] signedIds = new String[elementsToSign.length];
        for (int i = 0; i < elementsToSign.length; i++) {
            signedIds[i] = getWsuId(elementsToSign[i]);
        }

        String signaturemethod = null;
        if (senderSigningKey instanceof RSAPrivateKey)
            signaturemethod = SignatureMethod.RSA;
        else if (senderSigningKey instanceof DSAPrivateKey)
            signaturemethod = SignatureMethod.DSA;
        else if (senderSigningKey instanceof SecretKey)
            signaturemethod = SignatureMethod.HMAC;
        else {
            throw new DecoratorException("Private Key type not supported " +
              senderSigningKey.getClass().getName());
        }

        // Create signature template and populate with appropriate transforms. Reference is to SOAP Envelope
        TemplateGenerator template = new TemplateGenerator(elementsToSign[0].getOwnerDocument(),
            XSignature.SHA1, Canonicalizer.EXCLUSIVE, signaturemethod);
        template.setPrefix("ds");
        template.setIndentation(false);
        final Map strTransformsNodeToNode = new HashMap();
        for (int i = 0; i < elementsToSign.length; i++) {
            final Element element = elementsToSign[i];
            final String id = signedIds[i];

            final Reference ref;
            if ("Assertion".equals(element.getLocalName()) && SamlConstants.NS_SAML.equals(element.getNamespaceURI()))
            throw new IllegalArgumentException("This test code is not able to sign a SAML assertion");
            ref = template.createReference("#" + id);

            if (DomUtils.isElementAncestor(securityHeader, element)) {
                log.fine("Per policy, breaking Basic Security Profile rules with enveloped signature" +
                  " of element " + element.getLocalName() + " with Id=\"" + id + "\"");
                ref.addTransform(Transform.ENVELOPED);
            }

            ref.addTransform(Transform.C14N_EXCLUSIVE);
            template.addReference(ref);
        }
        Element signatureEl = template.getSignatureElement();

        // Ensure that CanonicalizationMethod has required c14n subelemen
        final Element signedInfoElement = template.getSignedInfoElement();
        Element c14nMethod = DomUtils.findFirstChildElementByName(signedInfoElement,
            SoapUtil.DIGSIG_URI,
            "CanonicalizationMethod");
        DsigUtil.addInclusiveNamespacesToElement(c14nMethod);

        NodeList transforms = signedInfoElement.getElementsByTagNameNS(signedInfoElement.getNamespaceURI(), "Transform");
        for (int i = 0; i < transforms.getLength(); ++i)
            if (Transform.C14N_EXCLUSIVE.equals(((Element)transforms.item(i)).getAttribute("Algorithm")))
                DsigUtil.addInclusiveNamespacesToElement((Element)transforms.item(i));

        SignatureContext sigContext = new SignatureContext();
        sigContext.setIDResolver(new IDResolver() {
            public Element resolveID(Document doc, String s) {
                Element e = SoapUtil.getElementByWsuId(doc, s);
                return e;
            }
        });
        sigContext.setEntityResolver(XmlUtil.getXss4jEntityResolver());
        sigContext.setAlgorithmFactory(new AlgorithmFactoryExtn() {
            public Transform getTransform(String s) throws NoSuchAlgorithmException {
                if (SoapUtil.TRANSFORM_STR.equals(s))
                    return new Transform() {
            public String getURI() {
                return SoapUtil.TRANSFORM_STR;
            }

            public void transform(TransformContext c) throws TransformException {
                Node source = c.getNode();
                if (source == null) throw new TransformException("Source node is null");
                final Node result = (Node)strTransformsNodeToNode.get(source);
                if (result == null) throw new TransformException("Destination node is null");
                ExclusiveC11r canon = new ExclusiveC11r();
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                try {
                    canon.canonicalize(result, bo);
                } catch (IOException e) {
                    throw (TransformException)new TransformException().initCause(e);
                }
                c.setContent(bo.toByteArray(), "UTF-8");
            }
        };
                return super.getTransform(s);
            }
        });
        try {
            sigContext.sign(signatureEl, senderSigningKey);
        } catch (XSignatureException e) {
            String msg = e.getMessage();
            if (msg != null && msg.indexOf("Found a relative URI") >= 0)       // Bug #1209
                throw new InvalidDocumentFormatException("Unable to sign this message due to a relative namespace URI.", e);
            throw new DecoratorException(e);
        }

        return signatureEl;
    }

    private static void addKeyInfo(Element securityHeader,
                                   Element signatureElement,
                                   String thumbprint)
    {
        String wssePrefix = securityHeader.getPrefix();
        Element keyInfoEl = securityHeader.getOwnerDocument().createElementNS(signatureElement.getNamespaceURI(),
            "KeyInfo");
        keyInfoEl.setPrefix("ds");
        Element secTokRefEl = securityHeader.getOwnerDocument().createElementNS(securityHeader.getNamespaceURI(),
            SoapUtil.SECURITYTOKENREFERENCE_EL_NAME);
        secTokRefEl.setPrefix(wssePrefix);
        Element refEl = securityHeader.getOwnerDocument().createElementNS(securityHeader.getNamespaceURI(),
            "KeyIdentifier");
        refEl.setPrefix(wssePrefix);
        secTokRefEl.appendChild(refEl);
        keyInfoEl.appendChild(secTokRefEl);
        refEl.setAttribute("ValueType", SoapUtil.VALUETYPE_X509_THUMB_SHA1);
        DomUtils.setTextContent(refEl, thumbprint);
        signatureElement.appendChild(keyInfoEl);
    }

    private static String getWsuId(Element element) {
        String id = SoapUtil.getElementWsuId(element);
        if (id == null)
            throw new IllegalArgumentException("Element does not have a wsu:Id attribute: " + (element == null ? null : element.getNodeName()));
        return id;
    }


    public static final String LOCUTUS_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIICHDCCAYWgAwIBAgIIdE4leiyWLRgwDQYJKoZIhvcNAQEFBQAwIjEgMB4GA1UE\n" +
            "AxMXcm9vdC5sb2N1dHVzLmw3dGVjaC5jb20wHhcNMDUwNjE2MjAzNjU1WhcNMDcw\n" +
            "NjE2MjA0NjU1WjAdMRswGQYDVQQDExJsb2N1dHVzLmw3dGVjaC5jb20wgZ8wDQYJ\n" +
            "KoZIhvcNAQEBBQADgY0AMIGJAoGBAKQP1oC38waQSAJsNuQGBjl7nM7Zbw+pZOPY\n" +
            "LCEthrAjKDuNrzBBu2i+ZcZUtiFqcCK+QhPQjbZOM5MNzdbCanvKI39CNQ4lEhXW\n" +
            "47vgmhiKDukBLG+9pFMJIWvhwotTQHCva1Qb7Mxgg42BqSci7LUdfNIf1G/CH5IL\n" +
            "dGtnbV+NAgMBAAGjYDBeMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgXgMB0G\n" +
            "A1UdDgQWBBSRzG6pkEQOLIJwxX4RuWV5RGheSTAfBgNVHSMEGDAWgBRjaNTFKmhJ\n" +
            "GBvD78Vmj23o/TDLtTANBgkqhkiG9w0BAQUFAAOBgQCBiRB1EPlY63fjqCgA+E+v\n" +
            "vCW57srLu970msL8aULiP8wQTWdyPA6T4nd462Q1WXld+BJm7r1X9LBTKe9QxHRb\n" +
            "LgDefmW74ODPPQVkoB39B2KFmsM6hgMKxE08x6UofgpSgq/RNIXPHL9JoJL4KOMu\n" +
            "M2Jois/relTDkONyiqXDTQ==\n" +
            "-----END CERTIFICATE-----";

}
