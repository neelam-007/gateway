/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.xml;

import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeBodyTest;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.saml.NameIdentifierInclusionType;
import com.l7tech.common.security.saml.SamlAssertionGenerator;
import com.l7tech.common.security.saml.SubjectStatement;
import com.l7tech.common.security.token.UsernameToken;
import com.l7tech.common.security.token.UsernameTokenImpl;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.DecorationRequirements.SimpleSecureConversationSession;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mike
 * @noinspection RedundantCast
 */
public class WssDecoratorTest extends TestCase {
    private static Logger log = Logger.getLogger(WssDecoratorTest.class.getName());
    private static final String ACTOR_NONE = "";

    static {
        JceProvider.init();
    }

    public WssDecoratorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WssDecoratorTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
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
            messageMessage = new Message(message);
            soapNs = message.getDocumentElement().getNamespaceURI();
            body = (Element)message.getElementsByTagNameNS(soapNs, SoapUtil.BODY_EL_NAME).item(0);
            assertNotNull(body);
            payload = XmlUtil.findFirstChildElement(body);
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
            this(new Message(message));
        }

        Context(Message messageMessage) throws IOException, SAXException {
            this.messageMessage = messageMessage;
            this.message = messageMessage.getXmlKnob().getDocumentReadOnly();
            soapNs = message.getDocumentElement().getNamespaceURI();
            body = (Element)message.getElementsByTagNameNS(soapNs, SoapUtil.BODY_EL_NAME).item(0);
            if (body != null)
                payload = XmlUtil.findFirstChildElement(body);
            if (payload != null)
                payloadNs = payload.getNamespaceURI();
        }
    }

    // TODO: replace most of the members in this class with a single DecorationRequirements field,
    //       rather than having to maintain 2 separate bits of code that translate TestDocument into DecorationRequirements                                      
    public static class TestDocument {
        public Context c;
        public Element senderSamlAssertion; // may be used instead of senderCert
        public X509Certificate senderCert;
        public PrivateKey senderKey;
        public X509Certificate recipientCert;
        public PrivateKey recipientKey;
        public boolean signTimestamp;
        public byte[] secureConversationKey;   // may be used instead of a sender cert + sender key if using WS-SC
        public Element[] elementsToEncrypt = new Element[0];
        public Element[] elementsToSign = new Element[0];
        public String[] attachmentsToSign = new String[0];
        public boolean signAttachmentHeaders;
        public boolean signSamlToken = false; // if true, SAML token should be signed
        public KeyInfoInclusionType keyInfoInclusionType = KeyInfoInclusionType.CERT;
        public String encryptionAlgorithm = XencAlgorithm.AES_128_CBC.getXEncName(); //default
        public String encryptedKeySha1 = null;
        public String signatureConfirmation = null;
        public String actor = null;
        public boolean encryptUsernameToken = false;
        public String keyEncryptionAlgoritm;
        public UsernameToken usernameToken = null;
        public boolean useDerivedKeys = false;
        public boolean signUsernameToken = false;
        public SecurityTokenResolver securityTokenResolver = null;

        public TestDocument(Context c, X509Certificate senderCert, PrivateKey senderKey,
                            X509Certificate recipientCert, PrivateKey recipientKey,
                            boolean signTimestamp,
                            Element[] elementsToEncrypt,
                            Element[] elementsToSign) {
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
                            KeyInfoInclusionType keyInfoInclusionType) {
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
                            String signatureConfirmation,
                            String actor,
                            UsernameToken senderUsernameToken,
                            boolean useDerivedKeys, boolean signUsernameToken)
        {
            this.c = c;
            this.senderSamlAssertion = senderSamlAssertion;
            this.senderCert = senderCert;
            this.senderKey = senderKey;
            this.recipientCert = recipientCert;
            this.recipientKey = recipientKey;
            this.signTimestamp = signTimestamp;
            if (elementsToEncrypt != null)
                this.elementsToEncrypt = elementsToEncrypt;
            if (encryptionAlgorithm != null)
                this.encryptionAlgorithm = encryptionAlgorithm;
            if (elementsToSign != null)
                this.elementsToSign = elementsToSign;
            this.secureConversationKey = secureConversationKey;
            this.signSamlToken = signSamlToken;
            this.keyInfoInclusionType = keyInfoInclusionType;
            this.encryptedKeySha1 = encryptedKeySha1;
            this.signatureConfirmation = signatureConfirmation;
            this.actor = actor;
            this.encryptUsernameToken = encryptUsernameToken;
            this.usernameToken = senderUsernameToken;
            this.useDerivedKeys = useDerivedKeys;
            this.signUsernameToken = signUsernameToken;
        }
    }

    private void runTest(final TestDocument d) throws Exception {
        WssDecorator decorator = new WssDecoratorImpl();
        log.info("Before decoration (*note: pretty-printed):" + XmlUtil.nodeToFormattedString(d.c.message));
        DecorationRequirements reqs = makeDecorationRequirements(d);

        decorator.decorateMessage(new Message(d.c.message), reqs);

        log.info("Decorated message (*note: pretty-printed):" + XmlUtil.nodeToFormattedString(d.c.message));
    }

    public DecorationRequirements makeDecorationRequirements(final TestDocument d) {
        DecorationRequirements reqs = new DecorationRequirements();
        reqs.setSenderSamlToken(d.senderSamlAssertion, d.signSamlToken);
        reqs.setSenderMessageSigningCertificate(d.senderCert);
        reqs.setRecipientCertificate(d.recipientCert);
        reqs.setSenderMessageSigningPrivateKey(d.senderKey);
        reqs.setSignTimestamp();
        reqs.setUsernameTokenCredentials(d.usernameToken);
        reqs.setKeyInfoInclusionType(d.keyInfoInclusionType);
        reqs.setSignatureConfirmation(d.signatureConfirmation);
        reqs.setEncryptUsernameToken(d.encryptUsernameToken);
        reqs.setSignUsernameToken(d.signUsernameToken);
        reqs.setUseDerivedKeys(d.useDerivedKeys);
        if (d.actor != null)
            reqs.setSecurityHeaderActor(d.actor.length() < 1 ? null : d.actor);
        if (d.secureConversationKey != null) {
            if (d.encryptedKeySha1 == null) {
                // Use WS-SecureConversation derived key token
                reqs.setSecureConversationSession(new SimpleSecureConversationSession(
                        "http://www.layer7tech.com/uuid/mike/myfunkytestsessionid",
                        d.secureConversationKey,
                        SoapUtil.WSSC_NAMESPACE
                ));
            } else {
                // Use KeyInfo #EncryptedKeySHA1, referencing implicit EncryptedKey which recipient is expected
                // to already possess.
                reqs.setEncryptedKey(d.secureConversationKey);
                reqs.setEncryptedKeySha1(d.encryptedKeySha1);
            }
        }
        if (d.elementsToEncrypt != null) {
            for (Element anElementsToEncrypt : d.elementsToEncrypt) {
                //noinspection unchecked
                reqs.getElementsToEncrypt().add(anElementsToEncrypt);
            }
        }
        if (d.encryptionAlgorithm != null) {
            reqs.setEncryptionAlgorithm(d.encryptionAlgorithm);
        }
        if (d.elementsToSign != null) {
            for (Element anElementsToSign : d.elementsToSign) {
                //noinspection unchecked
                reqs.getElementsToSign().add(anElementsToSign);
            }
        }
        return reqs;
    }

    public void testSimpleDecoration() throws Exception {
        runTest(getSimpleTestDocument());
    }

    public void testUsernameTokenDecoration() throws Exception {
        WssDecorator decorator = new WssDecoratorImpl();
        Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        log.info("Before decoration:" + XmlUtil.nodeToFormattedString(doc));
        DecorationRequirements reqs = new DecorationRequirements();
        reqs.setUsernameTokenCredentials(new UsernameTokenImpl("franco", "blahblah".toCharArray()));

        decorator.decorateMessage(new Message(doc), reqs);
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
    public void testWrappedSecurityHeader() throws Exception {
        runTest(getWrappedSecurityHeaderTestDocument());
    }*/

    public TestDocument getWrappedSecurityHeaderTestDocument() throws Exception {
        Context c = new Context();

        Element sec = SoapUtil.getOrMakeSecurityElement(c.message);
        final String privUri = "http://example.com/ws/security/stuff";
        Element privateStuff = c.message.createElementNS(privUri, "privateStuff");
        privateStuff.setPrefix("priv");
        privateStuff.setAttribute("xmlns:priv", privUri);
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

    public void testSigningOnly() throws Exception {
        runTest(getSigningOnlyTestDocument());
    }

    public void testGoogleProblem() throws Exception {
        TestDocument doc = getGoogleTestDocument();
        Message msg = new Message(doc.c.message);
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

    public void testSigningHirezTimestamps() throws Exception {
        TestDocument td = getSigningOnlyTestDocument();

        WssDecorator decorator = new WssDecoratorImpl();
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setSenderMessageSigningCertificate(td.senderCert);
        dreq.setSenderMessageSigningPrivateKey(td.senderKey);
        dreq.setTimestampCreatedDate(new Date());
        dreq.setIncludeTimestamp(true);
        dreq.setSignTimestamp();

        Pattern findCreated = Pattern.compile("<[^ :>]*:?created[^<]*", Pattern.CASE_INSENSITIVE);

        for (int i = 0; i < 10; ++i) {
            final Document doc = XmlUtil.stringToDocument(XmlUtil.nodeToString(td.c.message));
            dreq.getElementsToSign().clear();
            //noinspection unchecked
            dreq.getElementsToSign().add(SoapUtil.getBodyElement(doc));
            decorator.decorateMessage(new Message(doc),
                                      dreq);
            final Matcher matcher = findCreated.matcher(XmlUtil.nodeToString(doc));
            if (matcher.find()) {
                System.out.println(matcher.group());
            }
        }
    }

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
        testDocument.encryptionAlgorithm = encryptionAlgorithm;
        return testDocument;
    }

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

    public void testSigningOnlyWithSecureConversation() throws Exception {
        runTest(getSigningOnlyWithSecureConversationTestDocument());
    }

    public TestDocument getSigningOnlyWithSecureConversationTestDocument() throws Exception {
        final Context c = new Context();
        return new TestDocument(c,
                                null, null,
                                null,
                                null,
                                null,
                                true,
                                new Element[0],
                                new Element[]{c.body},
                                TestDocuments.getDotNetSecureConversationSharedSecret(), false, KeyInfoInclusionType.CERT);
    }

    public void testSigningAndEncryptionWithSecureConversation() throws Exception {
        runTest(getSigningAndEncryptionWithSecureConversationTestDocument());
    }

    public TestDocument getSigningAndEncryptionWithSecureConversationTestDocument() throws Exception {
        final Context c = new Context();
        return new TestDocument(c,
                                null, null,
                                null,
                                null,
                                null,
                                true,
                                new Element[]{c.productid, c.accountid},
                                new Element[]{c.body},
                                TestDocuments.getDotNetSecureConversationSharedSecret(), false, KeyInfoInclusionType.CERT);
    }

    public void testSignedSamlHolderOfKeyRequest() throws Exception {
        runTest(getSignedSamlHolderOfKeyRequestTestDocument(1));
    }

    public TestDocument getSignedSamlHolderOfKeyRequestTestDocument(int version) throws Exception {
        final Context c = new Context();
        Element senderSamlToken = createSenderSamlToken(null,
                                                        TestDocuments.getEttkClientCertificate(),
                null, TestDocuments.getDotNetServerCertificate(),
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

    public void testSignedSamlSenderVouchesRequest() throws Exception {
        runTest(getSignedSamlSenderVouchesRequestTestDocument(1));
    }

    public TestDocument getSignedSamlSenderVouchesRequestTestDocument(int version) throws Exception {
        final Context c = new Context();
        Element senderSamlToken = createSenderSamlToken("fbunky",
                                                        null,
                null, TestDocuments.getDotNetServerCertificate(),
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
                                          String nameQualifier, X509Certificate issuerCert,
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
            creds = new LoginCredentials(null, null,
                                         CredentialFormat.CLIENTCERT,
                                         RequestWssX509Cert.class,
                                         null,
                                         subjectCert);
            confirmationMethod = SubjectStatement.HOLDER_OF_KEY;
        } else {
            // Subject identified by nameIdentifier
            creds = LoginCredentials.makePasswordCredentials(subjectNameIdentifierValue, "secret".toCharArray(), HttpBasic.class);
            confirmationMethod = SubjectStatement.SENDER_VOUCHES;
        }
        SubjectStatement subjectStatement = SubjectStatement.createAuthenticationStatement(creds, confirmationMethod, KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);
        SamlAssertionGenerator generator = new SamlAssertionGenerator(si);
        return generator.createAssertion(subjectStatement, samlOptions).getDocumentElement();
    }

    public void testSignedEmptyElement() throws Exception {
        runTest(getSignedEmptyElementTestDocument());
    }

    public TestDocument getSignedEmptyElementTestDocument() throws Exception {
        final Context c = new Context();
        Element empty = XmlUtil.createAndAppendElementNS(c.payload, "empty", c.payload.getNamespaceURI(), null);
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true,
                                new Element[0],
                                new Element[]{empty});

    }

    public void testEncryptedEmptyElement() throws Exception {
        runTest(getEncryptedEmptyElementTestDocument());
    }

    public TestDocument getEncryptedEmptyElementTestDocument() throws Exception {
        final Context c = new Context();
        Element empty = XmlUtil.createAndAppendElementNS(c.payload, "empty", c.payload.getNamespaceURI(), null);
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true,
                                new Element[]{empty},
                                new Element[0]);

    }

    public void testEncryptedGooglesearchResponse() throws Exception {
        runTest(getEncryptedGooglesearchResponseTestDocument());
    }

    public TestDocument getEncryptedGooglesearchResponseTestDocument() throws Exception {
        final Context c = new Context(TestDocuments.getTestDocument(TestDocuments.GOOGLESEARCH_RESPONSE));
        Element ret = XmlUtil.findFirstChildElement(c.payload);
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


    public void testSignedGooglesearchResponse() throws Exception {
        runTest(getSignedGooglesearchResponseTestDocument());
    }

    public TestDocument getSignedGooglesearchResponseTestDocument() throws Exception {
        final Context c = new Context(TestDocuments.getTestDocument(TestDocuments.GOOGLESEARCH_RESPONSE));
        Element ret = XmlUtil.findFirstChildElement(c.payload);
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

    public void testSoapWithUnsignedAttachment() throws Exception {
        runTest(getSoapWithUnsignedAttachmentTestDocument());
    }

    public TestDocument getSoapWithUnsignedAttachmentTestDocument() throws Exception {
        final Context c = new Context(XmlUtil.stringToDocument(MimeBodyTest.SOAP));
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true,
                                new Element[0],
                                new Element[0]);
    }

    public void testSoapWithSignedAttachment() throws Exception {
        runTest(getSoapWithSignedAttachmentTestDocument());
    }

    public TestDocument getSoapWithSignedAttachmentTestDocument() throws Exception {
        final Message message = new Message();
        message.initialize(ContentTypeHeader.parseValue(MimeBodyTest.MESS2_CONTENT_TYPE), MimeBodyTest.MESS2.getBytes());
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

    public void testSoapWithSignedEncryptedAttachment() throws Exception {
        runTest(getSoapWithSignedEncryptedAttachmentTestDocument());
    }

    public TestDocument getSoapWithSignedEncryptedAttachmentTestDocument() throws Exception {
        final Context c = new Context(XmlUtil.stringToDocument(MimeBodyTest.SOAP));
        return new TestDocument(c,
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                true,
                                new Element[]{c.payload},
                                new Element[]{c.body});
    }

    public void testSignedAndEncryptedBodyWithNoBst() throws Exception {
        runTest(getSignedAndEncryptedBodyWithNoBstTestDocument());
    }

    public void testSignedBodyWithNoBst() throws Exception {
        TestDocument doc = getSignedBodyWithNoBstTestDocument();
        runTest(doc);
    }

    public TestDocument getSignedAndEncryptedBodyWithNoBstTestDocument() throws Exception {
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

    public TestDocument getSignedBodyWithNoBstTestDocument() throws Exception {
        final Context c = new Context();
        return new TestDocument(c,
                                null,
                                TestDocuments.getDotNetServerCertificate(),
                                TestDocuments.getDotNetServerPrivateKey(),
                                TestDocuments.getEttkClientCertificate(),
                                TestDocuments.getEttkClientPrivateKey(),
                                true,
                                null,
                                new Element[]{c.body},
                                null,
                                false,
                                KeyInfoInclusionType.STR_SKI);
    }

    public void testEncryptedUsernameToken() throws Exception {
        runTest(getEncryptedUsernameTokenTestDocument());
    }

    public TestDocument getEncryptedUsernameTokenTestDocument() throws Exception {
        final Context c = new Context();
        return new TestDocument(c,
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
                                KeyInfoInclusionType.STR_SKI,
                                true,
                                null,
                                null,
                                null,
                                new UsernameTokenImpl("testuser", "password".toCharArray()), false, false);
    }

    public void testSignedAndEncryptedUsernameToken() throws Exception {
        runTest(getSignedAndEncryptedUsernameTokenTestDocument());
    }

    public TestDocument getSignedAndEncryptedUsernameTokenTestDocument() throws Exception {
        final Context c = new Context();
        return new TestDocument(c,
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
                                KeyInfoInclusionType.STR_SKI,
                                true,
                                null,
                                null,
                                null,
                                new UsernameTokenImpl("testuser", "password".toCharArray()),
                                false,
                                true);
    }

    public void testEncryptedUsernameTokenWithDerivedKeys() throws Exception {
        runTest(getEncryptedUsernameTokenWithDerivedKeysTestDocument());
    }

    public TestDocument getEncryptedUsernameTokenWithDerivedKeysTestDocument() throws Exception {
        final Context c = new Context();
        return new TestDocument(c,
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
                                KeyInfoInclusionType.STR_SKI,
                                true,
                                null,
                                null,
                                null,
                                new UsernameTokenImpl("testuser", "password".toCharArray()),
                                true, false);
    }

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
                                 (Element)null,
                                 (X509Certificate)null,
                                 (PrivateKey)null,
                                 (X509Certificate)null,
                                 (PrivateKey)null,
                                 true,
                                 new Element[]{c.body},
                                 (String)null,
                                 new Element[]{c.body},
                                 keyBytes,
                                 false,
                                 KeyInfoInclusionType.CERT,
                                 false, "abc11EncryptedKeySHA1Value11blahblahblah11==",
                                 "abc11SignatureConfirmationValue11blahblahblah11==",
                                 ACTOR_NONE, null, false, false);

        testDocument.encryptionAlgorithm = XencAlgorithm.AES_256_CBC.getXEncName(); 

        return testDocument;
    }

    public TestDocument getOaepKeyEncryptionTestDocument() throws Exception {
        final Context c = new Context();
        TestDocument td = new TestDocument(
                c,
                null,
                null,
                TestDocuments.getDotNetServerCertificate(),
                TestDocuments.getDotNetServerPrivateKey(),
                true,
                null,
                new Element[]{c.body});
        td.keyEncryptionAlgoritm = SoapUtil.SUPPORTED_ENCRYPTEDKEY_ALGO_2;
        return td;
    }
}
