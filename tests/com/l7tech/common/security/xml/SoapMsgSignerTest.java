package com.l7tech.common.security.xml;

import com.ibm.xml.dsig.KeyInfo;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.XpathExpression;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.xml.soap.SOAPConstants;
import java.math.BigInteger;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @deprecated this test needs to be replaced with more comprehensive testing of WssProcessor and WssDecorator
 * @author alex
 * @version $Revision$
 */
public class SoapMsgSignerTest extends TestCase {
    /**
     * test <code>SoapMsgSignerTest</code> constructor
     */
    public SoapMsgSignerTest( String name ) {
        super( name );
    }

    public void testSign1Element() throws Exception {
        Document d = getCleartextDocument();
        signAccountId( d );
        Element sigValue = (Element)d.getElementsByTagNameNS( SoapUtil.DIGSIG_URI, "SignatureValue" ).item(0);
        String sigValueText = XmlUtil.findFirstChildTextNode( sigValue );
        assertTrue( sigValueText.startsWith( "fA0VI2vxQj39" ) );
        assertTrue( sigValueText.endsWith( "OKH9uv0IyvaUOipw=" ) );
    }

    public void testSign2Elements() throws Exception {
        Document testDoc = getCleartextDocument();
        signAccountId( testDoc );
        signAndEncryptPrice( testDoc );

        assertTrue( encryptedElementsEqual( testDoc, "price", getKeyReq() ));
    }

    public void testSign3Elements() throws Exception {
        Document testDoc = getCleartextDocument();
        signAccountId( testDoc );
        signAndEncryptPrice( testDoc );
        signAndEncryptAmount( testDoc );

        assertTrue( encryptedElementsEqual( testDoc, "price", getKeyReq() ));
        assertTrue(encryptedElementsEqual( testDoc, "amount", getKeyReq() ));

        System.out.println("Approximate reconstruction of test document: \n");
        XmlUtil.nodeToOutputStream( testDoc, System.out );
    }

    public void testSignEnvelope() {
        // todo test this
        // not bothering for now, as already arguably covered well in XmlManglerTest
    }

    public void testValidateSignatureDocument() throws Exception {
    }

    // TODO remove or replace after TROGDOR implementation
/*
    public void testValidateSignatureElement() throws Exception {
        Document testDoc = getCleartextDocument();
        signAccountId( testDoc );
        signAndEncryptPrice( testDoc );
        signAndEncryptAmount( testDoc );
        Session s = new Session( Long.valueOf( getSessionId() ).longValue(),
                                 System.currentTimeMillis(),
                                 getKeyReq().getEncoded(),
                                 getKeyRes().getEncoded(), 17 );

        ElementSecurity[] esecs = getElementSecurity();

        ReceiverXmlSecurityProcessor foo = new ReceiverXmlSecurityProcessor(s, getKeyReq(), s.getId(), esecs );
        SecurityProcessor.Result bar = foo.processInPlace( testDoc ); // todo OMGWTFBBQ
        XmlUtil.nodeToOutputStream( testDoc, System.out );
        System.out.println( bar.getCertificateChain() );
    }
*/

    public void testValidateElements_Sign() throws Exception {
        Document testDoc = getCleartextDocument();
        signAccountId( testDoc );
        checkSignatureOnElement( testDoc, "accountid" );
    }

    public void testValidateElements_SignSign() throws Exception {
        Document testDoc = getCleartextDocument();
        signAccountId( testDoc );
        signElement( testDoc, "price",  false, 2, 1 );
        checkSignatureOnElement( testDoc, "accountid" );
        checkSignatureOnElement( testDoc, "price" );
    }

    public void testValidateElements_SignSignSign() throws Exception {
        Document testDoc = getCleartextDocument();
        signAccountId( testDoc );
        signElement( testDoc, "price",  false, 2, 1 );
        signElement( testDoc, "amount", false, 3, 2 );
        checkSignatureOnElement( testDoc, "accountid" );
        checkSignatureOnElement( testDoc, "price" );
        checkSignatureOnElement( testDoc, "amount" );
    }

    public void testValidateElements_Crypt() throws Exception {
        Document testDoc = getCleartextDocument();
        signElement( testDoc, "accountid", true, 1, 1 );
        checkSignatureOnElement( testDoc, "accountid" );
    }

    public void testValidateElements_CryptCrypt() throws Exception {
        Document testDoc = getCleartextDocument();
        signElement( testDoc, "accountid", true, 1, 1 );
        signElement( testDoc, "price",     true, 2, 2 );
        checkSignatureOnElement( testDoc, "accountid" );
        checkSignatureOnElement( testDoc, "price" );
    }

    public void testValidateElements_CryptCryptCrypt() throws Exception {
        Document testDoc = getCleartextDocument();
        signElement( testDoc, "accountid", true, 1, 1 );
        signElement( testDoc, "price",     true, 2, 2 );
        signElement( testDoc, "amount",    true, 3, 3 );
        checkSignatureOnElement( testDoc, "accountid" );
        checkSignatureOnElement( testDoc, "price" );
        checkSignatureOnElement( testDoc, "amount" );
    }

    public void testValidateElements_SignCrypt() throws Exception {
        Document testDoc = getCleartextDocument();
        signAccountId( testDoc );
        signAndEncryptPrice( testDoc );
        checkSignatureOnElement( testDoc, "accountid" );
        checkSignatureOnElement( testDoc, "price" );
    }

    public void testValidateElements_SignCryptCrypt() throws Exception {
        Document testDoc = getCleartextDocument();
        signAccountId( testDoc );
        signAndEncryptPrice( testDoc );
        signAndEncryptAmount( testDoc );
        checkSignatureOnElement( testDoc, "accountid" );
        checkSignatureOnElement( testDoc, "price" );
        checkSignatureOnElement( testDoc, "amount" );
    }

    public void testValidateElements_SignCryptSign() throws Exception {
        Document testDoc = getCleartextDocument();
        signAccountId( testDoc );
        signAndEncryptPrice( testDoc );
        signElement( testDoc, "amount",    false, 3, 2 );
        checkSignatureOnElement( testDoc, "accountid" );
        checkSignatureOnElement( testDoc, "price" );
        checkSignatureOnElement( testDoc, "amount" );
    }

    public void testValidateElements_SignSignCrypt() throws Exception {
        Document testDoc = getCleartextDocument();
        signElement( testDoc, "accountid", false, 1, 0 );
        signElement( testDoc, "price",     false, 2, 0 );
        signElement( testDoc, "amount",    true, 3, 1 );
        checkSignatureOnElement( testDoc, "accountid" );
        checkSignatureOnElement( testDoc, "price" );
        checkSignatureOnElement( testDoc, "amount" );
    }

    public void testValidateElements_CryptSign() throws Exception {
        Document testDoc = getCleartextDocument();
        signElement( testDoc, "accountid", true, 1, 1 );
        signElement( testDoc, "price",     false, 2, 2 );
        checkSignatureOnElement( testDoc, "accountid" );
        checkSignatureOnElement( testDoc, "price" );
    }

    public void testValidateElements_CryptSign2_CheckOpposite() throws Exception {
        Document testDoc = getCleartextDocument();
        signElement( testDoc, "price",     false, 1, 0 );
        signElement( testDoc, "accountid", true, 2, 1 );
        checkSignatureOnElement( testDoc, "accountid" );
        checkSignatureOnElement( testDoc, "price" );
    }

    public void testValidateElements_CryptSign2_CheckSame() throws Exception {
        Document testDoc = getCleartextDocument();
        signElement( testDoc, "price",     false, 1, 0 );
        signElement( testDoc, "accountid", true, 2, 1 );
        checkSignatureOnElement( testDoc, "price" );
        checkSignatureOnElement( testDoc, "accountid" );
    }


    public void testValidateElements_CryptSignCrypt() throws Exception {
        Document testDoc = getCleartextDocument();
        signElement( testDoc, "accountid", true, 1, 1 );
        signElement( testDoc, "price",     false, 2, 0 );
        signElement( testDoc, "amount",    true, 3, 2 );
        checkSignatureOnElement( testDoc, "accountid" );
        checkSignatureOnElement( testDoc, "price" );
        checkSignatureOnElement( testDoc, "amount" );
    }

    public void testValidateElements_CryptSignCryptSign() throws Exception {
        Document testDoc = getCleartextDocument();
        signElement( testDoc, "accountid", true, 1, 1 );
        signElement( testDoc, "price",     false, 2, 0 );
        signElement( testDoc, "amount",    true, 3, 2 );
        signElement( testDoc, "productid", false, 4, 0 );
        checkSignatureOnElement( testDoc, "accountid" );
        checkSignatureOnElement( testDoc, "price" );
        checkSignatureOnElement( testDoc, "amount" );
        checkSignatureOnElement( testDoc, "productid" );
    }

    public void testValidateElements_CryptSignCryptCrypt() throws Exception {
        Document testDoc = getCleartextDocument();
        signElement( testDoc, "accountid", true, 1, 1 );
        signElement( testDoc, "price",     false, 2, 0 );
        signElement( testDoc, "amount",    true, 3, 2 );
        signElement( testDoc, "productid", true, 4, 3 );
        checkSignatureOnElement( testDoc, "accountid" );
        checkSignatureOnElement( testDoc, "price" );
        checkSignatureOnElement( testDoc, "amount" );
        checkSignatureOnElement( testDoc, "productid" );
    }

    public void testValidateElements_CryptSignSignCrypt() throws Exception {
        Document testDoc = getCleartextDocument();
        signElement( testDoc, "accountid", true, 1, 1 );
        signElement( testDoc, "price",     false, 2, 0 );
        signElement( testDoc, "amount",    false, 3, 0 );
        signElement( testDoc, "productid", true, 4, 2 );
        checkSignatureOnElement( testDoc, "accountid" );
        checkSignatureOnElement( testDoc, "price" );
        checkSignatureOnElement( testDoc, "amount" );
        checkSignatureOnElement( testDoc, "productid" );
    }

    private void checkSignatureOnElement( Document d, String elementName ) throws Exception {
        Element accountIdElement = (Element) d.getElementsByTagName( elementName ).item(0);
        SoapMsgSigner.validateSignature( d, accountIdElement );
    }

    private ElementSecurity[] getElementSecurity() {
        Map nm = new HashMap();
        nm.put( "soapenv", SOAPConstants.URI_NS_SOAP_ENVELOPE );
        nm.put( "impl", "http://warehouse.acme.com/ws" );

        return new ElementSecurity[] {
            new ElementSecurity( new XpathExpression("/soapenv:Envelope/soapenv:Body/impl:placeOrder/accountid", nm ),
                                 new XpathExpression( "/soapenv:Envelope/soapenv:Body/impl:placeOrder", nm ),
                                 false,
                                 "AES", 128 ),

            new ElementSecurity( new XpathExpression("/soapenv:Envelope/soapenv:Body/impl:placeOrder/price", nm ),
                                 new XpathExpression( "/soapenv:Envelope/soapenv:Body/impl:placeOrder/placeOrder", nm  ),
                                 true,
                                 "AES", 128 ),

            new ElementSecurity( new XpathExpression("/soapenv:Envelope/soapenv:Body/impl:placeOrder/amount", nm ),
                                 new XpathExpression( "/soapenv:Envelope/soapenv:Body/impl:placeOrder/placeOrder", nm  ),
                                 true,
                                 "AES", 128 ),
        };
    }


    /** @return true iff. the elementName in testDocument has the same cleartext as elementName in getSignedDocument(). */
    private boolean encryptedElementsEqual( Document testDocument, String elementName, Key key ) throws Exception {
        Document knownDoc = getSignedDocument();
        Element knownEl = (Element)testDocument.getElementsByTagName( elementName ).item(0);
        Element testEl = (Element)knownDoc.getElementsByTagName( elementName ).item(0);

        String knownCleartext = decryptElement(knownEl, getKeyReq() );
        String testCleartext = decryptElement(testEl, getKeyReq() );
        return knownCleartext.equals(testCleartext);
    }

    /**
     * Decrypt an encrypted XML element using AES128/CBC/PKCS5Padding.
     * @param parent A DOM Element with at least one CipherValue descendant element.
     * @param key A 128 bit AES key, wrapped in a JCE Key object.
     * @return The decrypted string
     * @throws Exception in case today isn't your lucky day
     */
    private String decryptElement( Element parent, Key key ) throws Exception {
        Element knownCipherValue = (Element)parent.getElementsByTagNameNS( SoapUtil.XMLENC_NS, "CipherValue" ).item(0);
        String knownCipherBase64 = XmlUtil.findFirstChildTextNode( knownCipherValue );
        byte[] knownCipher = HexUtils.decodeBase64( knownCipherBase64 );
        Cipher aes = Cipher.getInstance( "AES/CBC/PKCS5Padding", JceProvider.getSymmetricJceProvider() );
        aes.init( Cipher.DECRYPT_MODE, getKeyReq(), new IvParameterSpec(knownCipher, 0, 16) );
        byte[] knownPlain = aes.doFinal( knownCipher, 16, knownCipher.length - 16 );
        return new String(knownPlain, "UTF-8");
    }

    private void signAccountId( Document d ) throws Exception {
        signElement( d, "accountid", false, 1, 0 );
    }

    private void signAndEncryptPrice( Document d ) throws Exception {
        signElement( d, "price", true, 2, 1 );
    }

    private void signAndEncryptAmount( Document d ) throws Exception {
        signElement( d, "amount", true, 3, 2 );
    }

    private void signElement( Document d, String elementName, boolean encrypt, int signref, int encref ) throws Exception {
        Element priceElement = (Element)d.getElementsByTagName( elementName ).item(0);
        if (encrypt)
            XmlMangler.encryptXml( priceElement, getKeyReq().getEncoded(), getSessionId(), "encref" + encref );
        SoapMsgSigner.signElement( d, priceElement, "signref" + signref, getClientCertPrivateKey(), new X509Certificate[] { getClientCertificate() } );
    }

    private Document getCleartextDocument() throws Exception {
        return TestDocuments.getTestDocument( TestDocuments.PLACEORDER_CLEARTEXT );
    }

    private Document getSignedDocument() throws Exception {
        return TestDocuments.getTestDocument( TestDocuments.PLACEORDER_WITH_MAJESTY );
    }

    private RSAPublicKey clientCertPublicKey = null;
    private RSAPublicKey getClientCertPublicKey() throws Exception {
        if (clientCertPublicKey != null) return clientCertPublicKey;
        return clientCertPublicKey = (RSAPublicKey)getClientCertificate().getPublicKey();
    }

    private RSAPrivateKey clientCertPrivateKey = null;
    private RSAPrivateKey getClientCertPrivateKey() throws Exception {
        if (clientCertPrivateKey != null)
            return clientCertPrivateKey;
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

        return clientCertPrivateKey = privkey;
    }

    private X509Certificate clientCertificate = null;
    private X509Certificate getClientCertificate() throws Exception {
        if (clientCertificate != null)
            return clientCertificate;
        Document d = getSignedDocument();

        // Find KeyInfo bodyElement, and extract certificate from this
        Element keyInfoElement = (Element)d.getElementsByTagNameNS( SoapUtil.DIGSIG_URI, "KeyInfo" ).item(0);

        if (keyInfoElement == null) {
            throw new SignatureNotFoundException("KeyInfo bodyElement not found");
        }

        KeyInfo keyInfo = new KeyInfo(keyInfoElement);

        // Assume a single X509 certificate
        KeyInfo.X509Data[] x509DataArray = keyInfo.getX509Data();

        KeyInfo.X509Data x509Data = x509DataArray[0];
        X509Certificate[] certs = x509Data.getCertificates();

        X509Certificate cert = certs[0];

        return clientCertificate = cert;
    }

    private Properties keyProperties = null;
    private Properties getKeyProperties() throws Exception {
        if (keyProperties != null)
            return keyProperties;
        Properties p = new Properties();
        p.load(TestDocuments.getInputStream(TestDocuments.PLACEORDER_KEYS));
        return keyProperties = p;
    }

    private BigInteger clientPrivateExponent = null;
    private BigInteger getClientPrivateExponent() throws Exception {
        if (clientPrivateExponent != null)
            return clientPrivateExponent;
        String keyHex = getKeyProperties().getProperty("privateExponent");
        return clientPrivateExponent = new BigInteger(keyHex, 16);
    }

    private AesKey keyReq = null;
    private AesKey getKeyReq() throws Exception {
        if (keyReq != null) return keyReq;
        return keyReq = getKey("keyReq");
    }

    private AesKey keyRes = null;
    private AesKey getKeyRes() throws Exception {
        if (keyRes != null) return keyRes;
        return keyRes = getKey("keyRes");
    }

    private String sessionId = null;
    private String getSessionId() throws Exception {
        if (sessionId != null) return sessionId;
        return sessionId = getKeyProperties().getProperty("sessionId");
    }

    private AesKey getKey(String keyName) throws Exception {
        String keyHex = getKeyProperties().getProperty(keyName);
        return new AesKey(HexUtils.unHexDump( keyHex ), 128);
    }

    /**
     * create the <code>TestSuite</code> for the SoapMsgSignerTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite( SoapMsgSignerTest.class );
        return suite;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    /**
     * Test <code>SoapMsgSignerTest</code> main.
     */
    public static void main( String[] args ) throws
                                             Throwable {
        junit.textui.TestRunner.run( suite() );
    }
}