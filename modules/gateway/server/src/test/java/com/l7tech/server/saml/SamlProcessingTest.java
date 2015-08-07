package com.l7tech.server.saml;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.message.Message;
import com.l7tech.security.token.SamlSecurityToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.token.http.TlsClientCertToken;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.security.xml.*;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssProcessor;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.security.saml.SamlAssertionGenerator;
import com.l7tech.security.saml.SubjectStatement;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.TestDocuments;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapMessageGenerator;
import com.l7tech.util.DomUtils;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.xml.saml.SamlAssertionV1;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.server.MockServletApi;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.xmlbeans.XmlID;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML1.*;

import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class SamlProcessingTest.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SamlProcessingTest {
    private static final Logger logger = Logger.getLogger(SamlProcessingTest.class.getName());
    private static MockServletApi servletApi;
    private static ServiceDescriptor[] serviceDescriptors;
    private static SignerInfo authoritySigner;
    private static SignerInfo holderOfKeySigner;

    @BeforeClass
    public static void setUpClass() throws Exception {
        servletApi = MockServletApi.defaultMessageProcessingServletApi("com/l7tech/server/resources/testApplicationContext.xml");
        ApplicationContext context = servletApi.getApplicationContext();
        initializeServicesAndPolicies(context);

        authoritySigner = new SignerInfo(TestDocuments.getWssInteropBobKey(), TestDocuments.getWssInteropBobChain());
        holderOfKeySigner = new SignerInfo(TestDocuments.getWssInteropAliceKey(), TestDocuments.getWssInteropAliceChain());
    }

    @Before
    public void setUp() throws Exception {
        servletApi.reset();
    }

    /**
     * Test the authenticaiton assertion Holder of key. Create the authentication assertion, attach it to
     * the various sample message and use the WSS processor to verify it.
     *
     * @throws Exception
     */
    @Test
    public void testAuthenticationAssertionHolderOfKey() throws Exception {
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setClientAddress(InetAddress.getLocalHost());
        samlOptions.setAttestingEntity(holderOfKeySigner);
        SamlAssertionGenerator samlGenerator = new SamlAssertionGenerator(authoritySigner);
        SubjectStatement subjectStatement =
          SubjectStatement.createAuthenticationStatement(LoginCredentials.makeLoginCredentials(new TlsClientCertToken(holderOfKeySigner.getCertificateChain()[0]), SslAssertion.class),
              SubjectStatement.HOLDER_OF_KEY,
              KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);
        for (ServiceDescriptor serviceDescriptor : serviceDescriptors) {
            Wsdl wsdl = Wsdl.newInstance(null, new StringReader(serviceDescriptor.wsdlXml));
            wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
            SoapMessageGenerator sm = new SoapMessageGenerator();
            SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
            for (SoapMessageGenerator.Message request : requests) {
                SOAPMessage msg = request.getSOAPMessage();
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                msg.writeTo(bo);
                Document doc = XmlUtil.parse(new ByteArrayInputStream(bo.toByteArray()));
                samlGenerator.attachStatement(doc, subjectStatement, samlOptions);
                WssProcessor wssProcessor = new WssProcessorImpl();
                ProcessorResult result = wssProcessor.undecorateMessage(new Message(doc), null, null);
                XmlSecurityToken[] tokens = result.getXmlSecurityTokens();
                boolean found = false;
                for (XmlSecurityToken token : tokens) {
                    if (token instanceof SamlSecurityToken) {
                        found = true;
                    }
                }
                assertTrue("Saml security token not found", found);
            }
        }
    }

    /**
     * Test the authenticaiton assertion Sender vouches. Create the authentication
     * assertion, attach it to the various sample message and use the WSS processor
     * to verify it.
     *
     * @throws Exception
     */
    @Test
    public void testAuthenticationAssertionSenderVouches() throws Exception {
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setClientAddress(InetAddress.getLocalHost());
        samlOptions.setAttestingEntity(holderOfKeySigner);
        SamlAssertionGenerator samlGenerator = new SamlAssertionGenerator(authoritySigner);
        final String name = holderOfKeySigner.getCertificateChain()[0].getSubjectDN().getName();
          final LoginCredentials credentials = LoginCredentials.makeLoginCredentials(new HttpBasicToken(name, new char[] {}), HttpBasic.class);

        SubjectStatement subjectStatement = SubjectStatement.createAuthenticationStatement(credentials, SubjectStatement.SENDER_VOUCHES, KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);

        for (ServiceDescriptor serviceDescriptor : serviceDescriptors) {
            Wsdl wsdl = Wsdl.newInstance(null, new StringReader(serviceDescriptor.wsdlXml));
            wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
            SoapMessageGenerator sm = new SoapMessageGenerator();
            SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
            for (SoapMessageGenerator.Message request : requests) {
                SOAPMessage msg = request.getSOAPMessage();
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                msg.writeTo(bo);
                Document doc = XmlUtil.parse(new ByteArrayInputStream(bo.toByteArray()));
                samlGenerator.attachStatement(doc, subjectStatement, samlOptions);
                WssProcessor wssProcessor = new WssProcessorImpl();
                ProcessorResult result = wssProcessor.undecorateMessage(new Message(doc), null, null);
                XmlSecurityToken[] tokens = result.getXmlSecurityTokens();
                boolean found = false;
                for (XmlSecurityToken token : tokens) {
                    if (token instanceof SamlSecurityToken) {
                        found = true;
                    }
                }
                assertTrue("Saml security token not found", found);
            }
        }
    }

    /**
     * Test the authenticaiton assertion Sender vouches. Create the authentication
     * assertion, attach it to the various sample message and use the WSS processor
     * to verify it.
     *
     * @throws Exception
     */
    @Test
    public void testAuthorizationAssertionSenderVouches() throws Exception {
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setClientAddress(InetAddress.getLocalHost());
        samlOptions.setAttestingEntity(holderOfKeySigner);
        SamlAssertionGenerator samlGenerator = new SamlAssertionGenerator(authoritySigner);
        final String name = holderOfKeySigner.getCertificateChain()[0].getSubjectDN().getName();
        final LoginCredentials credentials = LoginCredentials.makeLoginCredentials(new HttpBasicToken(name, new char[] {}), HttpBasic.class);
        SubjectStatement subjectStatement = SubjectStatement.createAuthorizationStatement(
                credentials,
                SubjectStatement.SENDER_VOUCHES,
                KeyInfoInclusionType.CERT,
                "http://wheel", null, null, NameIdentifierInclusionType.FROM_CREDS, null, null, null);

        for (ServiceDescriptor serviceDescriptor : serviceDescriptors) {
            Wsdl wsdl = Wsdl.newInstance(null, new StringReader(serviceDescriptor.wsdlXml));
            wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
            SoapMessageGenerator sm = new SoapMessageGenerator();
            SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
            for (SoapMessageGenerator.Message request : requests) {
                SOAPMessage msg = request.getSOAPMessage();
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                msg.writeTo(bo);
                Document doc = XmlUtil.parse(new ByteArrayInputStream(bo.toByteArray()));
                samlGenerator.attachStatement(doc, subjectStatement, samlOptions);
                WssProcessor wssProcessor = new WssProcessorImpl();
                ProcessorResult result = wssProcessor.undecorateMessage(new Message(doc), null, null);
                XmlSecurityToken[] tokens = result.getXmlSecurityTokens();
                boolean found = false;
                for (XmlSecurityToken token : tokens) {
                    if (token instanceof SamlSecurityToken) {
                        found = true;
                    }
                }
                assertTrue("Saml security token not found", found);
            }
        }
    }

    /**
     * Test the authenticaiton assertion Holder Of Keys. Create the authentication
     * assertion, attach it to the various sample message and use the WSS processor
     * to verify it.
     *
     * @throws Exception
     */
    @Test
    public void testAuthorizationAssertionHolderOfKey() throws Exception {
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setClientAddress(InetAddress.getLocalHost());
        samlOptions.setAttestingEntity(holderOfKeySigner);
        SamlAssertionGenerator samlGenerator = new SamlAssertionGenerator(authoritySigner);
        final LoginCredentials credentials = LoginCredentials.makeLoginCredentials(new TlsClientCertToken(holderOfKeySigner.getCertificateChain()[0]), SslAssertion.class);
        SubjectStatement subjectStatement = SubjectStatement.createAuthorizationStatement(credentials,
          SubjectStatement.HOLDER_OF_KEY, KeyInfoInclusionType.CERT, "http://wheel", null, null, NameIdentifierInclusionType.FROM_CREDS, null, null, null);

        for (ServiceDescriptor serviceDescriptor : serviceDescriptors) {
            Wsdl wsdl = Wsdl.newInstance(null, new StringReader(serviceDescriptor.wsdlXml));
            wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
            SoapMessageGenerator sm = new SoapMessageGenerator();
            SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
            for (SoapMessageGenerator.Message request : requests) {
                SOAPMessage msg = request.getSOAPMessage();
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                msg.writeTo(bo);
                Document doc = XmlUtil.parse(new ByteArrayInputStream(bo.toByteArray()));
                samlGenerator.attachStatement(doc, subjectStatement, samlOptions);
                WssProcessor wssProcessor = new WssProcessorImpl();
                ProcessorResult result = wssProcessor.undecorateMessage(new Message(doc), null, null);
                XmlSecurityToken[] tokens = result.getXmlSecurityTokens();
                boolean found = false;
                for (XmlSecurityToken token : tokens) {
                    if (token instanceof SamlSecurityToken) {
                        found = true;
                    }
                }
                assertTrue("Saml security token not found", found);
            }
        }
    }

    private static void initializeServicesAndPolicies(ApplicationContext context)
            throws IOException, FindException, DeleteException, UpdateException,
            SaveException, VersionException, SAXException, PolicyAssertionException {

        ServiceAdmin serviceAdmin = (ServiceAdmin)context.getBean("serviceAdmin");
        EntityHeader[] headers = serviceAdmin.findAllPublishedServices();
        for (EntityHeader header : headers) {
            serviceAdmin.deletePublishedService(header.getStrId());
        }

        serviceDescriptors = new ServiceDescriptor[]{
            new ServiceDescriptor(TestDocuments.WSDL_DOC_LITERAL,
                                  TestDocuments.getTestDocumentAsXml(TestDocuments.WSDL_DOC_LITERAL),
                                  getAuthenticationAssertionPolicy()),
            new ServiceDescriptor(TestDocuments.WSDL_DOC_LITERAL2,
                                  TestDocuments.getTestDocumentAsXml(TestDocuments.WSDL_DOC_LITERAL2),
                                  getAuthorizationAssertionPolicy()),
            new ServiceDescriptor(TestDocuments.WSDL_DOC_LITERAL3,
                                  TestDocuments.getTestDocumentAsXml(TestDocuments.WSDL_DOC_LITERAL3),
                                  getAttributeAssertionPolicy())
        };

        for (ServiceDescriptor descriptor : serviceDescriptors) {
            PublishedService ps = new PublishedService();
            ps.setName(descriptor.name);
            ps.setWsdlXml(descriptor.wsdlXml);
            ps.getPolicy().setXml(descriptor.policyXml);
            serviceAdmin.savePublishedService(ps);
        }
    }

    private static String getAttributeAssertionPolicy() {
        SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        RequireWssSaml a = new RequireWssSaml();
        a.setAttributeStatement(samlAttributeStatement);
        Assertion policy = new AllAssertion(Arrays.asList(
            // Saml Attribute statement:
                    a,
            // Route:
            new HttpRoutingAssertion()
        ));

        return WspWriter.getPolicyXml(policy);
    }

    private static String getAuthorizationAssertionPolicy() {
        RequireWssSaml a = new RequireWssSaml();
        SamlAuthorizationStatement samlAuthorizationStatement = new SamlAuthorizationStatement();
        a.setAuthorizationStatement(samlAuthorizationStatement);
        Assertion policy = new AllAssertion(Arrays.asList(
            // Saml Authorization Statement:
                    a,
            // Route:
            new HttpRoutingAssertion()
        ));

        return WspWriter.getPolicyXml(policy);
    }

    private static String getAuthenticationAssertionPolicy() {
        SamlAuthenticationStatement samlAuthenticationStatement = new SamlAuthenticationStatement();
        RequireWssSaml a = new RequireWssSaml();
        a.setAuthenticationStatement(samlAuthenticationStatement);
        Assertion policy = new AllAssertion(Arrays.asList(
            // Saml Authentication Statement:
                    a
            // Route:
            //new HttpRoutingAssertion()
        ));

        return WspWriter.getPolicyXml(policy);
    }

    private static class ServiceDescriptor {
        final String name;
        final String wsdlXml;
        final String policyXml;

        public ServiceDescriptor(String name, String wsdlXml, String policyXml) {
            this.name = name;
            this.policyXml = policyXml;
            this.wsdlXml = wsdlXml;
        }
    }

    @Test
    public void testSenderVouchesWithThumbprint() throws Exception {
        AssertionType at = AssertionType.Factory.newInstance();
        XmlID aid = XmlID.Factory.newInstance();
        aid.setStringValue("EggSvTest-1");
        at.xsetAssertionID(aid);

        Calendar now = Calendar.getInstance();
        at.setIssueInstant(now);
        at.setIssuer("data.l7tech.com");
        at.setMajorVersion(BigInteger.ONE);
        at.setMinorVersion(BigInteger.ZERO);

        ConditionsType cond = at.addNewConditions();
        cond.setNotBefore(now);
        Calendar then = Calendar.getInstance();
        then.add(Calendar.YEAR, 25);
        cond.setNotOnOrAfter(then);

        AuthenticationStatementType ast = at.addNewAuthenticationStatement();
        ast.setAuthenticationInstant(now);
        ast.setAuthenticationMethod(SamlConstants.UNSPECIFIED_AUTHENTICATION);

        SubjectType subj = ast.addNewSubject();
        NameIdentifierType namei = subj.addNewNameIdentifier();
        namei.setFormat( SamlConstants.NAMEIDENTIFIER_X509_SUBJECT);
        namei.setStringValue("CN=mike");

        SubjectConfirmationType subjconf = subj.addNewSubjectConfirmation();
        subjconf.addConfirmationMethod("urn:oasis:names:tc:SAML:1.0:cm:sender-vouches");

        assertTrue(subjconf.validate());
        assertTrue(namei.validate());
        assertTrue(subj.validate());

        assertTrue(ast.validate());

        XmlOptions xo = new XmlOptions();
        Map<String,String> namespaces = new HashMap<String,String>();
        namespaces.put(SamlConstants.NS_SAML, SamlConstants.NS_SAML_PREFIX);
        xo.setSaveSuggestedPrefixes(namespaces);
        AssertionDocument ad = AssertionDocument.Factory.newInstance(xo);
        ad.setAssertion(at);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ad.save(bos, xo);

        Document assDoc = XmlUtil.stringToDocument(bos.toString());

        final X509Certificate signingCert =  TestDocuments.getDotNetServerCertificate();
        PrivateKey singingKey = TestDocuments.getDotNetServerPrivateKey();
        final Element assEl = assDoc.getDocumentElement();
        Element sig = createEnvelopedSignature(assEl, signingCert, singingKey);
        assEl.appendChild(sig);


        logger.info("Assertion:\n" + XmlUtil.nodeToString(assDoc));

        //XmlUtil.nodeToOutputStream(assDoc, new FileOutputStream("c:/newsamltest.xml"));

        final String assString = XmlUtil.nodeToFormattedString(assDoc);
        logger.info("\n\nAssertion (Pretty-printed): " + assString);

        // See if it validates
        AssertionDocument got = AssertionDocument.Factory.parse(assString);
        assertTrue(got.validate());

        // See if our code can deal with it
        CertUtils.getThumbprintSHA1(signingCert);
        CertUtils.getSki(signingCert);
        SecurityTokenResolver thumbResolver = new SimpleSecurityTokenResolver(signingCert);
        SamlAssertion sa = SamlAssertion.newInstance(assDoc.getDocumentElement(), thumbResolver);
        assertTrue(sa.isSenderVouches());
        assertNotNull(sa.getIssuerCertificate());
        sa.verifyEmbeddedIssuerSignature();


    }

    @Test
    public void testStuff() throws Exception {
        SubjectStatement stmt = SubjectStatement.createAttributeStatement(LoginCredentials.makeLoginCredentials(new HttpBasicToken("foo", "bar".toCharArray()), HttpBasic.class), SubjectStatement.BEARER, "foo", "urn:example.com:attributes", "bar", KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null);
        SamlAssertionGenerator.Options opts = new SamlAssertionGenerator.Options();
        SamlAssertionGenerator sag = new SamlAssertionGenerator(holderOfKeySigner);
        Document assertionDoc = sag.createAssertion(stmt, opts);

        SamlAssertion ass = new SamlAssertionV1(assertionDoc.getDocumentElement(), null);
        ass.verifyEmbeddedIssuerSignature();
    }

    @Test
    public void testAttributeStatementWithThumbprint() throws Exception {
        AssertionType at = AssertionType.Factory.newInstance();
        XmlID aid = XmlID.Factory.newInstance();
        aid.setStringValue("EggSvTest-1");
        at.xsetAssertionID(aid);

        Calendar now = Calendar.getInstance();
        at.setIssueInstant(now);
        at.setIssuer("data.l7tech.com");
        at.setMajorVersion(BigInteger.ONE);
        at.setMinorVersion(BigInteger.ZERO);

        ConditionsType cond = at.addNewConditions();
        cond.setNotBefore(now);
        Calendar then = Calendar.getInstance();
        then.add(Calendar.DATE, 1);
        cond.setNotOnOrAfter(then);

        AttributeStatementType ast = at.addNewAttributeStatement();

        SubjectType subj = ast.addNewSubject();
        NameIdentifierType namei = subj.addNewNameIdentifier();
        namei.setFormat(SamlConstants.NAMEIDENTIFIER_EMAIL);
        namei.setStringValue("mike@l7tech.com");

        SubjectConfirmationType subjconf = subj.addNewSubjectConfirmation();
        subjconf.addConfirmationMethod("urn:oasis:names:tc:SAML:1.0:cm:sender-vouches");

        assertTrue(subjconf.validate());
        assertTrue(namei.validate());
        assertTrue(subj.validate());

        AttributeType a1 = ast.addNewAttribute();
        a1.setAttributeName("ProductPortfolio");
        a1.setAttributeNamespace("http://www.egg.com/ns/attributes");
        XmlObject v1 = a1.addNewAttributeValue();
        v1.set(XmlObject.Factory.parse("<xml-fragment><xyz:ProductPortfolio xmlns:xyz=\"http://www.egg.com/ns/products\">\n" +
                                       "                                <xyz:Product>\n" +
                                       "                                    <xyz:Name>Red</xyz:Name>\n" +
                                       "                                    <xyz:Type>CreditCard</xyz:Type>\n" +
                                       "                                    <xyz:Activated>true</xyz:Activated>\n" +
                                       "                                </xyz:Product>\n" +
                                       "                                <xyz:Product>\n" +
                                       "                                    <xyz:Name>Blue</xyz:Name>\n" +
                                       "                                    <xyz:Type>CreditCard</xyz:Type>\n" +
                                       "                                    <xyz:Activated>true</xyz:Activated>\n" +
                                       "                                </xyz:Product>\n" +
                                       "                            </xyz:ProductPortfolio></xml-fragment>"));

        AttributeType a2 = ast.addNewAttribute();
        a2.setAttributeName("CustomerRiskCategory");
        a2.setAttributeNamespace("http://www.egg.com/ns/attributes");
        XmlObject v2 = a2.addNewAttributeValue();
        v2.set(XmlObject.Factory.parse("<xml-fragment>low</xml-fragment>"));

        assertTrue(ast.validate());

        XmlOptions xo = new XmlOptions();
        Map<String,String> namespaces = new HashMap<String,String>();
        namespaces.put(SamlConstants.NS_SAML, SamlConstants.NS_SAML_PREFIX);
        xo.setSaveSuggestedPrefixes(namespaces);
        AssertionDocument ad = AssertionDocument.Factory.newInstance(xo);
        ad.setAssertion(at);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ad.save(bos, xo);

        Document assDoc = XmlUtil.stringToDocument(bos.toString());

        X509Certificate signingCert =  TestDocuments.getDotNetServerCertificate();
        PrivateKey singingKey =  TestDocuments.getDotNetServerPrivateKey();
        final Element assEl = assDoc.getDocumentElement();
        Element sig = createEnvelopedSignature(assEl, signingCert, singingKey);
        assEl.appendChild(sig);


        logger.info("Assertion:\n" + XmlUtil.nodeToString(assDoc));

        //XmlUtil.nodeToOutputStream(assDoc, new FileOutputStream("c:/newsamlattr.xml"));

        final String assString = XmlUtil.nodeToFormattedString(assDoc);
        logger.info("\n\nAssertion (Pretty-printed): " + assString);

        // See if it validates
        AssertionDocument got = AssertionDocument.Factory.parse(assString);
        assertTrue(got.validate());

    }

    /**
     * Digitally sign the specified element, using the specified key and including the specified cert inline
     * in the KeyInfo.
     */
    public static Element createEnvelopedSignature(Element elementToSign,
                                                   X509Certificate senderSigningCert,
                                                   PrivateKey senderSigningKey)
            throws SignatureException, SignatureStructureException, XSignatureException {

        final Document factory = elementToSign.getOwnerDocument();
        final String wsseNs = SoapUtil.SECURITY_NAMESPACE;
        Element str = factory.createElementNS(wsseNs, "wsse:SecurityTokenReference");
        str.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:wsse", wsseNs);
        Element keyId = DomUtils.createAndAppendElementNS(str, "KeyIdentifier", wsseNs, "wsse");
        keyId.setAttribute("EncodingType", SoapUtil.ENCODINGTYPE_BASE64BINARY);
        keyId.setAttribute("ValueType", SoapUtil.VALUETYPE_SKI);
        DomUtils.setTextContent(keyId, CertUtils.getSki(senderSigningCert));

        return DsigUtil.createEnvelopedSignature(elementToSign, senderSigningCert, senderSigningKey, str, null, null);
    }

}
