package com.l7tech.server.saml;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.common.message.Message;
import com.l7tech.common.security.Keys;
import com.l7tech.common.security.saml.*;
import com.l7tech.common.security.token.SamlSecurityToken;
import com.l7tech.common.security.token.XmlSecurityToken;
import com.l7tech.common.security.xml.DsigUtil;
import com.l7tech.common.security.xml.SecurityTokenResolver;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.WssProcessor;
import com.l7tech.common.security.xml.processor.WssProcessorImpl;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.common.xml.saml.SamlAssertionV1;
import com.l7tech.console.util.SoapMessageGenerator;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.MockServletApi;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceAdmin;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
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
public class SamlProcessingTest extends TestCase {
    private static final Logger logger = Logger.getLogger(SamlProcessingTest.class.getName());
    private static MockServletApi servletApi;
    private static ServiceDescriptor[] serviceDescriptors;
    private static Keys authorityKeys;
    private static SignerInfo holderOfKeySigner;

    /**
     * test <code>SamlProcessingTest</code> constructor
     */
    public SamlProcessingTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * SamlProcessingTest <code>TestCase</code>
     */
    /**
     * create the <code>TestSuite</code> for the
     * ServerPolicyFactoryTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(SamlProcessingTest.class);
        TestSetup wrapper = new TestSetup(suite) {
            /**
             * sets the test environment
             *
             * @throws Exception on error deleting the stub data store
             */
            protected void setUp() throws Exception {
                servletApi = MockServletApi.defaultMessageProcessingServletApi("com/l7tech/common/testApplicationContext.xml");
                ApplicationContext context = servletApi.getApplicationContext();
                initializeServicesAndPolicies(context);
                authorityKeys = new Keys("cn=testauthority");
                holderOfKeySigner = new Keys("cn=fred").asSignerInfo();
            }

            protected void tearDown() throws Exception {
                ;
            }
        };
        return wrapper;
    }

    public void setUp() throws Exception {
        //servletApi.reset();
        //messageProcessingServlet = new SoapMessageProcessingServlet();
        //messageProcessingServlet.init(servletApi.getServletConfig());

    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    /**
     * Test the authenticaiton assertion Holder of key. Create the authentication assertion, attach it to
     * the various sample message and use the WSS processor to verify it.
     *
     * @throws Exception
     */
    public void testAuthenticationAssertionHolderOfKey() throws Exception {
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setClientAddress(InetAddress.getLocalHost());
        samlOptions.setAttestingEntity(holderOfKeySigner);
        SamlAssertionGenerator samlGenerator = new SamlAssertionGenerator(authorityKeys.asSignerInfo());
        SubjectStatement subjectStatement =
          SubjectStatement.createAuthenticationStatement(LoginCredentials.makeCertificateCredentials(holderOfKeySigner.getCertificateChain()[0], RequestWssX509Cert.class),
              SubjectStatement.HOLDER_OF_KEY,
              KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null);
        for (int i = 0; i < serviceDescriptors.length; i++) {
            ServiceDescriptor serviceDescriptor = serviceDescriptors[i];
            Wsdl wsdl = Wsdl.newInstance(null, new StringReader(serviceDescriptor.wsdlXml));
            wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
            SoapMessageGenerator sm = new SoapMessageGenerator();
            SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
            for (int j = 0; j < requests.length; j++) {
                SoapMessageGenerator.Message request = requests[j];
                SOAPMessage msg = request.getSOAPMessage();
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                msg.writeTo(bo);
                Document doc = XmlUtil.parse(new ByteArrayInputStream(bo.toByteArray()));
                samlGenerator.attachStatement(doc, subjectStatement, samlOptions);
                WssProcessor wssProcessor = new WssProcessorImpl();
                ProcessorResult result = wssProcessor.undecorateMessage(new Message(doc), null, null, null);
                XmlSecurityToken[] tokens = result.getXmlSecurityTokens();
                boolean found = false;
                for (int k = 0; k < tokens.length; k++) {
                    XmlSecurityToken token = tokens[k];
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
    public void testAuthenticationAssertionSenderVouches() throws Exception {
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setClientAddress(InetAddress.getLocalHost());
        samlOptions.setAttestingEntity(holderOfKeySigner);
        SamlAssertionGenerator samlGenerator = new SamlAssertionGenerator(authorityKeys.asSignerInfo());
        final String name = holderOfKeySigner.getCertificateChain()[0].getSubjectDN().getName();
          final LoginCredentials credentials = LoginCredentials.makePasswordCredentials(name, new char[] {}, HttpBasic.class);

        SubjectStatement subjectStatement = SubjectStatement.createAuthenticationStatement(credentials, SubjectStatement.SENDER_VOUCHES, KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null);

        for (int i = 0; i < serviceDescriptors.length; i++) {
            ServiceDescriptor serviceDescriptor = serviceDescriptors[i];
            Wsdl wsdl = Wsdl.newInstance(null, new StringReader(serviceDescriptor.wsdlXml));
            wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
            SoapMessageGenerator sm = new SoapMessageGenerator();
            SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
            for (int j = 0; j < requests.length; j++) {
                SoapMessageGenerator.Message request = requests[j];
                SOAPMessage msg = request.getSOAPMessage();
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                msg.writeTo(bo);
                Document doc = XmlUtil.parse(new ByteArrayInputStream(bo.toByteArray()));
                samlGenerator.attachStatement(doc, subjectStatement, samlOptions);
                WssProcessor wssProcessor = new WssProcessorImpl();
                ProcessorResult result = wssProcessor.undecorateMessage(new Message(doc), null, null, null);
                XmlSecurityToken[] tokens = result.getXmlSecurityTokens();
                boolean found = false;
                for (int k = 0; k < tokens.length; k++) {
                    XmlSecurityToken token = tokens[k];
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
    public void testAuthorizationAssertionSenderVouches() throws Exception {
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setClientAddress(InetAddress.getLocalHost());
        samlOptions.setAttestingEntity(holderOfKeySigner);
        SamlAssertionGenerator samlGenerator = new SamlAssertionGenerator(authorityKeys.asSignerInfo());
        final String name = holderOfKeySigner.getCertificateChain()[0].getSubjectDN().getName();
        final LoginCredentials credentials = LoginCredentials.makePasswordCredentials(name, new char[] {}, HttpBasic.class);
        SubjectStatement subjectStatement = SubjectStatement.createAuthorizationStatement(
                credentials,
                SubjectStatement.SENDER_VOUCHES,
                KeyInfoInclusionType.CERT,
                "http://wheel", null, null, NameIdentifierInclusionType.FROM_CREDS, null, null, null);

        for (int i = 0; i < serviceDescriptors.length; i++) {
            ServiceDescriptor serviceDescriptor = serviceDescriptors[i];
            Wsdl wsdl = Wsdl.newInstance(null, new StringReader(serviceDescriptor.wsdlXml));
            wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
            SoapMessageGenerator sm = new SoapMessageGenerator();
            SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
            for (int j = 0; j < requests.length; j++) {
                SoapMessageGenerator.Message request = requests[j];
                SOAPMessage msg = request.getSOAPMessage();
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                msg.writeTo(bo);
                Document doc = XmlUtil.parse(new ByteArrayInputStream(bo.toByteArray()));
                samlGenerator.attachStatement(doc, subjectStatement, samlOptions);
                WssProcessor wssProcessor = new WssProcessorImpl();
                ProcessorResult result = wssProcessor.undecorateMessage(new Message(doc), null, null, null);
                XmlSecurityToken[] tokens = result.getXmlSecurityTokens();
                boolean found = false;
                for (int k = 0; k < tokens.length; k++) {
                    XmlSecurityToken token = tokens[k];
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
    public void testAuthorizationAssertionHolderOfKey() throws Exception {
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setClientAddress(InetAddress.getLocalHost());
        samlOptions.setAttestingEntity(holderOfKeySigner);
        SamlAssertionGenerator samlGenerator = new SamlAssertionGenerator(authorityKeys.asSignerInfo());
        final LoginCredentials credentials = LoginCredentials.makeCertificateCredentials(holderOfKeySigner.getCertificateChain()[0], RequestWssX509Cert.class);
        SubjectStatement subjectStatement = SubjectStatement.createAuthorizationStatement(credentials,
          SubjectStatement.HOLDER_OF_KEY, KeyInfoInclusionType.CERT, "http://wheel", null, null, NameIdentifierInclusionType.FROM_CREDS, null, null, null);

        for (int i = 0; i < serviceDescriptors.length; i++) {
            ServiceDescriptor serviceDescriptor = serviceDescriptors[i];
            Wsdl wsdl = Wsdl.newInstance(null, new StringReader(serviceDescriptor.wsdlXml));
            wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
            SoapMessageGenerator sm = new SoapMessageGenerator();
            SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
            for (int j = 0; j < requests.length; j++) {
                SoapMessageGenerator.Message request = requests[j];
                SOAPMessage msg = request.getSOAPMessage();
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                msg.writeTo(bo);
                Document doc = XmlUtil.parse(new ByteArrayInputStream(bo.toByteArray()));
                samlGenerator.attachStatement(doc, subjectStatement, samlOptions);
                WssProcessor wssProcessor = new WssProcessorImpl();
                ProcessorResult result = wssProcessor.undecorateMessage(new Message(doc), null, null, null);
                XmlSecurityToken[] tokens = result.getXmlSecurityTokens();
                boolean found = false;
                for (int k = 0; k < tokens.length; k++) {
                    XmlSecurityToken token = tokens[k];
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
        for (int i = 0; i < headers.length; i++) {
            EntityHeader header = headers[i];
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

        for (int i = 0; i < serviceDescriptors.length; i++) {
            ServiceDescriptor descriptor = serviceDescriptors[i];
            PublishedService ps = new PublishedService();
            ps.setName(descriptor.name);
            ps.setWsdlXml(descriptor.wsdlXml);
            ps.getPolicy().setXml(descriptor.policyXml);
            serviceAdmin.savePublishedService(ps);
        }
    }

    private static String getAttributeAssertionPolicy() {
        SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        RequestWssSaml a = new RequestWssSaml();
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
        RequestWssSaml a = new RequestWssSaml();
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
        RequestWssSaml a = new RequestWssSaml();
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

    public static void testSenderVouchesWithThumbprint() throws Exception {
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
        namei.setFormat(SamlConstants.NAMEIDENTIFIER_X509_SUBJECT);
        namei.setStringValue("CN=mike");

        SubjectConfirmationType subjconf = subj.addNewSubjectConfirmation();
        subjconf.addConfirmationMethod("urn:oasis:names:tc:SAML:1.0:cm:sender-vouches");

        assertTrue(subjconf.validate());
        assertTrue(namei.validate());
        assertTrue(subj.validate());

        assertTrue(ast.validate());

        XmlOptions xo = new XmlOptions();
        Map namespaces = new HashMap();
        namespaces.put(SamlConstants.NS_SAML, SamlConstants.NS_SAML_PREFIX);
        xo.setSaveSuggestedPrefixes(namespaces);
        AssertionDocument ad = AssertionDocument.Factory.newInstance(xo);
        ad.setAssertion(at);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ad.save(bos, xo);

        Document assDoc = XmlUtil.stringToDocument(bos.toString());

        final X509Certificate signingCert = TestDocuments.getDotNetServerCertificate();
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

    public void testStuff() throws Exception {
        SubjectStatement stmt = SubjectStatement.createAttributeStatement(new LoginCredentials("foo", "bar".toCharArray(), CredentialFormat.BASIC, HttpBasic.class), SubjectStatement.BEARER, "foo", "urn:example.com:attributes", "bar", KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null);
        SamlAssertionGenerator.Options opts = new SamlAssertionGenerator.Options();
        SamlAssertionGenerator sag = new SamlAssertionGenerator(holderOfKeySigner);
        Document assertionDoc = sag.createAssertion(stmt, opts);

        SamlAssertion ass = new SamlAssertionV1(assertionDoc.getDocumentElement(), null);
        ass.verifyEmbeddedIssuerSignature();
    }

    public static void testAttributeStatementWithThumbprint() throws Exception {
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
        Map namespaces = new HashMap();
        namespaces.put(SamlConstants.NS_SAML, SamlConstants.NS_SAML_PREFIX);
        xo.setSaveSuggestedPrefixes(namespaces);
        AssertionDocument ad = AssertionDocument.Factory.newInstance(xo);
        ad.setAssertion(at);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ad.save(bos, xo);

        Document assDoc = XmlUtil.stringToDocument(bos.toString());

        X509Certificate signingCert = TestDocuments.getDotNetServerCertificate();
        PrivateKey singingKey = TestDocuments.getDotNetServerPrivateKey();
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
        str.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns:wsse", wsseNs);
        Element keyId = XmlUtil.createAndAppendElementNS(str, "KeyIdentifier", wsseNs, "wsse");
        keyId.setAttribute("EncodingType", SoapUtil.ENCODINGTYPE_BASE64BINARY);
        keyId.setAttribute("ValueType", SoapUtil.VALUETYPE_SKI);
        XmlUtil.setTextContent(keyId, CertUtils.getSki(senderSigningCert));

        return DsigUtil.createEnvelopedSignature(elementToSign, senderSigningCert, senderSigningKey, str, null);
    }

    /**
     * Test <code>SamlProcessingTest</code> main.
     */
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
