package com.l7tech.security.xml;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.TestKeys;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.cert.TestKeysLoader;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlAssertionGenerator;
import com.l7tech.security.saml.SubjectStatement;
import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.token.UsernameToken;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.DecorationRequirements.SimpleSecureConversationSession;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.X509BinarySecurityTokenImpl;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import com.l7tech.xml.MessageNotSoapException;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.xml.soap.SoapUtil;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Tests that ensure that the WssDecorator can produce output for various configurations without throwing an
 * exception.  In most cases these particular tests completely ignore the content of the output, other than to log it.
 * For tests that actually look at the output to see if it makes sense, see WssRoundTripTest.
 *
 * @author mike
 */
public class WssDecoratorTest {
    private static Logger log = Logger.getLogger(WssDecoratorTest.class.getName());
    private static final String ACTOR_NONE = "";
    public static final String TEST_WSSC_SESSION_ID = "http://www.layer7tech.com/uuid/mike/myfunkytestsessionid";

    @BeforeClass
    public static void beforeClass() {
        JceProvider.init();
        SyspropUtil.setProperty(SoapUtil.PROPERTY_DISCLOSE_ELEMENT_NAME_IN_WSU_ID, "true");
    }

    public static class Context {
        public Message messageMessage;
        public Document message;
        public String soapNs;
        public Element body;
        public String payloadNs;
        public Element payload;
        public Element price;
        public Element amount;
        public Element productid;
        public Element accountid;

        Context() throws IOException, SAXException {
            message = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
            messageMessage = new Message(message,0);
            soapNs = message.getDocumentElement().getNamespaceURI();
            body = (Element)message.getElementsByTagNameNS(soapNs, SoapUtil.BODY_EL_NAME).item(0);
            assertNotNull(body);
            payload = DomUtils.findFirstChildElement(body);
            assertNotNull(payload);
            payloadNs = payload.getNamespaceURI();
            price = (Element)message.getElementsByTagNameNS("", "price").item(0);
            assertNotNull(price);
            amount = (Element)message.getElementsByTagNameNS("", "amount").item(0);
            assertNotNull(amount);
            productid = (Element)message.getElementsByTagNameNS("", "productid").item(0);
            assertNotNull(productid);
            accountid = (Element)message.getElementsByTagNameNS("", "accountid").item(0);
            assertNotNull(accountid);
        }

        Context(Document message) throws IOException, SAXException {
            this(new Message(message,0));
        }

        Context(Message messageMessage) throws IOException, SAXException {
            this.messageMessage = messageMessage;
            this.message = messageMessage.getXmlKnob().getDocumentReadOnly();
            soapNs = message.getDocumentElement().getNamespaceURI();
            body = (Element)message.getElementsByTagNameNS(soapNs, SoapUtil.BODY_EL_NAME).item(0);
            if (body != null)
                payload = DomUtils.findFirstChildElement(body);
            if (payload != null)
                payloadNs = payload.getNamespaceURI();
        }
    }

    public static class TestDocument {
        public Context c;
        public PrivateKey recipientKey;
        public DecorationRequirements req = new DecorationRequirements();
        public SecurityTokenResolver securityTokenResolver = null;

        public TestDocument(Context c, DecorationRequirements req, PrivateKey recipientKey, SecurityTokenResolver securityTokenResolver) {
            this.c = c;
            this.recipientKey = recipientKey;
            this.req = req;
            this.securityTokenResolver = securityTokenResolver;
        }

        public TestDocument(Context c, X509Certificate senderCert, PrivateKey senderKey,
                            X509Certificate recipientCert, PrivateKey recipientKey,
                            boolean signTimestamp,
                            Element[] elementsToEncrypt,
                            Element[] elementsToSign) throws SAXException {
            this(c, null, senderCert, senderKey, recipientCert, recipientKey, signTimestamp,
                 elementsToEncrypt, elementsToSign, null, false, KeyInfoInclusionType.CERT);
        }

        public TestDocument(Context c, Element senderSamlAssertion, X509Certificate senderCert, PrivateKey senderKey,
                            X509Certificate recipientCert, PrivateKey recipientKey,
                            boolean signTimestamp,
                            Element[] elementsToEncrypt,
                            Element[] elementsToSign,
                            byte[] secureConversationKey,
                            boolean signSamlToken,
                            KeyInfoInclusionType keyInfoInclusionType) throws SAXException {
            this(c, senderSamlAssertion, senderCert, senderKey, recipientCert, recipientKey, signTimestamp,
                 elementsToEncrypt, null, elementsToSign, secureConversationKey, signSamlToken, keyInfoInclusionType, false, null, null, null, null, false, false);
        }

        public TestDocument(Context c,
                            Element senderSamlAssertion,
                            X509Certificate senderCert, PrivateKey senderKey,
                            X509Certificate recipientCert, PrivateKey recipientKey,
                            boolean signTimestamp,
                            Element[] elementsToEncrypt, String encryptionAlgorithm,
                            Element[] elementsToSign,
                            byte[] secureConversationKey,
                            boolean signSamlToken,
                            KeyInfoInclusionType keyInfoInclusionType,
                            boolean encryptUsernameToken,
                            String encryptedKeySha1,
                            String[] signatureConfirmations,
                            String actor,
                            UsernameToken senderUsernameToken,
                            boolean useDerivedKeys,
                            boolean signUsernameToken) throws SAXException {
            this.c = c;
            req.setSenderSamlToken(senderSamlAssertion == null ? null : SamlAssertion.newInstance(senderSamlAssertion), signSamlToken);
            req.setSenderMessageSigningCertificate(senderCert);
            req.setSenderMessageSigningPrivateKey(senderKey);
            req.setRecipientCertificate(recipientCert);
            this.recipientKey = recipientKey;

            req.setEncryptUsernameToken(encryptUsernameToken);
            req.setUsernameTokenCredentials(senderUsernameToken);

            initSecureConvSource(secureConversationKey, encryptedKeySha1);

            if (elementsToEncrypt != null) {
                for (Element element : elementsToEncrypt) {
                    req.addElementToEncrypt(element);
                }
            }
            if (encryptionAlgorithm != null) req.setEncryptionAlgorithm(encryptionAlgorithm);
            if (elementsToSign != null) req.getElementsToSign().addAll(Arrays.asList(elementsToSign));
            if (signTimestamp) req.setSignTimestamp(true);
            req.setSignUsernameToken(signUsernameToken);


            req.setKeyInfoInclusionType(keyInfoInclusionType);
            req.setEncryptedKeyReferenceInfo(KeyInfoDetails.makeEncryptedKeySha1Ref(encryptedKeySha1));
            if (signatureConfirmations != null) {
                for(String confirmedSignature : signatureConfirmations)
                req.addSignatureConfirmation(confirmedSignature);
            }
            if (actor != null) req.setSecurityHeaderActor(actor.length() < 1 ? null : actor);
            req.setUseDerivedKeys(useDerivedKeys);
        }

        private void initSecureConvSource(byte[] secureConversationKey, String encryptedKeySha1) {
            if (secureConversationKey != null) {
                if (encryptedKeySha1 == null) {
                    // Use WS-SecureConversation derived key token
                    req.setSecureConversationSession(new SimpleSecureConversationSession(
                            TEST_WSSC_SESSION_ID,
                            secureConversationKey,
                            SoapUtil.WSSC_NAMESPACE
                    ));
                } else {
                    // Use KeyInfo #EncryptedKeySHA1, referencing implicit EncryptedKey which recipient is expected
                    // to already possess.
                    req.setEncryptedKey(secureConversationKey);
                    req.setEncryptedKeyReferenceInfo(KeyInfoDetails.makeEncryptedKeySha1Ref(encryptedKeySha1));
                }
            }
        }
    }

    private WssDecorator.DecorationResult runTest(final TestDocument d) throws Exception {
        return runTest( d, null );
    }

    private WssDecorator.DecorationResult runTest(final TestDocument d, final Functions.UnaryVoid<Document> verifier) throws Exception {
        WssDecorator decorator = new WssDecoratorImpl();
        log.info("Before decoration (*note: pretty-printed):" + XmlUtil.nodeToFormattedString(d.c.message));

        WssDecorator.DecorationResult results = decorator.decorateMessage(new Message(d.c.message,0), d.req);

        log.info("Decorated message (*note: pretty-printed):" + XmlUtil.nodeToFormattedString(d.c.message));

        if ( verifier != null ) {
            verifier.call( d.c.message );
        }

        return results;
    }

    /**
     * Simple verifier for some basic checks
     */
    private Functions.UnaryVoid<Document> verifier( final boolean bst,
                                                    final int derivedKeys,
                                                    final boolean signature ) {
        return new Functions.UnaryVoid<Document>() {
            @Override
            public void call( final Document document ) {
                final Element securityHeader;
                try {
                    securityHeader = SoapUtil.getSecurityElementForL7( document );
                } catch ( InvalidDocumentFormatException e ) {
                    throw ExceptionUtils.wrap( e );
                }
                final int bstCount = XmlUtil.findChildElementsByName( securityHeader, SoapConstants.WS_SECURITY_NAMESPACE_LIST.toArray(new String[SoapConstants.WS_SECURITY_NAMESPACE_LIST.size()]), "BinarySecurityToken" ).size();
                final int derivedKeyCount = XmlUtil.findChildElementsByName( securityHeader, SoapConstants.WSSC_NAMESPACE_ARRAY, "DerivedKeyToken" ).size();
                final int signatureCount = XmlUtil.findChildElementsByName( securityHeader, SoapConstants.DIGSIG_URI, "Signature" ).size();

                assertTrue("Message does not contain the expected BinarySecurityToken", !bst || bstCount>0);
                assertTrue("Message contains an unexpected BinarySecurityToken", bst || bstCount==0);
                assertEquals("Derived key count", derivedKeys, derivedKeyCount);
                assertTrue("Message does not contain the expected Signature", !signature || signatureCount>0);
                assertTrue("Message contains an unexpected Signature", signature || signatureCount==0);
            }
        };
    }

    @Test
	public void testSimpleDecoration() throws Exception {
        runTest(getSimpleTestDocument());
    }

    @Test
	public void testUsernameTokenDecoration() throws Exception {
        WssDecorator decorator = new WssDecoratorImpl();
        Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        log.info("Before decoration:" + XmlUtil.nodeToFormattedString(doc));
        DecorationRequirements reqs = new DecorationRequirements();
        reqs.setUsernameTokenCredentials(new UsernameTokenImpl("franco", "blahblah".toCharArray()));

        decorator.decorateMessage(new Message(doc,0), reqs);
        log.info("Decorated message:" + XmlUtil.nodeToFormattedString(doc));
    }

    public TestDocument getSimpleTestDocument() throws Exception {
        return new TestDocument(new Context(),
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                false,
                                new Element[0],
                                new Element[0]);
    }

    /* we no longer support this
    @Test
	public void testWrappedSecurityHeader() throws Exception {
        runTest(getWrappedSecurityHeaderTestDocument());
    }*/

    public TestDocument getWrappedSecurityHeaderTestDocument() throws Exception {
        Context c = new Context();

        Element sec = SoapUtil.getOrMakeSecurityElement(c.message);
        final String privUri = "http://example.com/ws/security/stuff";
        Element privateStuff = c.message.createElementNS(privUri, "privateStuff");
        privateStuff.setPrefix("priv");
        privateStuff.setAttributeNS( XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:priv", privUri);
        sec.appendChild(privateStuff);

        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                false,
                                new Element[0],
                                new Element[0]);
    }

    @Test
	public void testSigningOnly() throws Exception {
        runTest(getSigningOnlyTestDocument());
    }

    @Test
	public void testSigningOnly_dsa_sha1() throws Exception {
        runTest(getSigningOnly_dsa_sha1_TestDocument());
    }

    public TestDocument getSigningOnly_dsa_sha1_TestDocument() throws Exception {
        final TestDocument td = getSigningOnlyTestDocument();
        Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("DSA_1024");
        td.req.setSenderMessageSigningCertificate(k.left);
        td.req.setSenderMessageSigningPrivateKey(k.right);
        td.req.setSignatureMessageDigest("SHA-1");
        return td;
    }

    @Ignore("This actually passes, but is misleading, since we currently just ignore the SHA-256 for DSA and just use SHA-1")
    @Test
	public void testSigningOnly_dsa_sha256() throws Exception {
        runTest(getSigningOnly_dsa_sha256_TestDocument());
    }

    public TestDocument getSigningOnly_dsa_sha256_TestDocument() throws Exception {
        final TestDocument td = getSigningOnlyTestDocument();
        Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("DSA_1024");
        td.req.setSenderMessageSigningCertificate(k.left);
        td.req.setSenderMessageSigningPrivateKey(k.right);
        td.req.setSignatureMessageDigest("SHA-256");
        return td;
    }

    @Test
	public void testSigningOnly_rsa_sha1() throws Exception {
        runTest(getSigningOnly_rsa_sha1_TestDocument());
    }

    public TestDocument getSigningOnly_rsa_sha1_TestDocument() throws Exception {
        final TestDocument td = getSigningOnlyTestDocument();
        Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("RSA_512");
        td.req.setSenderMessageSigningCertificate(k.left);
        td.req.setSenderMessageSigningPrivateKey(k.right);
        td.req.setSignatureMessageDigest("SHA-1");
        return td;
    }

    @Test
	public void testSigningOnly_rsa_sha256() throws Exception {
        runTest(getSigningOnly_rsa_sha256_TestDocument());
    }

    public TestDocument getSigningOnly_rsa_sha256_TestDocument() throws Exception {
        final TestDocument td = getSigningOnlyTestDocument();
        Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("RSA_512");
        td.req.setSenderMessageSigningCertificate(k.left);
        td.req.setSenderMessageSigningPrivateKey(k.right);
        td.req.setSignatureMessageDigest("SHA-256");
        return td;
    }

    @Test
	public void testSigningOnly_rsa_sha384() throws Exception {
        runTest(getSigningOnly_rsa_sha384_TestDocument());
    }

    public TestDocument getSigningOnly_rsa_sha384_TestDocument() throws Exception {
        final TestDocument td = getSigningOnlyTestDocument();
        Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("RSA_768");
        td.req.setSenderMessageSigningCertificate(k.left);
        td.req.setSenderMessageSigningPrivateKey(k.right);
        td.req.setSignatureMessageDigest("SHA-384");
        return td;
    }

    @Test
	public void testSigningOnly_rsa_sha512() throws Exception {
        runTest(getSigningOnly_rsa_sha512_TestDocument());
    }

    public TestDocument getSigningOnly_rsa_sha512_TestDocument() throws Exception {
        final TestDocument td = getSigningOnlyTestDocument();
        Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("RSA_1024");
        td.req.setSenderMessageSigningCertificate(k.left);
        td.req.setSenderMessageSigningPrivateKey(k.right);
        td.req.setSignatureMessageDigest("SHA-512");
        return td;
    }

    @Test
	public void testSigningOnly_ec_sha1() throws Exception {
        runTest(getSigningOnly_ec_sha1_TestDocument());
    }

    public TestDocument getSigningOnly_ec_sha1_TestDocument() throws Exception {
        final TestDocument td = getSigningOnlyTestDocument();
        Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("EC_secp256r1");
        td.req.setSenderMessageSigningCertificate(k.left);
        td.req.setSenderMessageSigningPrivateKey(k.right);
        td.req.setSignatureMessageDigest("SHA-1");
        return td;
    }

    @Test
	public void testSigningOnly_ec_sha256() throws Exception {
        runTest(getSigningOnly_ec_sha256_TestDocument());
    }

    public TestDocument getSigningOnly_ec_sha256_TestDocument() throws Exception {
        final TestDocument td = getSigningOnlyTestDocument();
        Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("EC_secp256r1");
        td.req.setSenderMessageSigningCertificate(k.left);
        td.req.setSenderMessageSigningPrivateKey(k.right);
        td.req.setSignatureMessageDigest("SHA-256");
        return td;
    }

    @Test
	public void testSigningOnly_ec_sha384() throws Exception {
        runTest(getSigningOnly_ec_sha384_TestDocument());
    }

    public TestDocument getSigningOnly_ec_sha384_TestDocument() throws Exception {
        final TestDocument td = getSigningOnlyTestDocument();
        Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("EC_secp384r1");
        td.req.setSenderMessageSigningCertificate(k.left);
        td.req.setSenderMessageSigningPrivateKey(k.right);
        td.req.setSignatureMessageDigest("SHA-384");
        return td;
    }

    @Test
	public void testSigningOnly_ec_sha512() throws Exception {
        runTest(getSigningOnly_ec_sha512_TestDocument());
    }

    public TestDocument getSigningOnly_ec_sha512_TestDocument() throws Exception {
        final TestDocument td = getSigningOnlyTestDocument();
        Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("EC_secp521r1");
        td.req.setSenderMessageSigningCertificate(k.left);
        td.req.setSenderMessageSigningPrivateKey(k.right);
        td.req.setSignatureMessageDigest("SHA-512");
        return td;
    }

    @Test
	public void testSigningOnlyWithProtectTokens() throws Exception {
        runTest(getSigningOnlyWithProtectTokensTestDocument());
    }

    public TestDocument getSigningOnlyWithProtectTokensTestDocument() throws Exception {
        final Context c = new Context();
        final TestDocument td = new TestDocument(c,
                TestDocuments.getEttkClientCertificate(),
                TestDocuments.getEttkClientPrivateKey(),
                TestDocuments.getDotNetServerCertificate(),
                TestDocuments.getDotNetServerPrivateKey(),
                true,
                new Element[0],
                new Element[]{c.body});
        td.req.setProtectTokens(true);
        return td;
    }

    @Test
	public void testSigningProtectTokenNoBst() throws Exception {
        runTest(getSigningProtectTokenNoBstTestDocument());
    }

    public TestDocument getSigningProtectTokenNoBstTestDocument() throws Exception {
        final Context c = new Context();
        final TestDocument td = new TestDocument(c,
                TestDocuments.getWssInteropAliceCert(),
                TestDocuments.getWssInteropAliceKey(),
                TestDocuments.getWssInteropBobCert(),
                TestDocuments.getWssInteropBobKey(),
                true,
                new Element[0],
                new Element[]{c.body});
        td.req.setKeyInfoInclusionType(KeyInfoInclusionType.STR_SKI);
        td.req.setProtectTokens(true);
        return td;
    }

    @Test
	public void testGoogleProblem() throws Exception {
        TestDocument doc = getGoogleTestDocument();
        Message msg = new Message(doc.c.message,0);
        assertTrue(msg.isSoap());
        runTest(getGoogleTestDocument());
    }

    public TestDocument getGoogleTestDocument() throws Exception {
        Document googleDoc = TestDocuments.getTestDocument(TestDocuments.DIR + "badgoogle.xml");
        final Context c = new Context(googleDoc);
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true,
                                new Element[]{c.body}, new Element[0]);
    }

    public TestDocument getSigningOnlyTestDocument() throws Exception {
        final Context c = new Context();
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true,
                                new Element[0],
                                new Element[]{c.body});
    }

    @Test
	public void testSigningHirezTimestamps() throws Exception {
        TestDocument td = getSigningOnlyTestDocument();

        WssDecorator decorator = new WssDecoratorImpl();
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setSenderMessageSigningCertificate(td.req.getSenderMessageSigningCertificate());
        dreq.setSenderMessageSigningPrivateKey(td.req.getSenderMessageSigningPrivateKey());
        dreq.setTimestampCreatedDate(new Date());
        dreq.setIncludeTimestamp(true);
        dreq.setSignTimestamp(true);

        Pattern findCreated = Pattern.compile("<[^ :>]*:?created[^<]*", Pattern.CASE_INSENSITIVE);

        for (int i = 0; i < 10; ++i) {
            final Document doc = XmlUtil.stringToDocument(XmlUtil.nodeToString(td.c.message));
            dreq.getElementsToSign().clear();
            //noinspection unchecked
            dreq.getElementsToSign().add(SoapUtil.getBodyElement(doc));
            decorator.decorateMessage(new Message(doc,0),
                                      dreq);
            final Matcher matcher = findCreated.matcher(XmlUtil.nodeToString(doc));
            if (matcher.find()) {
                System.out.println(matcher.group());
            }
        }
    }

    @Test
	public void testEncryptionOnly() throws Exception {
        runTest(getEncryptionOnlyTestDocument());
    }

    public TestDocument getEncryptionOnlyTestDocument() throws Exception {
        return getEncryptionOnlyTestDocument(XencAlgorithm.AES_128_CBC.getXEncName());
    }

    public TestDocument getEncryptionOnlyTestDocument(String encryptionAlgorithm) throws Exception {
        Context c = new Context();
        TestDocument testDocument = new TestDocument(c,
                                                     TestDocuments.getEttkClientCertificate(),
                                                     TestDocuments.getEttkClientPrivateKey(),
                                                     TestDocuments.getDotNetServerCertificate(),
                                                     TestDocuments.getDotNetServerPrivateKey(),
                                                     false,
                                                     new Element[]{c.body},
                                                     new Element[0]);
        testDocument.req.setEncryptionAlgorithm(encryptionAlgorithm);
        return testDocument;
    }

    @Test
	public void testSingleSignatureMultipleEncryption() throws Exception {
        runTest(getSingleSignatureMultipleEncryptionTestDocument());
    }

    public TestDocument getSingleSignatureMultipleEncryptionTestDocument() throws Exception {
        Context c = new Context();
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true,
                                new Element[]{c.productid, c.accountid},
                                new Element[]{c.body});
    }

    @Test
	public void testEncryptedBodySignedEnvelope() throws Exception {
        runTest(getEncryptedBodySignedEnvelopeTestDocument());
    }

    public TestDocument getEncryptedBodySignedEnvelopeTestDocument() throws Exception {
        Context c = new Context();
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                false,
                                new Element[]{c.body},
                                new Element[]{c.message.getDocumentElement()});
    }

    public TestDocument getSignedEnvelopeTestDocument() throws Exception {
        Context c = new Context();
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                false,
                                new Element[0],
                                new Element[]{c.message.getDocumentElement()});
    }

    @Test
	public void testSkilessRecipientCert() throws Exception {
        runTest(getSkilessRecipientCertTestDocument());
    }

    public TestDocument getSkilessRecipientCertTestDocument() throws Exception {
        Context c = new Context();
        return new TestDocument(c,
                                TestDocuments.getDotNetServerCertificate(), // reversed, so skiless IBM key is recip
                                TestDocuments.getDotNetServerPrivateKey(),
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                true,
                                new Element[]{c.body},
                                new Element[]{c.body});
    }

    @Test
	public void testNonsensicalSignedBodyEncryptedEnvelope() throws Exception {
        runTest(getNonsensicalSignedBodyEncryptedEnvelope());
    }

    public TestDocument getNonsensicalSignedBodyEncryptedEnvelope() throws Exception {
        Context c = new Context();
        return new TestDocument(c,
                                TestDocuments.getDotNetServerCertificate(), // reversed, so skiless IBM key is recip
                                TestDocuments.getDotNetServerPrivateKey(),
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                true,
                                new Element[]{c.message.getDocumentElement()},
                                new Element[]{c.body});
    }

    @Test
	public void testSigningOnlyWithSecureConversation() throws Exception {
        runTest(getSigningOnlyWithSecureConversationTestDocument());
    }

    public TestDocument getSigningOnlyWithSecureConversationTestDocument() throws Exception {
        final Context c = new Context();
        return new TestDocument(c, null, null, null, null, null, true,
                                new Element[0],
                                new Element[]{c.body},
                                TestDocuments.getDotNetSecureConversationSharedSecret(), false, KeyInfoInclusionType.CERT);
    }

    @Test
	public void testSigningAndEncryptionWithSecureConversation() throws Exception {
        runTest(getSigningAndEncryptionWithSecureConversationTestDocument());
    }

    public TestDocument getSigningAndEncryptionWithSecureConversationTestDocument() throws Exception {
        final Context c = new Context();
        return new TestDocument(c, null, null, null, null, null, true,
                                new Element[]{c.productid, c.accountid},
                                new Element[]{c.body},
                                TestDocuments.getDotNetSecureConversationSharedSecret(), false, KeyInfoInclusionType.CERT);
    }

    @Test
    public void testSigningWithSecureConversation2005WithSct() throws Exception {
        runTest(getSigningWithSecureConversation2005WithSctTestDocument());
    }

    public TestDocument getSigningWithSecureConversation2005WithSctTestDocument() throws Exception {
        final Context c = new Context();
        TestDocument td = new TestDocument(c, null, null, null, null, null, true,
                                new Element[]{c.productid, c.accountid},
                                new Element[]{c.body},
                                TestDocuments.getDotNetSecureConversationSharedSecret(), false, KeyInfoInclusionType.CERT);
        td.req.setSecureConversationSession(new SimpleSecureConversationSession(
                "urn:layer7_test_sct:00001",
                TestDocuments.getDotNetSecureConversationSharedSecret(),
                SoapConstants.WSSC_NAMESPACE2));
        td.req.setOmitSecurityContextToken(false);
        return td;
    }

    @Test
    public void testSigningWithSecureConversation2005WithoutSct() throws Exception {
        runTest(getSigningWithSecureConversation2005WithoutSctTestDocument());
    }

    public TestDocument getSigningWithSecureConversation2005WithoutSctTestDocument() throws Exception {
        final Context c = new Context();
        TestDocument td = new TestDocument(c, null, null, null, null, null, true,
                                new Element[]{c.productid, c.accountid},
                                new Element[]{c.body},
                                TestDocuments.getDotNetSecureConversationSharedSecret(), false, KeyInfoInclusionType.CERT);
        td.req.setSecureConversationSession(new SimpleSecureConversationSession(
                "urn:layer7_test_sct:00001",
                TestDocuments.getDotNetSecureConversationSharedSecret(),
                SoapConstants.WSSC_NAMESPACE2));
        td.req.setOmitSecurityContextToken(true);
        return td;
    }

    @Test
    public void testSigningWithSecureConversation2004WithoutSct() throws Exception {
        runTest(getSigningWithSecureConversation2004WithoutSctTestDocument());
    }

    public TestDocument getSigningWithSecureConversation2004WithoutSctTestDocument() throws Exception {
        final Context c = new Context();
        TestDocument td = new TestDocument(c, null, null, null, null, null, true,
                                new Element[]{c.productid, c.accountid},
                                new Element[]{c.body},
                                TestDocuments.getDotNetSecureConversationSharedSecret(), false, KeyInfoInclusionType.CERT);
        td.req.setOmitSecurityContextToken(true);
        return td;
    }

    @Test
	public void testSignedSamlHolderOfKeyRequest() throws Exception {
        runTest(getSignedSamlHolderOfKeyRequestTestDocument(1));
    }

    public TestDocument getSignedSamlHolderOfKeyRequestTestDocument(int version) throws Exception {
        final Context c = new Context();
        Element senderSamlToken = createSenderSamlToken(null,
                                                        TestDocuments.getEttkClientCertificate(),
                TestDocuments.getDotNetServerCertificate(),
                                                        TestDocuments.getDotNetServerPrivateKey(),
                                                        version);
        return new TestDocument(c,
                                senderSamlToken,
                                null,
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true,
                                new Element[0],
                                new Element[]{c.body},
                                null, false, KeyInfoInclusionType.CERT);
    }

    @Test
	public void testSignedSamlSenderVouchesRequest() throws Exception {
        runTest(getSignedSamlSenderVouchesRequestTestDocument(1));
    }

    public TestDocument getSignedSamlSenderVouchesRequestTestDocument(int version) throws Exception {
        final Context c = new Context();
        Element senderSamlToken = createSenderSamlToken("fbunky",
                                                        null,
                                                        TestDocuments.getDotNetServerCertificate(),
                                                        TestDocuments.getDotNetServerPrivateKey(),
                                                        version);
        return new TestDocument(c,
                                senderSamlToken,
                                TestDocuments.getEttkServerCertificate(),
                                TestDocuments.getEttkServerPrivateKey(),
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                true,
                                new Element[0],
                                new Element[]{c.body},
                                null,
                                true, KeyInfoInclusionType.CERT);
    }

    private Element createSenderSamlToken(String subjectNameIdentifierValue,
                                          X509Certificate subjectCert,
                                          X509Certificate issuerCert,
                                          PrivateKey issuerPrivateKey,
                                          int version)
      throws Exception {
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setClientAddress(InetAddress.getLocalHost());
        samlOptions.setVersion(version);
        SignerInfo si = new SignerInfo(issuerPrivateKey, new X509Certificate[]{issuerCert});
        LoginCredentials creds;
        SubjectStatement.Confirmation confirmationMethod;
        if (subjectCert != null) {
            // Subject identified by cert
            X509BinarySecurityTokenImpl token = new X509BinarySecurityTokenImpl(subjectCert, null);
            token.onPossessionProved();
            creds = LoginCredentials.makeLoginCredentials(
                    token,
                    RequireWssX509Cert.class);
            confirmationMethod = SubjectStatement.HOLDER_OF_KEY;
        } else {
            // Subject identified by nameIdentifier
            creds = LoginCredentials.makeLoginCredentials(new HttpBasicToken(subjectNameIdentifierValue, "secret".toCharArray()), HttpBasic.class);
            confirmationMethod = SubjectStatement.SENDER_VOUCHES;
        }
        SubjectStatement subjectStatement = SubjectStatement.createAuthenticationStatement(creds, confirmationMethod, KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);
        SamlAssertionGenerator generator = new SamlAssertionGenerator(si);
        return generator.createAssertion(subjectStatement, samlOptions).getDocumentElement();
    }

    @Test
	public void testSignedEmptyElement() throws Exception {
        runTest(getSignedEmptyElementTestDocument());
    }

    public TestDocument getSignedEmptyElementTestDocument() throws Exception {
        final Context c = new Context();
        Element empty = DomUtils.createAndAppendElementNS(c.payload, "empty", c.payload.getNamespaceURI(), null);
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true,
                                new Element[0],
                                new Element[]{empty});

    }

    @Test
	public void testEncryptedEmptyElement() throws Exception {
        runTest(getEncryptedEmptyElementTestDocument());
    }

    public TestDocument getEncryptedEmptyElementTestDocument() throws Exception {
        final Context c = new Context();
        Element empty = DomUtils.createAndAppendElementNS(c.payload, "empty", c.payload.getNamespaceURI(), null);
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true,
                                new Element[]{empty},
                                new Element[0]);

    }

    @Test
	public void testEncryptedGooglesearchResponse() throws Exception {
        runTest(getEncryptedGooglesearchResponseTestDocument());
    }

    public TestDocument getEncryptedGooglesearchResponseTestDocument() throws Exception {
        final Context c = new Context(TestDocuments.getTestDocument(TestDocuments.GOOGLESEARCH_RESPONSE));
        Element ret = DomUtils.findFirstChildElement(c.payload);
        assertNotNull(ret);
        assertTrue(ret.getLocalName().equals("return")); // make sure we found the right one
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true,
                                new Element[]{ret},
                                new Element[0]);
    }


    @Test
	public void testSignedGooglesearchResponse() throws Exception {
        runTest(getSignedGooglesearchResponseTestDocument());
    }

    public TestDocument getSignedGooglesearchResponseTestDocument() throws Exception {
        final Context c = new Context(TestDocuments.getTestDocument(TestDocuments.GOOGLESEARCH_RESPONSE));
        Element ret = DomUtils.findFirstChildElement(c.payload);
        assertNotNull(ret);
        assertTrue(ret.getLocalName().equals("return")); // make sure we found the right one
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true,
                                new Element[0],
                                new Element[]{ret});
    }

    @Test
	public void testNonSoapRequest() throws Exception {
        try {
            runTest(getNonSoapRequestTestDocument());
            fail("Expected MessageNotSoapException was not thrown");
        } catch (MessageNotSoapException e) {
            // ok
        }
    }

    public TestDocument getNonSoapRequestTestDocument() throws Exception {
        final Context c = new Context(TestDocuments.getTestDocument(TestDocuments.NON_SOAP_REQUEST));
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                false,
                                new Element[0],
                                new Element[0]);
    }

    @Test
	public void testSoapWithUnsignedAttachment() throws Exception {
        runTest(getSoapWithUnsignedAttachmentTestDocument());
    }

    public TestDocument getSoapWithUnsignedAttachmentTestDocument() throws Exception {
        final Context c = new Context(XmlUtil.stringToDocument(SOAP));
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true,
                                new Element[0],
                                new Element[0]);
    }

    @Test
	public void testSoapWithSignedAttachment() throws Exception {
        runTest(getSoapWithSignedAttachmentTestDocument());
    }

    public TestDocument getSoapWithSignedAttachmentTestDocument() throws Exception {
        final Message message = new Message();
        message.initialize(ContentTypeHeader.parseValue(MESS2_CONTENT_TYPE), MESS2.getBytes(),0);
        final Context c = new Context(message);
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true,
                                new Element[0],
                                new Element[]{c.body});
    }

    @Test
	public void testSoapWithSignedEncryptedAttachment() throws Exception {
        runTest(getSoapWithSignedEncryptedAttachmentTestDocument());
    }

    public TestDocument getSoapWithSignedEncryptedAttachmentTestDocument() throws Exception {
        final Context c = new Context(XmlUtil.stringToDocument(SOAP));
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true,
                                new Element[]{c.payload},
                                new Element[]{c.body});
    }

    @Test
	public void testSignedAndEncryptedBodyWithNoBst() throws Exception {
        runTest(getSignedAndEncryptedBodySkiTestDocument());
    }

    @Test
	public void testSignedBodyWithSki() throws Exception {
        TestDocument doc = getSignedBodyDocument(KeyInfoInclusionType.STR_SKI);
        runTest(doc);
    }

    @Test
	public void testSignedBodyWithIssuerSerial() throws Exception {
        TestDocument doc = getSignedBodyDocument(KeyInfoInclusionType.ISSUER_SERIAL);
        runTest(doc);
    }

    public TestDocument getSignedAndEncryptedBodySkiTestDocument() throws Exception {
        final Context c = new Context();
        return new TestDocument(c,
                                null,
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                true,
                                new Element[]{c.body},
                                new Element[]{c.body},
                                null,
                                false,
                                KeyInfoInclusionType.STR_SKI);
    }

    private TestDocument getSignedBodyDocument(final KeyInfoInclusionType keyInfoType) throws Exception {
        Context c = new Context();
        return new TestDocument(c,
                                null,
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                true,
                                null,
                                new Element[]{ c.body},
                                null,
                                false,
                                keyInfoType
        );
    }

    @Test
	public void testEncryptedUsernameToken() throws Exception {
        runTest(getEncryptedUsernameTokenTestDocument(), verifier( false, 0, true ) );
    }

    public TestDocument getEncryptedUsernameTokenTestDocument() throws Exception {
        final Context c = new Context();
        return new TestDocument(c, null, null, null,
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true, null, null,
                                new Element[]{c.body},
                                null, false,
                                KeyInfoInclusionType.STR_SKI,
                                true, null, new String[] {null}, null,
                                new UsernameTokenImpl("testuser", "password".toCharArray()),
                                false,
                                true);
    }

    @Test
	public void testSignedAndEncryptedUsernameToken() throws Exception {
        runTest(getSignedAndEncryptedUsernameTokenTestDocument(), verifier( false, 0, true ) );
    }

    public TestDocument getSignedAndEncryptedUsernameTokenTestDocument() throws Exception {
        final Context c = new Context();
        return new TestDocument(c, null, null, null,
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true, null, null,
                                new Element[]{c.body},
                                null, false,
                                KeyInfoInclusionType.STR_SKI,
                                true, null, new String[]{null}, null,
                                new UsernameTokenImpl("testuser", "password".toCharArray()),
                                false, true);
    }

    @Test
	public void testSignedUsernameToken() throws Exception {
        runTest(getSignedUsernameTokenTestDocument(), verifier( false, 0, true ) );
    }

    public TestDocument getSignedUsernameTokenTestDocument() throws Exception {
        final Context c = new Context();
        return new TestDocument(c,
                                null,
                                null,
                                null,
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                false,
                                null,
                                null,
                                new Element[0],
                                null,
                                false,
                                KeyInfoInclusionType.CERT,
                                false,
                                null,
                                new String[]{null},
                                null,
                                new UsernameTokenImpl("testuser", null),
                                false,
                                true);
    }

    @Test
	public void testEncryptedUsernameTokenWithDerivedKeys() throws Exception {
        runTest(getEncryptedUsernameTokenWithDerivedKeysTestDocument(), verifier( false, 2, true ) );
    }

    public TestDocument getEncryptedUsernameTokenWithDerivedKeysTestDocument() throws Exception {
        final Context c = new Context();
        return new TestDocument(c, null, null, null,
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true, null, null,
                                new Element[]{c.body},
                                null, false,
                                KeyInfoInclusionType.STR_SKI,
                                true, null, new String[]{null}, null,
                                new UsernameTokenImpl("testuser", "password".toCharArray()),
                                true,
                                true);
    }

    @Test
    @BugNumber(9802)
	public void testSignedAndEncryptedUsernameTokenWithEncryptedSignature() throws Exception {
        runTest(getSignedAndEncryptedUsernameTokenWithEncryptedSignatureTestDocument(), verifier( false, 0, false ) );
    }

    public TestDocument getSignedAndEncryptedUsernameTokenWithEncryptedSignatureTestDocument() throws Exception {
        final Context c = new Context();
        final TestDocument td = new TestDocument(c, null, null, null,
                TestDocuments.getDotNetServerCertificate(),
                TestDocuments.getDotNetServerPrivateKey(),
                true, null, null,
                new Element[]{c.body},
                null, false,
                KeyInfoInclusionType.STR_SKI,
                true, null, new String[]{null}, null,
                new UsernameTokenImpl("testuser", "password".toCharArray()),
                false, true);
        td.req.setEncryptSignature(true);
        return td;
    }

    @Test
    @BugNumber(9906)
    public void testSignedAndEncryptedUsernameTokenX509Signature() throws Exception {
        final TestDocument testDoc = getSignedAndEncryptedUsernameTokenTestDocument();
        testDoc.req.setSenderMessageSigningCertificate( TestDocuments.getEttkClientCertificate() );
        testDoc.req.setSenderMessageSigningPrivateKey( TestDocuments.getEttkClientPrivateKey() );
        testDoc.req.setPreferredSigningTokenType( DecorationRequirements.PreferredSigningTokenType.X509 );
        testDoc.req.setKeyInfoInclusionType( KeyInfoInclusionType.CERT );
        runTest(testDoc, verifier( true, 0, true ) );
    }

    @Test
	public void testWssInteropResponse() throws Exception {
        runTest(getWssInteropResponseTestDocument());
    }

    public TestDocument getWssInteropResponseTestDocument() throws Exception {
        final Context c = new Context();

        byte[] keyBytes =  new byte[] {5,2,4,5,
                                       8,7,9,6,
                                       32,4,1,55,
                                       8,7,77,7,
                                       4,55,33,22,
                                       28,55,-25,33,
                                       -120,55,66,33,
                                       83,22,44,55};

        TestDocument testDocument =
                new TestDocument(c,
                                 null,
                                 null,
                                 null,
                                 null,
                                 null,
                                 true,
                                 new Element[]{c.body},
                                 null,
                                 new Element[]{c.body},
                                 keyBytes,
                                 false,
                                 KeyInfoInclusionType.CERT,
                                 false, "abc11EncryptedKeySHA1Value11blahblahblah11==",
                                 new String[] {"abc11SignatureConfirmationValue11blahblahblah11=="},
                                 ACTOR_NONE, null, false, false);

        testDocument.req.setEncryptionAlgorithm(XencAlgorithm.AES_256_CBC.getXEncName());

        return testDocument;
    }

    public TestDocument getOaepKeyEncryptionTestDocument() throws Exception {
        final Context c = new Context();
        TestDocument td =
            new TestDocument(c,
                            null,
                            null,
                            null,
                            TestDocuments.getDotNetServerCertificate(),
                            TestDocuments.getDotNetServerPrivateKey(),
                            true,
                            null,
                            null,
                            new Element[]{c.body},
                            null,
                            false,
                            KeyInfoInclusionType.CERT,
                            false,
                            null,
                            new String[]{null},
                            null,
                            null,
                            false,
                            false);
        td.req.setKeyEncryptionAlgorithm(SoapUtil.SUPPORTED_ENCRYPTEDKEY_ALGO_2);
        return td;
    }

     private static final String MESS_PAYLOAD_NS = "urn:EchoAttachmentsService";
     private static final String SOAP = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
             "<env:Envelope xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
             "    xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
             "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
             "  <env:Body>\n" +
             "    <n1:echoOne xmlns:n1=\"" + MESS_PAYLOAD_NS + "\"\n" +
             "        env:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
             "      <file href=\"cid:-76392836.15558\"></file>\n" +
             "    </n1:echoOne>\n" +
             "  </env:Body>\n" +
             "</env:Envelope>\n";

     private static final String MESS2_BOUNDARY = "----=Part_-763936460.00306951464153826";
     private static final String MESS2_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
             MESS2_BOUNDARY + "\"; start=\"-76394136.13454\"";
     private static final String MESS2 = "------=Part_-763936460.00306951464153826\r\n" +
             "Content-Transfer-Encoding: 8bit\r\n" +
             "Content-Type: text/xml; charset=utf-8\r\n" +
             "Content-ID: -76394136.13454\r\n" +
             "\r\n" +
             "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
             "<env:Envelope xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
             "    xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
             "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
             "  <env:Body>\n" +
             "    <n1:echoOne xmlns:n1=\"urn:EchoAttachmentsService\"\n" +
             "        env:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
             "      <file href=\"cid:-76392836.13454\"></file>\n" +
             "    </n1:echoOne>\n" +
             "  </env:Body>\n" +
             "</env:Envelope>\n" +
             "\r\n" +
             "------=Part_-763936460.00306951464153826\r\n" +
             "Content-Transfer-Encoding: 8bit\r\n" +
             "Content-Type: application/octet-stream\r\n" +
             "Content-ID: <-76392836.13454>\r\n" +
             "\r\n" +
             "require 'soap/rpc/driver'\n" +
             "require 'soap/attachment'\n" +
             "\n" +
             "attachment = ARGV.shift || __FILE__\n" +
             "\n" +
             "#server = 'http://localhost:7700/'\n" +
             "server = 'http://data.l7tech.com:80/'\n" +
             "\n" +
             "driver = SOAP::RPC::Driver.new(server, 'urn:EchoAttachmentsService')\n" +
             "driver.wiredump_dev = STDERR\n" +
             "driver.add_method('echoOne', 'file')\n" +
             "\n" +
             "File.open(attachment)  do |fin|\n" +
             "  File.open('attachment.out', 'w') do |fout|\n" +
             ".fout << driver.echoOne(SOAP::Attachment.new(fin))\n" +
             "  end      \n" +
             "end\n" +
             "\n" +
             "\n" +
             "\r\n" +
             "------=Part_-763936460.00306951464153826--\r\n";


    @Test
	public void testExplicitSignatureConfirmations() throws Exception {
        runTest(getExplicitSignatureConfirmationsTestDocument());
    }

    public TestDocument getExplicitSignatureConfirmationsTestDocument() throws Exception {
        final Context c = new Context();
        TestDocument td =
                new TestDocument(c,
                        null,
                        TestDocuments.getEttkClientCertificate(),
                        TestDocuments.getEttkClientPrivateKey(),
                        TestDocuments.getDotNetServerCertificate(),
                        TestDocuments.getDotNetServerPrivateKey(),
                        true, new Element[]{ c.body },
                        null, new Element[]{ c.body },
                        null, false,
                        KeyInfoInclusionType.CERT,
                        false, null, null,
                        ACTOR_NONE,
                        null, false, false );
        td.req.addSignatureConfirmation("abc11SignatureConfirmationValue11blahblahblah11==");
        td.req.addSignatureConfirmation("abc11SignatureConfirmationValue22blahblahblah22==");
        return td;
    }

    @Test
	public void testConfigSecHdrAttributes() throws Exception {
        runTest(getConfigSecHdrAttributesTestDocument());
    }

    public TestDocument getConfigSecHdrAttributesTestDocument() throws Exception {
        Pair<X509Certificate, PrivateKey> client = new TestCertificateGenerator().subject("cn=testclient").generateWithKey();
        Pair<X509Certificate, PrivateKey> server = new TestCertificateGenerator().subject("cn=testserver").generateWithKey();

        Context c = new Context();
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setSecurityHeaderMustUnderstand(false);
        dreq.setRecipientCertificate(server.left);
        dreq.setSecurityHeaderActor("http://schemas.xmlsoap.org/soap/actor/next");
        dreq.setSenderMessageSigningCertificate(client.left);
        dreq.setSenderMessageSigningPrivateKey(client.right);
        dreq.getElementsToSign().add(c.body);
        return new TestDocument(c, dreq, server.right, null);
    }

    @Test
	public void testSysPropSecHdrMustUnderstandFalse() throws Exception {
        doTestSecHdrMustUnderstand(false);
    }

    @Test
	public void testSysPropSecHdrMustUnderstandTrue() throws Exception {
        doTestSecHdrMustUnderstand(true);
    }

    private void doTestSecHdrMustUnderstand(boolean expect) throws IOException, SAXException, InvalidDocumentFormatException, GeneralSecurityException, DecoratorException {
        String oldPropertyValue = System.getProperty(SoapUtil.PROPERTY_MUSTUNDERSTAND);
        try {
            SyspropUtil.setProperty(SoapUtil.PROPERTY_MUSTUNDERSTAND, Boolean.toString(expect));
            DecorationRequirements req = new DecorationRequirements();
            req.setUsernameTokenCredentials(new UsernameTokenImpl("joe", "sekrit".toCharArray()));
            Message msg = new Message(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT),0);
            new WssDecoratorImpl().decorateMessage(msg, req);
            Document doc = msg.getXmlKnob().getDocumentReadOnly();
            Element sechdr = SoapUtil.getSecurityElement(doc, "secure_span");
            assertEquals(expect ? "1" : "0", sechdr.getAttributeNS(doc.getDocumentElement().getNamespaceURI(), "mustUnderstand"));
        } finally {
            if (oldPropertyValue == null)
                SyspropUtil.clearProperty(SoapUtil.PROPERTY_MUSTUNDERSTAND);
            else
                SyspropUtil.setProperty(SoapUtil.PROPERTY_MUSTUNDERSTAND, oldPropertyValue);
        }
    }

    /* Testing Suite-B Crypto support */
    @Test
	public void testSigningWithEcdsaSha256Algorithm() throws Exception {
        runTest(getSuiteBSigningTestDocument(null)); // default to sha-256
    }

    @Test
	public void testSigningWithEcdsaSha384Algorithm() throws Exception {
        runTest(getSuiteBSigningTestDocument("SHA-384"));
    }

    public TestDocument getSuiteBSigningTestDocument(final String hashMD) throws Exception {
        Pair<X509Certificate, PrivateKey> client = new TestCertificateGenerator().curveName("secp384r1").subject("cn=ecctestclient").generateWithKey();
        Pair<X509Certificate, PrivateKey> server = new TestCertificateGenerator().curveName("secp384r1").subject("cn=ecctestserver").generateWithKey();

        Context c = new Context(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT_ONELINE));
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setRecipientCertificate(server.left);
        dreq.setSenderMessageSigningCertificate(client.left);
        dreq.setSenderMessageSigningPrivateKey(client.right);
        dreq.getElementsToSign().add(c.body);
        if (hashMD != null)
            dreq.setSignatureMessageDigest(hashMD);
        return new TestDocument(c, dreq, server.right, null);
    }

    @Ignore("Message level encryption using ECDH-ES not yet supported")
    @Test
	public void testEccSigEnc() throws Exception {
        runTest(getEccSigEncTestDocument());
    }

    public TestDocument getEccSigEncTestDocument() throws Exception {
        // Set up a test that attempts to encrypt some stuff for an EC server cert.
        // This will require using elliptic curve Diffie-Hellman in Ephemeral-Static mode 
        Pair<X509Certificate, PrivateKey> client = new TestCertificateGenerator().curveName("secp384r1").subject("cn=ecctestclient").generateWithKey();
        Pair<X509Certificate, PrivateKey> server = new TestCertificateGenerator().curveName("secp384r1").subject("cn=ecctestserver").generateWithKey();

        Context c = new Context(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT_ONELINE));
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setRecipientCertificate(server.left);
        dreq.setSenderMessageSigningCertificate(client.left);
        dreq.setSenderMessageSigningPrivateKey(client.right);
        dreq.getElementsToSign().add(c.body);
        dreq.addElementToEncrypt(c.body);
        dreq.setSignatureMessageDigest("SHA-384");
        return new TestDocument(c, dreq, server.right, null);
    }
    
    @Test
    public void testSignWithSamlHokSecretKey() throws Exception {
        runTest(getSignWithSamlHokSecretKeyTestDocument(), makeSamlHokSecretKeyVerifier());
    }

    TestDocument getSignWithSamlHokSecretKeyTestDocument() throws Exception {

        // Get hold of a SAML HoK token with secret key subject confirmation.
        // We currently don't support generating such beasts (and have no current requirement to do so)
        // but we have a test document that uses one.  We'll extract the token from it and use it to decorate placeorder_cleartext.
        final Document wcfInitialReq = TestDocuments.getTestDocument(TestDocuments.DIR + "wcf_unum/00_cl_rst_sct.xml");
        Element samlElement = (Element) wcfInitialReq.getElementsByTagNameNS(SoapUtil.SAML_NAMESPACE, "Assertion").item(0);
        assertNotNull(samlElement);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.canonicalize(samlElement, baos);
        String samlString = baos.toString();

        // Parse extracted SAML assertion and unwrap secret key using recipient private key
        SignerInfo[] privateKeys = TestKeysLoader.loadPrivateKeys(TestDocuments.DIR + "wcf_unum/", ".p12", "password".toCharArray(),
                "bookstoreservice_com", "bookstorests_com");
        final SimpleSecurityTokenResolver resolver = new SimpleSecurityTokenResolver(null, privateKeys);
        SamlAssertion samlAssertion = SamlAssertion.newInstance(XmlUtil.stringToDocument(samlString).getDocumentElement(), resolver);
        EncryptedKey subjectConfirmationEncryptedKey = samlAssertion.getSubjectConfirmationEncryptedKey(resolver, null);

        // Create decoration requirements that will decorate placeorder_cleartext with this assertion
        Context c = new Context();
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setSenderSamlToken(samlAssertion);
        dreq.addElementToSign(c.body);
        dreq.addElementToEncrypt(c.body);
        dreq.setEncryptedKey(subjectConfirmationEncryptedKey.getSecretKey());
        dreq.setEncryptedKeyReferenceInfo(KeyInfoDetails.makeKeyId(samlAssertion.getAssertionId(), false, SoapConstants.VALUETYPE_SAML_ASSERTIONID_SAML11));
        dreq.setUseDerivedKeys(true);

        return new TestDocument(c, dreq, null, resolver);
    }

    @Test
    @BugNumber(9965)
    public void testSignWithSamlHokSecretKeyWithSamlPreferred() throws Exception {
        runTest(getSignWithSamlHokSecretKeyWithSamlPreferredTestDocument(), makeSamlHokSecretKeyVerifier());
    }

    TestDocument getSignWithSamlHokSecretKeyWithSamlPreferredTestDocument() throws Exception {
        TestDocument td = getSignWithSamlHokSecretKeyTestDocument();
        td.req.setPreferredSigningTokenType(DecorationRequirements.PreferredSigningTokenType.SAML_HOK);
        td.req.setSenderMessageSigningPrivateKey(TestDocuments.getDotNetServerPrivateKey());
        return td;
    }

    private Functions.UnaryVoid<Document> makeSamlHokSecretKeyVerifier() {
        return new Functions.UnaryVoid<Document>() {
            @Override
            public void call(Document document) {
                assertEquals("Decorated message must include two DerivedKeyToken elements", 2, document.getElementsByTagNameNS("http://schemas.xmlsoap.org/ws/2004/04/sc", "DerivedKeyToken").getLength());
            }
        };
    }

    /**
     * Validate behaviour of WS-Addressing header signing behaviour.
     * Tests default signing of incoming WSA headers when somthing else is being signed, and not signed when nothing
     * else is. Tests explicit signing when it's requested and tests explicit not signing when it's requested.
     * 
     * @throws Exception
     */
    @Test
    @BugNumber(9661)
    public void testSignWsaAddressingBehaviour() throws Exception{
        WssDecorator decorator = new WssDecoratorImpl();
        DecorationRequirements decReq = new DecorationRequirements();

        Message msg = new Message(XmlUtil.parse(soapMsgWithWsaHeaders));

        WssDecorator.DecorationResult result = decorator.decorateMessage( msg, decReq);

        Map<String,Boolean> signatures = result.getSignatures();
        Assert.assertEquals("Nothing should have been signed", 0, signatures.size());

        Pair<X509Certificate, PrivateKey> server = new TestCertificateGenerator().subject("cn=testserver").generateWithKey();

        //reset and test always behaviour
        decorator = new WssDecoratorImpl();
        decReq = new DecorationRequirements();
        msg = new Message(XmlUtil.parse(soapMsgWithWsaHeaders));

        decReq.setRecipientCertificate(server.left);
        decReq.setWsaHeaderSignStrategy(DecorationRequirements.WsaHeaderSigningStrategy.ALWAYS_SIGN_WSA_HEADERS);
        result = decorator.decorateMessage( msg, decReq);

        signatures = result.getSignatures();
        Assert.assertEquals("Headers should have been signed", 1, signatures.size());

        //reset and test never behaviour
        decorator = new WssDecoratorImpl();
        decReq = new DecorationRequirements();
        msg = new Message(XmlUtil.parse(soapMsgWithWsaHeaders));

        decReq.setWsaHeaderSignStrategy(DecorationRequirements.WsaHeaderSigningStrategy.NEVER_SIGN_WSA_HEADERS);

        result = decorator.decorateMessage( msg, decReq);
        signatures = result.getSignatures();

        Assert.assertEquals("Nothing should have been signed", 0, signatures.size());

        //reset and test default behaviour - nothing is being signed
        decorator = new WssDecoratorImpl();
        decReq = new DecorationRequirements();
        msg = new Message(XmlUtil.parse(soapMsgWithWsaHeaders));

        //just for illustration, this is the default
        decReq.setWsaHeaderSignStrategy(DecorationRequirements.WsaHeaderSigningStrategy.DEFAULT_WSA_HEADER_SIGNING_BEHAVIOUR);

        result = decorator.decorateMessage( msg, decReq);
        signatures = result.getSignatures();

//        System.out.println(XmlUtil.nodeToFormattedString(msg.getXmlKnob().getDocumentReadOnly()));
        Assert.assertEquals("Nothing should have been signed", 0, signatures.size());

        //test and test default behaviour - something is being signed, so WSA headers should be signed.
        decorator = new WssDecoratorImpl();
        decReq = new DecorationRequirements();
        msg = new Message(XmlUtil.parse(soapMsgWithWsaHeaders));

        decReq.setRecipientCertificate(server.left);
        //just for illustration, this is the default
        decReq.setWsaHeaderSignStrategy(DecorationRequirements.WsaHeaderSigningStrategy.DEFAULT_WSA_HEADER_SIGNING_BEHAVIOUR);
        decReq.setSignTimestamp(true);
        result = decorator.decorateMessage( msg, decReq);
        System.out.println(XmlUtil.nodeToFormattedString(msg.getXmlKnob().getDocumentReadOnly()));

        signatures = result.getSignatures();
        Assert.assertEquals("Headers should have been signed", 1, signatures.size());

        //confirm elements are part of signature
        NodeList foundNodes = msg.getXmlKnob().getDocumentReadOnly().getDocumentElement().getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Reference");
        boolean foundMessage = false;
        boolean foundAction = false;

        for(int i = 0; i < foundNodes.getLength(); i++) {
            Node node = foundNodes.item(i);
            NamedNodeMap nodeMap = node.getAttributes();
            Node uriAttributeNode = nodeMap.getNamedItem("URI");
            if(uriAttributeNode != null){
                String uri = uriAttributeNode.getNodeValue();
                if(uri.contains("MessageID-")){
                    foundMessage = true;
                } else if(uri.contains("Action-")){
                    foundAction = true;
                }
            }
        }

        Assert.assertTrue("Action should have been signed", foundAction);
        Assert.assertTrue("Message should have been signed", foundMessage);
    }

    @Test
    @BugNumber(9749)
    public void testEncryptedSignature() throws Exception {
        runTest(getEncryptedSignatureTestDocument());
    }

    TestDocument getEncryptedSignatureTestDocument() throws Exception {
        final Context c = new Context();
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setSenderMessageSigningCertificate(TestDocuments.getEttkServerCertificate());
        dreq.setSenderMessageSigningPrivateKey(TestDocuments.getEttkServerPrivateKey());
        dreq.setRecipientCertificate(TestDocuments.getDotNetServerCertificate());
        dreq.setProtectTokens(true);
        dreq.setEncryptSignature(true);
        dreq.addElementToSign(c.body);
        dreq.addElementToEncrypt(c.body);
        return new TestDocument(c, dreq, TestDocuments.getDotNetServerPrivateKey(), null);
    }

    @Test
    @BugNumber(9749)
    public void testWholeElementEncryption() throws Exception {
        runTest(getTestWholeElementEncryptionTestDocument());
    }

    TestDocument getTestWholeElementEncryptionTestDocument() throws Exception {
        final Context c = new Context();
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setSenderMessageSigningCertificate(TestDocuments.getEttkServerCertificate());
        dreq.setSenderMessageSigningPrivateKey(TestDocuments.getEttkServerPrivateKey());
        dreq.setRecipientCertificate(TestDocuments.getDotNetServerCertificate());
        dreq.addElementToSign(c.body);
        dreq.addElementToEncrypt(c.productid, new ElementEncryptionConfig(false));
        return new TestDocument(c, dreq, TestDocuments.getDotNetServerPrivateKey(), null);
    }

    private String soapMsgWithWsaHeaders = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "    <s:Header>\n" +
            "        <wsa:MessageID\n" +
            "            wsu:Id=\"MessageID-1-c964f617e4a522c1be2318c705c8226b\"\n" +
            "            xmlns:wsa=\"http://www.w3.org/2005/08/addressing\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">MessageId-7ed352fd-3834-eb5d-a295-69a191c964aa</wsa:MessageID>\n" +
            "        <wsa:Action wsu:Id=\"Action-0-03bf5da2ac59e7fd95baab5734a040a7\"\n" +
            "            xmlns:wsa=\"http://www.w3.org/2005/08/addressing\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">http://ws.fraudlabs.com/MailBoxValidator</wsa:Action>\n" +
            "    </s:Header>\n" +
            "    <s:Body>\n" +
            "        <tns:MailBoxValidator xmlns:tns=\"http://ws.fraudlabs.com/\">\n" +
            "            <tns:EMAIL>string</tns:EMAIL>\n" +
            "            <tns:LICENSE>string</tns:LICENSE>\n" +
            "        </tns:MailBoxValidator>\n" +
            "    </s:Body>\n" +
            "</s:Envelope>";
}
