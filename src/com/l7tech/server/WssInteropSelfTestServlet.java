/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.ibm.xml.dsig.*;
import com.ibm.xml.dsig.transform.ExclusiveC11r;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.token.EncryptedElement;
import com.l7tech.common.security.token.SignedElement;
import com.l7tech.common.security.token.UsernameTokenImpl;
import com.l7tech.common.security.xml.DsigUtil;
import com.l7tech.common.security.xml.XencUtil;
import com.l7tech.common.security.xml.decorator.DecoratorException;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.WssProcessorImpl;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.SoapFaultDetail;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
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

/**
 * @author mike
 */
public class WssInteropSelfTestServlet extends HttpServlet {
    /** Get a single-valued parameter. */
    private String getSingleParam(HttpServletRequest hreq, String paramName) {
        Map pm = hreq.getParameterMap();
        String[] values = (String[])pm.get(paramName);
        if (values == null || values.length < 1) return null;
        if (values.length > 1) throw new IllegalArgumentException("Must be only one value for parameter " + paramName);
        return values[0];
    }

    protected void doPost(HttpServletRequest hreq, HttpServletResponse hres)
            throws ServletException, IOException
    {
        hres.setStatus(200);
        hres.setContentType("text/plain");
        hres.setHeader("CACHE-CONTROL", "NO-CACHE");
        hres.setHeader("PRAGMA", "NO-CACHE");

        final ServletOutputStream os = hres.getOutputStream();

        try {
            String username = getSingleParam(hreq, "username");
            String password = getSingleParam(hreq, "password");
            String url = getSingleParam(hreq, "url");

            if (username == null || username.length() < 1) throw new IllegalArgumentException("A username is required.");
            if (password == null || password.length() < 1) throw new IllegalArgumentException("A password is required.");
            if (url == null || url.length() < 1 || !isValidUrl(url)) throw new IllegalArgumentException("A valid target url is required.");

            url = url.replace("mail.l7tech.com:7080", "bones.l7tech.com:8080");

            doSendTestMessage(os, url, username, password);

        } catch (Exception e) {
            os.println("\n");
            explain(os, "  * " + ExceptionUtils.getMessage(e) + "\n");

            //os.println("\n\n");
            //e.printStackTrace(new PrintStream(os));
        }

        hres.flushBuffer();
    }

    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    protected void doGet(HttpServletRequest hreq, HttpServletResponse hres) throws ServletException, IOException
    {
        hres.setStatus(200);
        hres.setContentType("text/html");
        final ServletOutputStream os = hres.getOutputStream();
        os.print("<html><head><title>Layer 7 - WSS 1.1 Interop</title></head><body bgcolor=\"white\">");
        os.print("<img align=\"left\" src=\"http://www.layer7tech.com/assets/images/layer7_logo.gif\" alt=\"Layer 7 Technologies\">");
        os.print("<br clear=\"all\"><p><br></p><h3>WS-Security 1.1 Interop</h3>");
        os.print("<form method=\"post\">");
        os.print("<p>To point the Layer 7 client at your server, fill in the form below and click Submit.</p>");
        os.print("<table border=\"0\">");
        os.print("<tr><td>Username:</td><td><input type=\"text\" name=\"username\" size=\"10\" value=\"alice\"></td></tr>");
        os.print("<tr><td>Password:</td><td><input type=\"text\" name=\"password\" size=\"10\" value=\"password\"></td></tr>");
        os.print("<tr><td>URL:</td><td><input type=\"text\" name=\"url\" size=\"80\" value=\"http://mail.l7tech.com:7080/xml/ping\"></td></tr>");
        os.print("<tr><td>&nbsp;</td><td><input type=\"submit\" value=\"Submit\"></td></tr>");
        os.print("</table>");
        os.print("<p>Known interop participants:</p>");
        os.print("<table border=\"1\" cellpadding=\"3\" cellspacing=\"0\" style=\"font-size: 90%; border: gray solid 2px; border-collapse: collapse; text-align: left\">");
        os.print("<tr><th>Participant</th><th>Endpoint URL</th><th>Username</th><th>Password</th></tr>");
        os.print("<tr><td>Layer 7 Technologies</td><td>http://mail.l7tech.com:7080/xml/ping</td><td>alice</td><td>password</td></tr>");
        os.print("<tr><td><a href=\"http://192.35.232.217:9080/wsse11/index.jsp\">IBM</a></td><td>http://192.35.232.217:9080/wsse11/Scenario8</td><td>alice</td><td>password</td></tr>");
        os.print("<tr><td>Microsoft</td><td>http://131.107.72.15/wss11/ping</td><td>Alice</td><td>abcd!1234</td></tr>");
        os.print("<tr><td><a href=\"http://liberty.oracle.com:8446/wss11interop/interop.jsp\">Oracle</a></td><td>http://liberty.oracle.com:8446/webservice/webservice</td><td>alice</td><td>fiodi</td></tr>");
        os.print("</table>");
        os.print("</form></body></html>");
        hres.flushBuffer();
    }

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
        Document d = XmlUtil.stringToDocument(MESS);
        Element env = d.getDocumentElement();

        Element header = XmlUtil.findFirstChildElementByName(env, (String)null, "Header");
        Element secHeader = XmlUtil.findFirstChildElementByName(header, (String)null, "Security");
        Element body = XmlUtil.findFirstChildElementByName(env, (String)null, "Body");

        // Make a new encryption key
        final String KEY_HEX = "37982a7c3a9fac218abfbaed959c743d094d052a1a9e824ee90a443a940c69ba";    // placeholder
        byte[] keyBytes = HexUtils.unHexDump(KEY_HEX);
        new Random().nextBytes(keyBytes);
        final AesKey aesKey = new AesKey(keyBytes, 256);
        XencUtil.XmlEncKey encKey = new XencUtil.XmlEncKey(XencUtil.AES_256_CBC, aesKey);

        // Get the certs we're gonna use for the recipient
        //KeystoreUtils ksu = (KeystoreUtils)ApplicationContexts.getProdApplicationContext().getBean("keystore");
        //X509Certificate recipCert = ksu.getSslCert();
        X509Certificate recipCert = (X509Certificate)CertUtils.getFactory().generateCertificate(new ByteArrayInputStream(CERT.getBytes()));

        String recipCertPrint = CertUtils.getCertificateFingerprint(recipCert, CertUtils.ALG_SHA1, CertUtils.FINGERPRINT_BASE64);

        // Replace the KeyIdentifier thumbprint
        Element encryptedKeyEl = XmlUtil.findFirstChildElementByName(secHeader, (String)null, "EncryptedKey");
        Element ekkinf = XmlUtil.findFirstChildElementByName(encryptedKeyEl, (String)null, "KeyInfo");
        Element ekstr = XmlUtil.findFirstChildElementByName(ekkinf, (String)null, "SecurityTokenReference");
        Element ekkid = XmlUtil.findFirstChildElementByName(ekstr, (String)null, "KeyIdentifier");
        XmlUtil.setTextContent(ekkid, recipCertPrint);
        ekkid.setAttribute("ValueType", SoapUtil.VALUETYPE_X509_THUMB_SHA1);
        XencUtil.assertKeyInfoMatchesCertificate(ekkinf, recipCert);

        // Replace the EncryptedKey payload with our own symmetric key
        String aesKeyB64 = XencUtil.encryptKeyWithRsaAndPad(keyBytes, recipCert.getPublicKey(), new Random());
        Element ekcd = XmlUtil.findFirstChildElementByName(encryptedKeyEl, (String)null, "CipherData");
        Element ekcv = XmlUtil.findFirstChildElementByName(ekcd, (String)null, "CipherValue");
        XmlUtil.setTextContent(ekcv, aesKeyB64);

        // Replace the encrypted payload with a plaintext one
        Element oldPayloadEl = XmlUtil.findFirstChildElement(body);
        String newPayloadStr = "<tns:Ping xmlns:tns=\"http://xmlsoap.org/Ping\">Layer 7 Technologies - Scenario #8</tns:Ping>";
        Element newPayloadEl = XmlUtil.stringToDocument(newPayloadStr).getDocumentElement();
        newPayloadEl = (Element)d.importNode(newPayloadEl, true);
        body.replaceChild(newPayloadEl, oldPayloadEl);

        // Replace the encrypted UsernameToken with our own
        UsernameTokenImpl utok = new UsernameTokenImpl(utUsername, utPassword.toCharArray());
        Element oldUtok = XmlUtil.findFirstChildElementByName(secHeader, (String)null, "EncryptedData");
        Element newUtok = utok.asElement(d, secHeader.getNamespaceURI(), secHeader.getPrefix());
        newUtok = (Element)d.importNode(newUtok, true);
        newUtok.setAttributeNS(SoapUtil.WSU_NAMESPACE, "wsu:Id", "UsernameToken");
        secHeader.replaceChild(newUtok, oldUtok);

        // Replace the existing EncryptedHeader with one that we are able to decrypt
        Element encPingHdr = XmlUtil.findFirstChildElementByName(header, (String)null, "EncryptedHeader");
        String newPingHdrXml = "<tns:PingHeader wsu:Id=\"PingHeader\" xmlns:tns=\"http://xmlsoap.org/Ping\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">Layer 7 Technologies - Scenario #8</tns:PingHeader>";
        Element newPingHdrEl = (Element)d.importNode(XmlUtil.stringToDocument(newPingHdrXml).getDocumentElement(), true);
        header.replaceChild(newPingHdrEl, encPingHdr);


        // Replace the Signature header with a new one made with the proper cert
        Element timestampEl = XmlUtil.findFirstChildElementByName(secHeader, (String)null, "Timestamp");
        Element created = XmlUtil.findFirstChildElementByName(timestampEl, (String)null, "Created");
        Element expires = XmlUtil.findFirstChildElementByName(timestampEl, (String)null, "Expires");
        Calendar cal = Calendar.getInstance();
        XmlUtil.setTextContent(created, ISO8601Date.format(cal.getTime()));
        cal.add(Calendar.DAY_OF_MONTH, 1);
        XmlUtil.setTextContent(expires, ISO8601Date.format(cal.getTime()));

        Element[] signedEl = new Element[] {
            timestampEl,
            newUtok,
            newPingHdrEl,
            body,
        };
        Element newSig = makeSignature(aesKey, signedEl, secHeader);
        addKeyInfo(secHeader, newSig, encryptedKeyEl, null);
        newSig = (Element)d.importNode(newSig, true);
        Element oldSig = XmlUtil.findFirstChildElementByName(secHeader, (String)null, "Signature");
        secHeader.replaceChild(newSig, oldSig);
        NodeList sigvals = secHeader.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "SignatureValue");
        if (sigvals.getLength() != 1) throw new IllegalStateException("Found " + sigvals.getLength() + " SignatureValue elements; expected 1");
        Element sigval = (Element)sigvals.item(0);
        final String signatureValue = XmlUtil.getTextValue(sigval);


        // Encrypt the new Ping header
        Element wrappedNewPing = XmlUtil.stringToDocument("<a>" + newPingHdrXml + "</a>").getDocumentElement();
        Element newEncPingHdrEl = XencUtil.encryptElement(wrappedNewPing, encKey);
        newEncPingHdrEl = (Element)d.importNode(newEncPingHdrEl, true);
        encPingHdr.replaceChild(newEncPingHdrEl, XmlUtil.findFirstChildElement(encPingHdr));
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
        //log.info("Final message (reformatted): " + newStr);
        //assertEquals(oldStr, newStr); // uncomment to see a visual comparison in IDEA of our changes

        return new MsgInfo(env.getOwnerDocument(), aesKeyB64, signatureValue, keyBytes);
    }

    private static void assertTrue(boolean soapMessage) {
        if (!soapMessage) throw new IllegalStateException("Assertion failed");
    }

    private static void assertNotNull(Object d) {
        if (d == null) throw new IllegalStateException("Object was null but wasn't supposed to be");
    }

    private void doSendTestMessage(ServletOutputStream os, String url, String username, String password) throws Exception {
        MsgInfo msgInfo = makeTestMessage(username, password);
        Document d = msgInfo.doc;
        assertNotNull(d);
        assertTrue(SoapUtil.isSoapMessage(d));
        final byte[] requestBytes = XmlUtil.nodeToString(d).getBytes("UTF8");
        os.println("\nSending request XML:\n\n" + new String(requestBytes, "UTF8"));

        SimpleHttpClient hc = new SimpleHttpClient(new UrlConnectionHttpClient());
        GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(url));
        params.setContentType(ContentTypeHeader.XML_DEFAULT);
        params.setExtraHeaders(new HttpHeader[] {new GenericHttpHeader("SOAPAction", "\"\"")});
        SimpleHttpClient.SimpleXmlResponse result = hc.postXml(params, d);

        Document responseDoc = result.getDocument();
        os.println("\n\nReceived response XML:\n\n" + XmlUtil.nodeToFormattedString(responseDoc));

        final SoapFaultDetail soapFaultDetail = SoapFaultUtils.gatherSoapFaultDetail(responseDoc);
        if (soapFaultDetail != null) {
            explain(os, "  * Response was a SOAP fault.\n");
            return;
        }

        final AesKey aesKey = new AesKey(msgInfo.keyBytes, 256);
        WssProcessorImpl wsp = new WssProcessorImpl();
        wsp.setKnownEncryptedKey(aesKey, HexUtils.decodeBase64(msgInfo.encryptedKeySha1, true));
        ProcessorResult wssResults = wsp.undecorateMessage(new Message(responseDoc), null, null, null, null);

        String failmess = "";
        boolean encBody = false;
        //log.info("The following elements had at least all their content encrypted:");
        EncryptedElement[] enc = wssResults.getElementsThatWereEncrypted();
        for (int i = 0; i < enc.length; i++) {
            EncryptedElement encryptedElement = enc[i];
            final String n = encryptedElement.asElement().getLocalName();
            if ("Body".equals(n)) encBody = true;
        }
        if (!encBody) failmess += "  * Response body was not encrypted or could not be decrypted.\n";

        //log.info("The following elements were signed:");
        boolean sigBody = false;
        boolean sigTimestamp = false;
        boolean sigSigConf = false;
        SignedElement[] signed = wssResults.getElementsThatWereSigned();
        for (int i = 0; i < signed.length; i++) {
            SignedElement signedElement = signed[i];
            final String n = signedElement.asElement().getLocalName();
            if ("Body".equals(n)) sigBody = true;
            if ("Timestamp".equals(n)) sigTimestamp = true;
            if ("SignatureConfirmation".equals(n)) sigSigConf = true;
        }
        if (!sigBody) failmess += "  * Response body was not signed or signature could not be verified.\n";
        if (!sigTimestamp) failmess += "  * Response timestamp was missing or not signed, or the signature could not be verified.\n";
        if (!sigSigConf) failmess += "  * Response SignatureConfirmation was missing or not signed, or the signature could not be verified.\n";

        final String expectedSigConf = msgInfo.signatureConf;
        final String foundSigConf = wssResults.getLastSignatureConfirmation();
        if (!expectedSigConf.equals(foundSigConf))
            failmess += "  * Response did not contain the expected SignatureConfirmation Value.  Expected=" + expectedSigConf + "   Found=" + foundSigConf + "\n";

        Element payload = SoapUtil.getPayloadElement(responseDoc);
        if (payload == null) {
            failmess += "  * Response did not include a SOAP Body payload.\n";
        } else {
            if ((!"PingResponse".equals(payload.getLocalName())) || !("http://xmlsoap.org/Ping".equals(payload.getNamespaceURI()))) {
                failmess += "  * Response did not include a PingResponse element.\n";
            } else {
                String pingResponse = XmlUtil.getTextValue(payload);
                os.println("\nPing response message: \"" + pingResponse + "\"");
            }
        }

        explain(os, failmess);
    }

    private void explain(ServletOutputStream os, String failmess) throws IOException {
        if (failmess.length() > 0) {
            os.print("\n\nTest unsuccessful.  Reason:\n" + failmess);
        } else {
            os.print("\n\nTest successful.");
        }
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

            if (XmlUtil.isElementAncestor(securityHeader, element)) {
                ref.addTransform(Transform.ENVELOPED);
            }

            ref.addTransform(Transform.C14N_EXCLUSIVE);
            template.addReference(ref);
        }
        Element signatureEl = template.getSignatureElement();

        // Ensure that CanonicalizationMethod has required c14n subelemen
        final Element signedInfoElement = template.getSignedInfoElement();
        Element c14nMethod = XmlUtil.findFirstChildElementByName(signedInfoElement,
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
        sigContext.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
                throw new SAXException("Unsupported external entity reference publicId=" + publicId +
                  ", systemId=" + systemId);
            }
        });
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


    private static final String CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDCjCCAfKgAwIBAgIQYDju2/6sm77InYfTq65x+DANBgkqhkiG9w0BAQUFADAw\n" +
            "MQ4wDAYDVQQKDAVPQVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENB\n" +
            "MB4XDTA1MDMxOTAwMDAwMFoXDTE4MDMxOTIzNTk1OVowQDEOMAwGA1UECgwFT0FT\n" +
            "SVMxIDAeBgNVBAsMF09BU0lTIEludGVyb3AgVGVzdCBDZXJ0MQwwCgYDVQQDDANC\n" +
            "b2IwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAMCquMva4lFDrv3fXQnKK8Ck\n" +
            "SU7HvVZ0USyJtlL/yhmHH/FQXHyYY+fTcSyWYItWJYiTZ99PAbD+6EKBGbdfuJNU\n" +
            "JCGaTWc5ZDUISqM/SGtacYe/PD/4+g3swNPzTUQAIBLRY1pkr2cm3s5Ch/f+mYVN\n" +
            "BR41HnBeIxybw25kkoM7AgMBAAGjgZMwgZAwCQYDVR0TBAIwADAzBgNVHR8ELDAq\n" +
            "MCiiJoYkaHR0cDovL2ludGVyb3AuYmJ0ZXN0Lm5ldC9jcmwvY2EuY3JsMA4GA1Ud\n" +
            "DwEB/wQEAwIEsDAdBgNVHQ4EFgQUXeg55vRyK3ZhAEhEf+YT0z986L0wHwYDVR0j\n" +
            "BBgwFoAUwJ0o/MHrNaEd1qqqoBwaTcJJDw8wDQYJKoZIhvcNAQEFBQADggEBAIiV\n" +
            "Gv2lGLhRvmMAHSlY7rKLVkv+zEUtSyg08FBT8z/RepUbtUQShcIqwWsemDU8JVts\n" +
            "ucQLc+g6GCQXgkCkMiC8qhcLAt3BXzFmLxuCEAQeeFe8IATr4wACmEQE37TEqAuW\n" +
            "EIanPYIplbxYgwP0OBWBSjcRpKRAxjEzuwObYjbll6vKdFHYIweWhhWPrefquFp7\n" +
            "TefTkF4D3rcctTfWJ76I5NrEVld+7PBnnJNpdDEuGsoaiJrwTW3Ixm40RXvG3fYS\n" +
            "4hIAPeTCUk3RkYfUkqlaaLQnUrF2hZSgiBNLPe8gGkYORccRIlZCGQDEpcWl1Uf9\n" +
            "OHw6fC+3hkqolFd5CVI=\n" +
            "-----END CERTIFICATE----- ";

    private static final String MESS = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\"\n" +
            "    xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"\n" +
            "    xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"\n" +
            "    xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"\n" +
            "    xmlns:wsse11=\"http://docs.oasis-open.org/wss/2005/xx/oasis-2005xx-wss-wssecurity-secext-1.1.xsd\">\n" +
            "<soap:Header>\n" +
            "<wsse11:EncryptedHeader wsu:Id=\"EncPingHeader\"><xenc:EncryptedData><xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/><xenc:CipherData><xenc:CipherValue>Ii0t6VeDmNQ6pWVQpz1MdZwchSTs7W+i1pRL3hutniZU2GFxJabDbE56ge5Whx2r+zrKlTkvOUjbEe2sE4WaJw48h/oO+/8wD95MfBMgVv+u7pNmp7UUWbM2pFvEesuYqHBlrlFxV593FOdbX/FI0HcdXLnJglS5/lLUr6Mridy9ENBWYh1P0sr1H2OCzgRtyxK0UjzyBcpH6QN36WxMX+XM/yC6SjVHifKpc11sCvEqAPrgAvlAh4AL2NSAfzQ8coC6c90mZhsd1xzoc3YsbJd79aW9SVMizrXScnDaUiEvIi2GJ0trHiDtSdY/jzBX</xenc:CipherValue></xenc:CipherData></xenc:EncryptedData></wsse11:EncryptedHeader>\n" +
            "<wsse:Security soap:mustUnderstand=\"1\">\n" +
            "<wsu:Timestamp wsu:Id=\"Timestamp\"><wsu:Created>2005-05-14T05:55:22.994Z</wsu:Created><wsu:Expires>2005-05-15T05:55:22.994Z</wsu:Expires></wsu:Timestamp>\n" +
            "<xenc:EncryptedKey Id=\"EncKey\"><xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/><ds:KeyInfo><wsse:SecurityTokenReference><wsse:KeyIdentifier\n" +
            "ValueType=\"http://docs.oasis-open.org/wss/2004/xx/oasis-2004xx-wss-x509-token-profile-1.1#X509ThumbprintSHA1\">LKiQ/CmFrJDJqCLFcjlhIsmZ/+0=</wsse:KeyIdentifier></wsse:SecurityTokenReference></ds:KeyInfo><xenc:CipherData><xenc:CipherValue>bYfDxlgGGoaF40mDswdACx0RGuwSubQbcM9N06QqmIQ8oy9TMyUk1dMnw7y/sPWYx3uXy0rYhC8sLRGsVdihpvS+RTb/K0B2P/kCryEG4iJvJCacTXoR9lDP1CCjbTdCXrkNfZ0ocmiA2mcHdhLeibAT+XYgqs+c9kgGmwaMSJM=</xenc:CipherValue></xenc:CipherData><xenc:ReferenceList><xenc:DataReference URI=\"#EncPingHeader\"/><xenc:DataReference URI=\"#EncBody\"/><xenc:DataReference URI=\"#EncUsernameToken\"/></xenc:ReferenceList></xenc:EncryptedKey>\n" +
            "<xenc:EncryptedData Id=\"EncUsernameToken\"\n" +
            "Type=\"http://www.w3.org/2001/04/xmlenc#Element\"><xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/><xenc:CipherData><xenc:CipherValue>TJezCegnJjeS6EcUh0wFVcseeri/QBXBsE1y0JMT0bIiqTnMeDL8gWWMPau4PVHjm+n47kVno2+KIXu3wjdJnASOS1kErchWiF6fC1kf9LSj5B2VTOqFPTazyaDFuVfZ9PmOyfsyGrE/9IzZLUCjAtY3gqjZUtVpTwHbeV8/p4neSRwHLUMZ1+AnvqgejjzkowgB43Z9y7Sourb+7mat1MPTCrP1aDyIBBS11v81xg7JGStvO6xA6Ufd9KjSv9uDEU5I4K5w6IY6Iv0P3xgxw7VxBP9xzi0GmbvYJFa7RPcOHUN2S+Sqr8jvZZAv6QjbI494y07h/tWgrQEBe/qQSi7tWfhGVoeh30JuBaaplP/yzXruVFImlP5lMZT7SITKQKjt+WiEwvmfKoFOrWPEO03e2EdHlDtzz6qoMY7LKswtelIexlXVcbF9bKCc7hc/VsdbTCJGLwehNP84cBk1tCJKIHQd8oq8CLOMuazMlT4=</xenc:CipherValue></xenc:CipherData></xenc:EncryptedData><ds:Signature><ds:SignedInfo><ds:CanonicalizationMethod\n" +
            "Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod\n" +
            "Algorithm=\"http://www.w3.org/2000/09/xmldsig#hmac-sha1\"/><ds:Reference URI=\"#Timestamp\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>IiFV7HSiL3mHn9gAHQmosC4MLiM=</ds:DigestValue></ds:Reference><ds:Reference\n" +
            "URI=\"#UsernameToken\"><ds:Transforms><ds:Transform\n" +
            "Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>Y43GKqThYec5VhjJo9uMUUmijTI=</ds:DigestValue></ds:Reference><ds:Reference URI=\"#PingHeader\"><ds:Transforms><ds:Transform\n" +
            "Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod\n" +
            "Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>qNh8qc0JAeT2DHKLDhnhx1SeLIs=</ds:DigestValue></ds:Reference><ds:Reference\n" +
            "URI=\"#Body\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod\n" +
            "Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>AApMppXTUonGAvIEvijdw3MRd/Y=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>fNa57H35Xm/14dDK3wBJ1pkW6i4=</ds:SignatureValue><ds:KeyInfo><wsse:SecurityTokenReference><wsse:Reference URI=\"#EncKey\"/></wsse:SecurityTokenReference></ds:KeyInfo></ds:Signature></wsse:Security></soap:Header><soap:Body\n" +
            "wsu:Id=\"Body\"><xenc:EncryptedData\n" +
            "Id=\"EncPing\" Type=\"http://www.w3.org/2001/04/xmlenc#Content\"><xenc:EncryptionMethod\n" +
            "Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/><xenc:CipherData><xenc:CipherValue>ZLpEO/voXN4dDw2Sp2/9Hcqvs8cT48RcozXrtNzKeOcewom3zMANIxg2sZBZ47DCISJL61hdPwLdoqBfbY6LDfXr0ghK2gwlj70jOMsrLWU=</xenc:CipherValue></xenc:CipherData></xenc:EncryptedData></soap:Body></soap:Envelope>";
}
