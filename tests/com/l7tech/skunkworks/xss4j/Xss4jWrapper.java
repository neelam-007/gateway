/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.xss4j;

import com.ibm.xml.dsig.*;
import com.ibm.xml.dsig.KeyInfo;
import com.ibm.xml.dsig.util.AdHocIDResolver;
import com.ibm.xml.enc.*;
import com.ibm.xml.enc.type.*;
import com.ibm.xml.enc.util.AdHocIdResolver;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public class Xss4jWrapper {
    public static final String DIGSIG_URI = "http://www.w3.org/2000/09/xmldsig#";

    public Xss4jWrapper() {
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware( true );
        dbf.setValidating( false );
    }

    public Document munge() throws Exception, IOException, SAXException {
        Document doc = parse( CLEARTEXT );

        // encrypt & sign price
        signElement( doc, "price", true, 1, 1 );

        // encrypt & sign amount
        signElement( doc, "amount", true, 2, 2 );

        // sign account id
        signElement( doc, "accountid", false, 3, 0 );

        return doc;
    }

    public Document unmunge( Document doc ) throws Exception {
        checkSignatureOnElement( doc, "accountid" ); // *** boom ***
        checkSignatureOnElement( doc, "price" );
        checkSignatureOnElement( doc, "amount" );

        decryptXml( doc, "price", getSecretKey() );
        decryptXml( doc, "amount", getSecretKey() );

        return doc;
    }

    private Document parse( String xml ) throws Exception {
        return dbf.newDocumentBuilder().parse( new ByteArrayInputStream( xml.getBytes("UTF-8") ) );
    }

    private SecretKey getSecretKey() {
        return new SecretKey() {
            public byte[] getEncoded() {
                byte[] newbytes = new byte[16];
                byte[] bad = unHexDump( SYMMETRIC );
                System.arraycopy( bad, 0, newbytes, 0, 16 );
                return newbytes;
            }

            public String getAlgorithm() {
                return "AES";
            }

            public String getFormat() {
                return "RAW";
            }
        };
    }

    private void signElement( Document d, String elementName, boolean encrypt, int signref, int encref ) throws Exception {
        Element priceElement = (Element)d.getElementsByTagName( elementName ).item(0);
        if (encrypt)
            encryptXml( priceElement, getSecretKey(), SESSION_ID, "encref" + encref );
        signXml( d, priceElement, "signref" + signref, getClientCertPrivateKey(), getClientCertificate() );
    }

    private boolean decryptXml(Document soapMsg, String elementName, Key key) throws Exception {
        // Find message part
        Element messagePartElement = (Element)soapMsg.getElementsByTagName(elementName).item(0);
        Element encryptedDataElement = (Element)messagePartElement.getElementsByTagNameNS( XMLENC_NS, "EncryptedData" ).item(0);
        if ( encryptedDataElement == null ) return false;
        String messagePartId = encryptedDataElement.getAttribute( "Id" );

        // Locate EncryptedData element by its reference in the Security header
        NodeList dataRefEls = soapMsg.getElementsByTagNameNS(XMLENC_NS, "DataReference");
        if ( dataRefEls == null || dataRefEls.getLength() == 0 )
            throw new Exception("no DataReference tag in the message");

        for ( int i = 0; i < dataRefEls.getLength(); i++ ) {
            Element dataRefEl = (Element)dataRefEls.item(i);
            String dataRefUri = dataRefEl.getAttribute("URI");
            if ( dataRefUri != null && dataRefUri.equals(messagePartId ) ) {
                // Create decryption context and decrypt the EncryptedData subtree. Note that this effects the
                // soapMsg document
                DecryptionContext dc = new DecryptionContext();
                AlgorithmFactoryExtn af = new AlgorithmFactoryExtn();
                dc.setAlgorithmFactory(af);
                dc.setEncryptedType(encryptedDataElement, EncryptedData.CONTENT, null, null);
                dc.setKey(key);

                dc.decrypt();
                dc.replace();
                return true;
            }
        }
        return false;
    }

    private void cleanup( Document document ) {
//        Element refListEl = (Element)dataRefEl.getParentNode();
//        refListEl.removeChild(dataRefEl);
    }

    private void signXml( Document document, Element messagePart, String referenceId, PrivateKey privateKey, X509Certificate cert ) throws Exception {
        if (document == null || messagePart == null | referenceId == null ||
          privateKey == null || cert == null) {
            throw new IllegalArgumentException();
        }

        String id = messagePart.getAttribute(referenceId);
        if (id == null || "".equals(id)) {
            id = referenceId;
            messagePart.setAttribute(ID_ATTRIBUTE_NAME, referenceId);
        }

        // set the appropriate signature method
        String signaturemethod = SignatureMethod.RSA;

        // Create signature template and populate with appropriate transforms. Reference is to SOAP Envelope
        TemplateGenerator template = new TemplateGenerator(document, XSignature.SHA1, Canonicalizer.W3C2, signaturemethod);
        template.setPrefix(DS_PREFIX);
        Reference ref = template.createReference("#" + id);
        ref.addTransform(com.ibm.xml.dsig.Transform.ENVELOPED);
        ref.addTransform(com.ibm.xml.dsig.Transform.W3CC14N2);
        template.addReference(ref);
        Element emptySignatureElement = template.getSignatureElement();

        // Signature is inserted in Header/Security, as per WS-S
        Element securityHeaderElement = getOrMakeSecurityElement(document);

        Element signatureElement = (Element)securityHeaderElement.appendChild(emptySignatureElement);

        // Include KeyInfo element in signature and embed cert into subordinate X509Data element
        KeyInfo keyInfo = new KeyInfo();
        KeyInfo.X509Data x509Data = new KeyInfo.X509Data();
        x509Data.setCertificate(cert);
        x509Data.setParameters(cert, true, true, true);
        keyInfo.setX509Data(new KeyInfo.X509Data[]{x509Data});
        keyInfo.insertTo(signatureElement, DS_PREFIX);

//        normalizeDoc(document);

        // Setup context and sign document
        SignatureContext sigContext = new SignatureContext();
        AdHocIDResolver idResolver = new AdHocIDResolver(document);
        sigContext.setIDResolver(idResolver);

        sigContext.sign(signatureElement, privateKey);
    }

    private void encryptXml( Element element, Key secretKey, String keyName, String ref ) throws Exception {
        Document soapMsg = element.getOwnerDocument();
        if (secretKey.getEncoded().length < 16)
            throw new IllegalArgumentException("keyBytes must be at least 16 bytes long for AES128");

        CipherData cipherData = new CipherData();
        cipherData.setCipherValue(new CipherValue());
        com.ibm.xml.enc.type.KeyInfo keyInfo = new com.ibm.xml.enc.type.KeyInfo();
        KeyName kn = new KeyName();
        kn.setName(keyName);
        keyInfo.addKeyName(kn);
        EncryptionMethod encMethod = new EncryptionMethod();
        encMethod.setAlgorithm(EncryptionMethod.AES128_CBC);
        EncryptedData encData = new EncryptedData();
        encData.setCipherData(cipherData);
        encData.setEncryptionMethod(encMethod);
        encData.setKeyInfo(keyInfo);
        encData.setId(ref);
        Element encDataElement = null;
        try {
            encDataElement = encData.createElement(soapMsg, true);
        } catch (StructureException e) {
            throw new Exception(e);
        }

        // Create encryption context and encrypt the header subtree
        EncryptionContext ec = new EncryptionContext();
        AlgorithmFactoryExtn af = new AlgorithmFactoryExtn();
        ec.setAlgorithmFactory(af);
        ec.setEncryptedType(encDataElement, EncryptedData.CONTENT, null, null);

        ec.setData(element);
        ec.setKey(secretKey);

        try {
            ec.encrypt();
            ec.replace();
        } catch (KeyInfoResolvingException e) {
            throw new Exception(e);
        } catch (StructureException e) {
            throw new Exception(e);
        }

        // Insert a WSS style header with a ReferenceList refering to the EncryptedData element
        addWssHeader(soapMsg.getDocumentElement(), ref);
    }

    /**
     * Update the document WSS header with the encrypted element reference info.
     *
     * @param element     the document element
     * @param referenceId the element reference id
     */
    private static void addWssHeader(Element element, String referenceId) {
        Document document = element.getOwnerDocument();
        // Add new namespaces to Envelope element, as per spec.

        Element rootElement = document.getDocumentElement();
        if (rootElement.getAttributeNodeNS(NS_ENC_URI, "xenc") == null) {
            rootElement.setAttributeNS(NS_ENC_URI, "xmlns:xenc", XMLENC_NS);
        }


        Element securityEl = getOrMakeSecurityElement(document);

        // Check if there's already a ReferenceList
        NodeList refEls = securityEl.getElementsByTagNameNS( XMLENC_NS, "ReferenceList" );
        Element refEl = null;
        if ( refEls.getLength() == 0 ) {
            refEl = document.createElementNS(XMLENC_NS, "xenc:ReferenceList");
            securityEl.appendChild(refEl);
        } else if ( refEls.getLength() > 1 ) {
            throw new IllegalStateException( "Uh-oh! Multiple ReferenceList elements already!" );
        } else {
            refEl = (Element)refEls.item(0);
        }

        // Add Security element to header, referencing the encrypted body
        Element dataRefEl = document.createElementNS(XMLENC_NS, "xenc:DataReference");
        dataRefEl.setAttribute("URI", referenceId);

        refEl.appendChild(dataRefEl);
    }

    public static Element getOrMakeSecurityElement(Document soapMsg) {
        NodeList listSecurityElements = soapMsg.getElementsByTagNameNS(SECURITY_NAMESPACE, SECURITY_EL_NAME);
        if (listSecurityElements.getLength() < 1) {
            listSecurityElements = soapMsg.getElementsByTagNameNS(SECURITY_NAMESPACE2, SECURITY_EL_NAME);
        }
        if (listSecurityElements.getLength() < 1) {
            // element does not exist
            Element header = getOrMakeHeader(soapMsg);
            Element securityEl = soapMsg.createElementNS(SECURITY_NAMESPACE, SECURITY_EL_NAME);
            securityEl.setPrefix(SECURITY_NAMESPACE_PREFIX);
            securityEl.setAttribute("xmlns:" + SECURITY_NAMESPACE_PREFIX, SECURITY_NAMESPACE);
            header.insertBefore(securityEl, null);
            return securityEl;
        } else {
            return (Element)listSecurityElements.item(0);
        }
    }

    public static Element getOrMakeHeader(Document soapMsg) {
        // use the soap flavor of this document
        String soapEnvNS = soapMsg.getDocumentElement().getNamespaceURI();
        NodeList list = soapMsg.getElementsByTagNameNS(soapEnvNS, HEADER_EL_NAME);
        if (list.getLength() < 1) {
            String soapEnvNamespacePrefix = soapMsg.getDocumentElement().getPrefix();

            // create header element
            Element header = soapMsg.createElementNS(soapEnvNS, HEADER_EL_NAME);
            header.setPrefix(soapEnvNamespacePrefix);

            // if the body is there, get it so that the header can be inserted before it
            Element body = getBody(soapMsg);
            if (body != null)
                soapMsg.getDocumentElement().insertBefore(header, body);
            else
                soapMsg.getDocumentElement().appendChild(header);
            return header;
        } else
            return (Element)list.item(0);
    }

    public static Element getBody(Document soapMsg) {
        // use the soap flavor of this document
        String soapEnvNS = soapMsg.getDocumentElement().getNamespaceURI();
        // if the body is there, get it so that the header can be inserted before it
        NodeList bodylist = soapMsg.getElementsByTagNameNS(soapEnvNS, BODY_EL_NAME);
        if (bodylist.getLength() > 0) {
            return (Element)bodylist.item(0);
        } else
            return null;
    }

    private RSAPublicKey clientCertPublicKey = null;
    private RSAPublicKey getClientCertPublicKey() throws Exception {
        if (clientCertPublicKey != null) return clientCertPublicKey;
        return clientCertPublicKey = (RSAPublicKey)getClientCertificate().getPublicKey();
    }

    private BigInteger getClientPrivateExponent() throws Exception {
        String keyHex = PRIVATE_EXPONENT;
        return new BigInteger(keyHex, 16);
    }


    private RSAPrivateKey getClientCertPrivateKey() throws Exception {
        final RSAPublicKey pubkey = getClientCertPublicKey();
        final BigInteger exp = getClientPrivateExponent();
        RSAPrivateKey privkey = new RSAPrivateKey() {
            public BigInteger getPrivateExponent() {
                return exp;
            }

            public byte[] getEncoded() {
                throw new UnsupportedOperationException();
            }

            public String getAlgorithm() {
                return "RSA";
            }

            public String getFormat() {
                return "RAW";
            }

            public BigInteger getModulus() {
                return pubkey.getModulus();
            }
        };

        return privkey;
    }


    private X509Certificate getClientCertificate() throws Exception {
        // Find KeyInfo bodyElement, and extract certificate from this
        Document keyInfoDoc = parse( KEYINFO );
        Element keyInfoElement = keyInfoDoc.getDocumentElement();

        if (keyInfoElement == null) {
            throw new Exception("KeyInfo bodyElement not found");
        }

        KeyInfo keyInfo = new KeyInfo(keyInfoElement);

        // Assume a single X509 certificate
        KeyInfo.X509Data[] x509DataArray = keyInfo.getX509Data();

        KeyInfo.X509Data x509Data = x509DataArray[0];
        X509Certificate[] certs = x509Data.getCertificates();

        X509Certificate cert = certs[0];
        return cert;
    }

    public static void main( String[] args ) throws Exception {
        Xss4jWrapper me = new Xss4jWrapper();
        Document doc = me.munge();
        System.err.println( "Got munged:\n" + documentToString( doc ) );
        Document unmunged = me.unmunge( doc ); // *** boom ***
        System.err.println( "Got unmunged:\n" + documentToString(unmunged) );
    }

    private static String documentToString( Document doc ) throws IOException {
        XMLSerializer xmlSerializer = new XMLSerializer();
        OutputFormat of = new OutputFormat();
        of.setIndent(4);
        xmlSerializer.setOutputFormat(of);
        StringWriter sw = new StringWriter();
        xmlSerializer.setOutputCharStream( sw );
        xmlSerializer.serialize( doc );
        return sw.getBuffer().toString();
    }


    private DocumentBuilderFactory dbf;

    private byte[] unHexDump( String hexData ) {
        if ( hexData.length() % 2 != 0 ) throw new IllegalArgumentException( "String must be of even length" );
        byte[] bytes = new byte[hexData.length()/2];
        for ( int i = 0; i < hexData.length(); i+=2 ) {
            int b1 = nybble( hexData.charAt(i) );
            int b2 = nybble( hexData.charAt(i+1) );
            byte b = (byte)((b1 << 4) + b2);
            bytes[i/2] = b;
        }
        return bytes;
    }

    private byte nybble( char hex ) {
        if ( hex <= '9' && hex >= '0' ) {
            return (byte)(hex - '0');
        } else if ( hex >= 'a' && hex <= 'f' ) {
            return (byte)(hex - 'a' + 10 );
        } else if ( hex >= 'A' && hex <= 'F' ) {
            return (byte)(hex - 'F' + 10 );
        } else {
            throw new IllegalArgumentException( "Invalid hex digit " + hex );
        }
    }

    private void checkSignatureOnElement( Document d, String elementName ) throws Exception {
        Element el = (Element) d.getElementsByTagName( elementName ).item(0);
        validateSignature( d, el );
    }

    private X509Certificate validateSignature( Document soapMsg, Element bodyElement ) throws Exception {
//        normalizeDoc(soapMsg);

        // find signature bodyElement
        final Element sigElement = getSignatureHeaderElement(soapMsg, bodyElement);
        if (sigElement == null) {
            throw new Exception("No signature bodyElement in this document");
        }

        SignatureContext sigContext = new SignatureContext();
        AdHocIDResolver idResolver = new AdHocIDResolver(soapMsg);
        sigContext.setIDResolver(idResolver);

        // Find KeyInfo element, and extract certificate from this
        Element keyInfoElement = KeyInfo.searchForKeyInfo(sigElement);
        if (keyInfoElement == null) {
            throw new Exception("KeyInfo bodyElement not found in " + sigElement.toString());
        }
        KeyInfo keyInfo = null;
        try {
            keyInfo = new KeyInfo(keyInfoElement);
        } catch (XSignatureException e) {
            throw new Exception("Unable to extract KeyInfo from signature", e);
        }

        // Assume a single X509 certificate
        KeyInfo.X509Data[] x509DataArray = keyInfo.getX509Data();
        // according to javadoc, this can be null
        if (x509DataArray == null || x509DataArray.length < 1) {
            throw new Exception("No x509 data found in KeyInfo bodyElement");
        }
        KeyInfo.X509Data x509Data = x509DataArray[0];
        X509Certificate[] certs = x509Data.getCertificates();
        // according to javadoc, this can be null
        if (certs == null || certs.length < 1) {
            throw new Exception("Could not get X509 cert");
        }
        X509Certificate cert = certs[0];

        // validate signature
        PublicKey pubKey = cert.getPublicKey();
        Validity validity = sigContext.verify(sigElement, pubKey);

        if (!validity.getCoreValidity()) { // *** boom ***
            throw new Exception("Validity not achieved: " + validity.getSignedInfoMessage() +
                                                ": " + validity.getReferenceMessage( 0 ) );
        }

        // verify that the entire envelope is signed
        String refid = bodyElement.getAttribute(ID_ATTRIBUTE_NAME);
        if (refid == null || refid.length() < 1) {
            throw new Exception("No reference id on envelope");
        }
        String envelopeURI = "#" + refid;
        for (int i = 0; i < validity.getNumberOfReferences(); i++) {
            if (!validity.getReferenceValidity(i)) {
                throw new Exception("Validity not achieved for bodyElement " + validity.getReferenceURI(i));
            }
            if (envelopeURI.equals(validity.getReferenceURI(i))) {
                // SUCCESS, RETURN THE CERT
                // first, consume the signature bodyElement by removing it
                sigElement.getParentNode().removeChild(sigElement);
                return cert;
            }
        }
        // if we get here, the envelope uri reference was not verified
        throw new Exception("No reference to envelope was verified.");
   }

    private static Element getSignatureHeaderElement(Document doc, Element bodyElement) throws Exception {
        Element header = findFirstChildElement( doc.getDocumentElement() );
        if ( header == null )
            throw new Exception( "SOAP header not found" );

        String bodyId = bodyElement.getAttribute( ID_ATTRIBUTE_NAME );
        if ( bodyId == null )
            throw new Exception( "ID attribute not found in supposedly signed body element" );

        Element security = findOnlyOneChildElementByName( header, SECURITY_NAMESPACE, SECURITY_EL_NAME );
        if ( security == null )
            throw new Exception( SECURITY_EL_NAME + " header not found" );

        // find signature element(s)
        List signatureElements = findChildElementsByName( security, DIGSIG_URI, SIGNATURE_EL_NAME );
        if (signatureElements.size() < 1) {
            throw new Exception( "No " + SIGNATURE_EL_NAME + " elements were found in " + SECURITY_EL_NAME + " header" );
        }

        // Find signature element matching the specified bodyElement
        for ( Iterator i = signatureElements.iterator(); i.hasNext(); ) {
            Element signature = (Element) i.next();
            Element signedInfo = findOnlyOneChildElementByName( signature,
                                                                DIGSIG_URI,
                                                                SIGNED_INFO_EL_NAME );
            Element reference = findOnlyOneChildElementByName( signedInfo,
                                                               DIGSIG_URI,
                                                               REFERENCE_EL_NAME );
            String uri = reference.getAttribute( "URI" );
            if ( uri == null || !uri.startsWith("#") || uri.length() < 2 ) {
                throw new Exception( "SignedInfo/Reference/URI is missing or points to non-local body part" );
            }

            if ( uri.substring(1).equals(bodyId) )
                return signature;
        }

        throw new Exception( "Did not find any matching Signature element" );
    }

    public static Element findFirstChildElement( Element parent ) {
        NodeList children = parent.getChildNodes();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE ) return (Element)n;
        }
        return null;
    }
    public static Element findFirstChildElementByName( Element parent, String nsuri, String name ) {
        if ( nsuri == null || name == null ) throw new IllegalArgumentException( "nsuri and name must be non-null!" );
        NodeList children = parent.getChildNodes();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE &&
                 name.equals( n.getLocalName()) &&
                 nsuri.equals( n.getNamespaceURI() ) )
                return (Element)n;
        }
        return null;
    }
    public static Element findOnlyOneChildElementByName( Element parent, String nsuri, String name ) throws Exception {
        if ( nsuri == null || name == null ) throw new IllegalArgumentException( "nsuri and name must be non-null!" );
        NodeList children = parent.getChildNodes();
        Element result = null;
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE &&
                 name.equals( n.getLocalName()) &&
                 nsuri.equals( n.getNamespaceURI() ) ) {
                if ( result != null ) throw new Exception( "Found multiple child elements: " + nsuri + ": " + name );
                result = (Element)n;
            }
        }
        return result;
    }
    public static List findChildElementsByName( Element parent, String nsuri, String name ) {
        if ( nsuri == null || name == null ) throw new IllegalArgumentException( "nsuri and name must be non-null!" );
        List found = new ArrayList();

        NodeList children = parent.getChildNodes();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE &&
                 name.equals( n.getLocalName()) &&
                 nsuri.equals( n.getNamespaceURI() ) )
                found.add( n );
        }

        return found;
    }

    private static final String KEYINFO = "<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                                         "    <ds:X509Data>\n" +
                                         "        <ds:X509IssuerSerial>\n" +
                                         "            <ds:X509IssuerName>CN=root.locutus.l7tech.com</ds:X509IssuerName>\n" +
                                         "            <ds:X509SerialNumber>4202746819200835680</ds:X509SerialNumber>\n" +
                                         "        </ds:X509IssuerSerial>\n" +
                                         "        <ds:X509SKI>o7bou7bVKgAfnOHAlGFohaqUWIc=</ds:X509SKI>\n" +
                                         "        <ds:X509SubjectName>CN=alex</ds:X509SubjectName>\n" +
                                         "\n" +
                                         "        <ds:X509Certificate>MIICEDCCAXmgAwIBAgIIOlMn9wdeYGAwDQYJKoZIhvcNAQEFBQAwIjEgMB4GA1UEAxMXcm9vdC5s\n" +
                                         "            b2N1dHVzLmw3dGVjaC5jb20wHhcNMDQwNDAxMDEzMTExWhcNMDYwNDAxMDE0MTExWjAPMQ0wCwYD\n" +
                                         "            VQQDEwRhbGV4MIGdMA0GCSqGSIb3DQEBAQUAA4GLADCBhwKBgQCG/pfjoWFxDceZ/lmk6oU0pL1q\n" +
                                         "            RzthFeAxalY+3SYEgM7016pzdQFp2Q1kipMRd4aAr7D0P1VlUzJY0xV07FMA19pc1NLsoiL48H9v\n" +
                                         "            wi02uAks5ydw/lbOWzO2VMUW+W0619tPsqrkibZQapowncWOvvFWwCU/Wh+aEQWCirGMtQIBEaNk\n" +
                                         "            MGIwDwYDVR0TAQH/BAUwAwEBADAPBgNVHQ8BAf8EBQMDB6AAMB0GA1UdDgQWBBSjtui7ttUqAB+c\n" +
                                         "            4cCUYWiFqpRYhzAfBgNVHSMEGDAWgBScfxfUJeD+dDOvgtX95a+OkkV25TANBgkqhkiG9w0BAQUF\n" +
                                         "            AAOBgQByNGVcPLg+H8s+CHUAGEmRhaQlitWBVs5F9effsyqwaz9gMr641OBaccvNtl92+25KmEIu\n" +
                                         "            ul6YvM0LfZG4r/LoUv8Xfgjb4IvHLbIFmekN80Pqr7pxVaiYRqMMwtGlTHpok9dtKLHZm0o/OoKz EishCafGGrzBkx2uSH4xUTeTYg==</ds:X509Certificate>\n" +
                                         "    </ds:X509Data>\n" +
                                         "</ds:KeyInfo>";

    public static final String CLEARTEXT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<soapenv:Envelope\n" +
        "    xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
        "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
        "    <soapenv:Body>\n" +
        "        <ns1:placeOrder\n" +
        "            soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:ns1=\"http://warehouse.acme.com/ws\">\n" +
        "            <productid xsi:type=\"xsd:long\">-9206260647417300294</productid>\n" +
        "            <amount xsi:type=\"xsd:long\">1</amount>\n" +
        "            <price xsi:type=\"xsd:float\">5.0</price>\n" +
        "            <accountid xsi:type=\"xsd:long\">228</accountid>\n" +
        "        </ns1:placeOrder>\n" +
        "    </soapenv:Body>\n" +
        "</soapenv:Envelope>";

    private static final String SYMMETRIC = "31a4418777d573b9349211db8b7a96b38249f351a7abb8b93fd2f8c81b5fbdbc";
    private static final String SESSION_ID = "5901929434192275332";
    private static final String PRIVATE_EXPONENT = "575971570e11dfbd9f4586763d88b08b79a7bd3d266bff189871fb9216a021080d7140411d87f1db13f99b68b983c5cf8071aebc28fb0553f366a6b387e435b44f4ea87aeef8bb247ce557bd1a7b09d4754c0eab239ad99d51c7df152956e03ab9e2bd61230b70dc8851113978f39c9d99f5e555aed0d3471619d4873a4520b1";

    public static final String NS_ENC_URI = "http://www.w3.org/2000/xmlns/";
    public static final String XMLENC_NS = "http://www.w3.org/2001/04/xmlenc#";
    public static final String SECURITY_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/xx/secext";
    public static final String SECURITY_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2002/12/secext";

    public static final String SECURITY_NAMESPACE_PREFIX = "wsse";
    private static final String DS_PREFIX = "ds";

    public static final String SECURITY_EL_NAME = "Security";
    public static final String BODY_EL_NAME = "Body";
    public static final String HEADER_EL_NAME = "Header";
    public static final String ID_ATTRIBUTE_NAME = "Id";
    public static final String SIGNATURE_EL_NAME = "Signature";
    public static final String SIGNED_INFO_EL_NAME = "SignedInfo";
    public static final String REFERENCE_EL_NAME = "Reference";
}