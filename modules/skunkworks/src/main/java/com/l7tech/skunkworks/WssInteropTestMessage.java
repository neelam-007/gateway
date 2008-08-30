/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.skunkworks;

import com.ibm.xml.dsig.*;
import com.ibm.xml.dsig.transform.ExclusiveC11r;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.security.keys.AesKey;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.EncryptedElement;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.security.xml.KeyInfoElement;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.security.xml.DsigUtil;
//import com.l7tech.common.TestDocuments;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ISO8601Date;
import com.l7tech.util.InvalidDocumentFormatException;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class WssInteropTestMessage extends TestCase {
    private static final Logger log = Logger.getLogger(WssInteropTestMessage.class.getName());

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

    public MsgInfo makeTestMessage(String utUsername, String utPassword) throws Exception {

        // Take the sample request from WSS interop and turn it into one that uses certifictes that are known.

        // Complications:
        // - The sample document does signing before encryption -- that is, it has the EncryptedKey in front of
        //   the Signature in the Security header.  So we'll need to replace all the encrypted bits with plaintext,
        //   replace the signature with a new version, and then re-encrypt all the encrypted bits with the new key.

        // Start with the WSS example request
        Document d = null;// TestDocuments.getTestDocument(TestDocuments.WSS2005JUL_REQUEST_ORIG);
        Element env = d.getDocumentElement();
        String oldStr = XmlUtil.nodeToFormattedString(env);
        Element header = DomUtils.findFirstChildElementByName(env, (String)null, "Header");
        Element secHeader = DomUtils.findFirstChildElementByName(header, (String)null, "Security");
        Element body = DomUtils.findFirstChildElementByName(env, (String)null, "Body");

        // Make a new encryption key
        final String KEY_HEX = "37982a7c3a9fac218abfbaed959c743d094d052a1a9e824ee90a443a940c69ba";    // placeholder
        byte[] keyBytes = HexUtils.unHexDump(KEY_HEX);
        new Random().nextBytes(keyBytes);
        final AesKey aesKey = new AesKey(keyBytes, 256);
        XencUtil.XmlEncKey encKey = new XencUtil.XmlEncKey( XencUtil.AES_256_CBC, keyBytes);

        // Get the certs we're gonna use for the recipient
        //DefaultKey ksu = (DefaultKey)ApplicationContexts.getProdApplicationContext().getBean("keystore");
        //X509Certificate recipCert = ksu.getSslCert();
//        X509Certificate recipCert = TestDocuments.getWssInteropBobCert();
        X509Certificate recipCert = CertUtils.decodeCert(LOCUTUS_CERT.getBytes("UTF-8"));
        String recipCertPrint = CertUtils.getCertificateFingerprint(recipCert, CertUtils.ALG_SHA1, CertUtils.FINGERPRINT_BASE64);

        // Replace the KeyIdentifier thumbprint
        Element encryptedKeyEl = DomUtils.findFirstChildElementByName(secHeader, (String)null, "EncryptedKey");
        Element ekkinf = DomUtils.findFirstChildElementByName(encryptedKeyEl, (String)null, "KeyInfo");
        Element ekstr = DomUtils.findFirstChildElementByName(ekkinf, (String)null, "SecurityTokenReference");
        Element ekkid = DomUtils.findFirstChildElementByName(ekstr, (String)null, "KeyIdentifier");
        ekkid.setTextContent(recipCertPrint); // XXX This is a 1.5-ism, not in the 1.4 DOM
        ekkid.setAttribute("ValueType", SoapUtil.VALUETYPE_X509_THUMB_SHA1);
        KeyInfoElement.assertKeyInfoMatchesCertificate(ekkinf, recipCert);

        // Replace the EncryptedKey payload with our own symmetric key
        String aesKeyB64 = HexUtils.encodeBase64(XencUtil.encryptKeyWithRsaAndPad(keyBytes, recipCert.getPublicKey(), new Random()), true);
        Element ekcd = DomUtils.findFirstChildElementByName(encryptedKeyEl, (String)null, "CipherData");
        Element ekcv = DomUtils.findFirstChildElementByName(ekcd, (String)null, "CipherValue");
        ekcv.setTextContent(aesKeyB64); // XXX This is a 1.5-ism, not in the 1.4 DOM

        // Replace the encrypted payload with a plaintext one
        Element oldPayloadEl = DomUtils.findFirstChildElement(body);
        String newPayloadStr = "<tns:Ping xmlns:tns=\"http://xmlsoap.org/Ping\">Layer 7 Technologies - Scenario #8</tns:Ping>";
        Element newPayloadEl = XmlUtil.stringToDocument(newPayloadStr).getDocumentElement();
        newPayloadEl = (Element)d.importNode(newPayloadEl, true);
        body.replaceChild(newPayloadEl, oldPayloadEl);

        // Replace the encrypted UsernameToken with our own
        UsernameTokenImpl utok = new UsernameTokenImpl(utUsername, utPassword.toCharArray());
        Element oldUtok = DomUtils.findFirstChildElementByName(secHeader, (String)null, "EncryptedData");
        Element newUtok = utok.asElement(d, secHeader.getNamespaceURI(), secHeader.getPrefix());
        newUtok = (Element)d.importNode(newUtok, true);
        newUtok.setAttributeNS(SoapUtil.WSU_NAMESPACE, "wsu:Id", "UsernameToken");
        secHeader.replaceChild(newUtok, oldUtok);

        // Replace the existing EncryptedHeader with one that we are able to decrypt
        Element encPingHdr = DomUtils.findFirstChildElementByName(header, (String)null, "EncryptedHeader");
        String newPingHdrXml = "<tns:PingHeader wsu:Id=\"PingHeader\" xmlns:tns=\"http://xmlsoap.org/Ping\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">Layer 7 Technologies - Scenario #8</tns:PingHeader>";
        Element newPingHdrEl = (Element)d.importNode(XmlUtil.stringToDocument(newPingHdrXml).getDocumentElement(), true);
        header.replaceChild(newPingHdrEl, encPingHdr);


        // Replace the Signature header with a new one made with the proper cert
        Element timestampEl = DomUtils.findFirstChildElementByName(secHeader, (String)null, "Timestamp");
        Element created = DomUtils.findFirstChildElementByName(timestampEl, (String)null, "Created");
        Element expires = DomUtils.findFirstChildElementByName(timestampEl, (String)null, "Expires");
        Calendar cal = Calendar.getInstance();
        created.setTextContent( ISO8601Date.format(cal.getTime())); // XXX java 1.5 DOM feature
        cal.add(Calendar.DAY_OF_MONTH, 1);
        expires.setTextContent(ISO8601Date.format(cal.getTime())); // XXX java 1.5 DOM feature

        Element[] signedEl = new Element[] {
            timestampEl,
            newUtok,
            newPingHdrEl,
            body,
        };
        Element newSig = makeSignature(aesKey, signedEl, secHeader);
        addKeyInfo(secHeader, newSig, encryptedKeyEl, null);
        newSig = (Element)d.importNode(newSig, true);
        Element oldSig = DomUtils.findFirstChildElementByName(secHeader, (String)null, "Signature");
        secHeader.replaceChild(newSig, oldSig);
        NodeList sigvals = secHeader.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "SignatureValue");
        if (sigvals.getLength() != 1) throw new IllegalStateException("Found " + sigvals.getLength() + " SignatureValue elements; expected 1");
        Element sigval = (Element)sigvals.item(0);
        final String signatureValue = DomUtils.getTextValue(sigval);


        // Encrypt the new Ping header
        Element wrappedNewPing = XmlUtil.stringToDocument("<a>" + newPingHdrXml + "</a>").getDocumentElement();
        Element newEncPingHdrEl = XencUtil.encryptElement(wrappedNewPing, encKey);
        newEncPingHdrEl = (Element)d.importNode(newEncPingHdrEl, true);
        encPingHdr.replaceChild(newEncPingHdrEl, DomUtils.findFirstChildElement(encPingHdr));
        encPingHdr = (Element)d.importNode(encPingHdr, true);
        header.replaceChild(encPingHdr, newPingHdrEl);

        // Encrypt the new UsernameToken
        Element wrappedUtok = XmlUtil.stringToDocument("<a>" + XmlUtil.nodeToString(newUtok) + "</a>").getDocumentElement();
        Element newUtokEnc = XencUtil.encryptElement(wrappedUtok, encKey);
        newUtokEnc.setAttribute("Id", oldUtok.getAttribute("Id"));
        newUtokEnc.setAttribute("Type", oldUtok.getAttribute("Type"));
        newUtokEnc = (Element)d.importNode(newUtokEnc, true);
        secHeader.replaceChild(newUtokEnc, newUtok);

        // Encrypt the new paylod
        Element newPayloadEnc = XencUtil.encryptElement(body, encKey);
        newPayloadEnc.setAttribute("Id", "EncBody"); // change this -- possible error in example document (EncPing instead of EncBody)
        newPayloadEnc.setAttribute("Type", oldPayloadEl.getAttribute("Type"));
        newPayloadEnc = (Element)d.importNode(newPayloadEnc, true);

        final String newStr = XmlUtil.nodeToFormattedString(env);
        log.info("Final message (reformatted): " + newStr);
        //assertEquals(oldStr, newStr); // uncomment to see a visual comparison in IDEA of our changes

        return new MsgInfo(env.getOwnerDocument(), aesKeyB64, signatureValue, keyBytes);
    }

    public static void disabled_testSendUndecoratedMessage() throws Exception {
        Document d = null;//TestDocuments.getTestDocument(TestDocuments.DIR + "/wssInterop/simpleRequest.xml");
        assertNotNull(d);
        assertTrue(SoapUtil.isSoapMessage(d));
        final byte[] requestBytes = XmlUtil.nodeToString(d).getBytes("UTF8");
        log.info("Sending request:\n" + new String(requestBytes, "UTF8"));

        final String url = "http://bones.l7tech.com:8080/xml/insecurePing";
        //final String url = "http://bones.:7700/xml/ping";
        SimpleHttpClient hc = new SimpleHttpClient(new UrlConnectionHttpClient());
        GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(url));
        params.setContentType(ContentTypeHeader.XML_DEFAULT);
        params.setExtraHeaders(new HttpHeader[] {new GenericHttpHeader("SOAPAction", "\"\"")});
        SimpleHttpClient.SimpleXmlResponse result = hc.postXml(params, d);
        log.info("Response status: " + result.getStatus());
        log.info("Response content type: " + result.getContentType());
        log.info("Response content length: " + result.getContentLength());

        Document responseDoc = result.getDocument();
        log.info("Response body:\n" + XmlUtil.nodeToFormattedString(responseDoc));
    }

    public void testSendTestMessage() throws Exception {
        //doSendTestMessage("http://131.107.72.15/wss11/ping", "Alice", "abcd!1234"); // Microsoft
        //doSendTestMessage("http://liberty.oracle.com:8446/webservice/webservice", "alice", "fiodi");   // Oracle
        //doSendTestMessage("http://192.35.232.217:9080/wsse11/Scenario8", "alice", "password"); // IBM
        doSendTestMessage("http://locutus.l7tech.com:8080/xml/ping", "alice", "password"); // Layer 7
    }

    public void printTestMessage() throws Exception {
        String url = "http://locutus.l7tech.com:8080/xml/ping";
        String username = "alice";
        String password = "password";
        MsgInfo msgInfo = makeTestMessage(username, password);
    }

    private void doSendTestMessage(String url, String username, String password) throws Exception {
        final MsgInfo msgInfo = makeTestMessage(username, password);
        Document d = msgInfo.doc;
        assertNotNull(d);
        assertTrue(SoapUtil.isSoapMessage(d));
        final byte[] requestBytes = XmlUtil.nodeToString(d).getBytes("UTF8");
        log.info("Sending request:\n" + new String(requestBytes, "UTF8"));

        SimpleHttpClient hc = new SimpleHttpClient(new UrlConnectionHttpClient());
        GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(url));
        params.setContentType(ContentTypeHeader.XML_DEFAULT);
        params.setExtraHeaders(new HttpHeader[] {new GenericHttpHeader("SOAPAction", "\"\"")});
        SimpleHttpClient.SimpleXmlResponse result = hc.postXml(params, d);
        log.info("Response status: " + result.getStatus());
        log.info("Response content type: " + result.getContentType());
        log.info("Response content length: " + result.getContentLength());

        Document responseDoc = result.getDocument();
        log.info("Response body:\n" + XmlUtil.nodeToFormattedString(responseDoc));

        final AesKey aesKey = new AesKey(msgInfo.keyBytes, 256);
        WssProcessorImpl wsp = new WssProcessorImpl();


        SecurityTokenResolver resolver = new SimpleSecurityTokenResolver() {
            public byte[] getSecretKeyByEncryptedKeySha1(String encryptedKeySha1) {
                return aesKey.getEncoded();
            }
        };
        ProcessorResult wssResults = wsp.undecorateMessage(new Message(responseDoc), null, null, resolver);

        log.info("The following elements had at least all their content encrypted:");
        EncryptedElement[] enc = wssResults.getElementsThatWereEncrypted();
        for (int i = 0; i < enc.length; i++) {
            EncryptedElement encryptedElement = enc[i];
            log.info("  " + encryptedElement.asElement().getNodeName());
        }

        log.info("The following elements were signed:");
        SignedElement[] signed = wssResults.getElementsThatWereSigned();
        for (int i = 0; i < signed.length; i++) {
            SignedElement signedElement = signed[i];
            log.info("  " + signedElement.asElement().getNodeName());
        }

        final String expectedSigConf = msgInfo.signatureConf;
        // TODO maintain a list of signature confirmations
        // TODO maintain a list of signature confirmations
        // TODO maintain a list of signature confirmations
        // TODO maintain a list of signature confirmations
        final String foundSigConf = wssResults.getLastSignatureConfirmation();
        if (expectedSigConf.equals(foundSigConf))
            log.info("Expected signature confirmation was found: " + expectedSigConf);
        else
            log.warning("Expected signature confirmation was ***NOT*** found!  Expected=" + expectedSigConf + "   found=" + foundSigConf);
        // TODO maintain a list of signature confirmations
        // TODO maintain a list of signature confirmations
        // TODO maintain a list of signature confirmations
        // TODO maintain a list of signature confirmations

        log.info("Undecorated response (pretty-printed): " + XmlUtil.nodeToFormattedString(responseDoc));

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
                                   Element keyInfoReferenceTarget,
                                   String keyInfoValueTypeURI)
    {
        String bstId = getWsuId(keyInfoReferenceTarget);
        String wssePrefix = securityHeader.getPrefix();
        Element keyInfoEl = securityHeader.getOwnerDocument().createElementNS(signatureElement.getNamespaceURI(),
            "KeyInfo");
        keyInfoEl.setPrefix("ds");
        Element secTokRefEl = securityHeader.getOwnerDocument().createElementNS(securityHeader.getNamespaceURI(),
            SoapUtil.SECURITYTOKENREFERENCE_EL_NAME);
        secTokRefEl.setPrefix(wssePrefix);
        Element refEl = securityHeader.getOwnerDocument().createElementNS(securityHeader.getNamespaceURI(),
            "Reference");
        refEl.setPrefix(wssePrefix);
        secTokRefEl.appendChild(refEl);
        keyInfoEl.appendChild(secTokRefEl);
        refEl.setAttribute("URI", "#" + bstId);
        if (keyInfoValueTypeURI != null && keyInfoValueTypeURI.length() > 0)
            refEl.setAttribute("ValueType", keyInfoValueTypeURI);
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
