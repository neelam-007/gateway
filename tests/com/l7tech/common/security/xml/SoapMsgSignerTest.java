package com.l7tech.common.security.xml;

import com.ibm.xml.dsig.KeyInfo;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import java.math.BigInteger;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Properties;

/**
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
        XmlUtil.documentToOutputStream( testDoc, System.out );
    }

    public void testSignEnvelope() {
        // todo test this
        // not bothering for now, as already arguably covered well in XmlManglerTest
    }

    public void testValidateSignatureDocument() throws Exception {
    }

    public void testValidateSignatureElement() throws Exception {
        Document testDoc = getCleartextDocument();
        signAccountId( testDoc );
//        ReceiverXmlSecurityProcessor foo = new ReceiverXmlSecurityProcessor();
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
        BASE64Decoder b64decoder = new BASE64Decoder();
        byte[] knownCipher = b64decoder.decodeBuffer( knownCipherBase64 );
        Cipher aes = Cipher.getInstance( "AES/CBC/PKCS5Padding", JceProvider.getProvider() );
        aes.init( Cipher.DECRYPT_MODE, getKeyReq(), new IvParameterSpec(knownCipher, 0, 16) );
        byte[] knownPlain = aes.doFinal( knownCipher, 16, knownCipher.length - 16 );
        return new String(knownPlain, "UTF-8");
    }

    private void signAccountId( Document d ) throws Exception {
        Element accountIdElement = (Element) d.getElementsByTagName( "accountid" ).item(0);
        SoapMsgSigner.signElement( d, accountIdElement, "signref1", getClientCertPrivateKey(), getClientCertificate() );
    }

    private void signAndEncryptPrice( Document d ) throws Exception {
        Element priceElement = (Element)d.getElementsByTagName( "price" ).item(0);
        XmlMangler.encryptXml( priceElement, getKeyReq().getEncoded(), getSessionId(), "encref1" );
        SoapMsgSigner.signElement( d, priceElement, "signref2", getClientCertPrivateKey(), getClientCertificate() );
    }

    private void signAndEncryptAmount( Document d ) throws Exception {
        Element amountElement = (Element)d.getElementsByTagName( "amount" ).item(0);
        XmlMangler.encryptXml( amountElement, getKeyReq().getEncoded(), getSessionId(), "encref2" );
        SoapMsgSigner.signElement( d, amountElement, "signref3", getClientCertPrivateKey(), getClientCertificate() );
    }

    private Document getCleartextDocument() throws Exception {
        return TestDocuments.getTestDocument( TestDocuments.PLACEORDER_CLEARTEXT );
    }

    private Document getSignedDocument() throws Exception {
        return TestDocuments.getTestDocument( TestDocuments.PLACEORDER_WITH_MAJESTY );
    }

    private RSAPublicKey getClientCertPublicKey() throws Exception {
        return (RSAPublicKey)getClientCertificate().getPublicKey();
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

        return cert;
    }

    private Properties getKeyProperties() throws Exception {
        Properties p = new Properties();
        p.load(TestDocuments.getInputStream(TestDocuments.PLACEORDER_KEYS));
        return p;
    }

    private BigInteger getClientPrivateExponent() throws Exception {
        String keyHex = getKeyProperties().getProperty("privateExponent");
        return new BigInteger(keyHex, 16);
    }

    private AesKey getKeyReq() throws Exception {
        return getKey("keyReq");
    }

    private AesKey getKeyRes() throws Exception {
        return getKey("keyRes");
    }

    private String getSessionId() throws Exception {
        return getKeyProperties().getProperty("sessionId");
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