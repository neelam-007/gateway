package com.l7tech.server.saml;

import com.l7tech.common.security.Keys;
import com.l7tech.common.security.saml.SamlAssertionGenerator;
import com.l7tech.common.security.saml.SubjectStatement;
import com.l7tech.common.security.token.SamlSecurityToken;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.WssProcessor;
import com.l7tech.common.security.xml.processor.WssProcessorImpl;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.SoapMessageGenerator;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.MockServletApi;
import com.l7tech.server.SoapMessageProcessingServlet;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ResolutionParameterTooLongException;
import com.l7tech.service.ServiceAdmin;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * Class SamlProcessingTest.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SamlProcessingTest extends TestCase {
    private static MockServletApi servletApi;
    private static ServiceDescriptor[] serviceDescriptors;
    private SoapMessageProcessingServlet messageProcessingServlet;
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
        servletApi.reset();
        messageProcessingServlet = new SoapMessageProcessingServlet();
        messageProcessingServlet.init(servletApi.getServletConfig());

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
          SubjectStatement.HOLDER_OF_KEY);
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
                ProcessorResult result = wssProcessor.undecorateMessage(doc, null, null, null);
                SecurityToken[] tokens = result.getSecurityTokens();
                boolean found = false;
                for (int k = 0; k < tokens.length; k++) {
                    SecurityToken token = tokens[k];
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

        SubjectStatement subjectStatement = SubjectStatement.createAuthenticationStatement(credentials, SubjectStatement.SENDER_VOUCHES);

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
                ProcessorResult result = wssProcessor.undecorateMessage(doc, null, null, null);
                SecurityToken[] tokens = result.getSecurityTokens();
                boolean found = false;
                for (int k = 0; k < tokens.length; k++) {
                    SecurityToken token = tokens[k];
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
        SubjectStatement subjectStatement = SubjectStatement.createAuthorizationStatement(credentials,
          SubjectStatement.SENDER_VOUCHES, "http://wheel", null, null);

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
                ProcessorResult result = wssProcessor.undecorateMessage(doc, null, null, null);
                SecurityToken[] tokens = result.getSecurityTokens();
                boolean found = false;
                for (int k = 0; k < tokens.length; k++) {
                    SecurityToken token = tokens[k];
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
          SubjectStatement.HOLDER_OF_KEY, "http://wheel", null, null);

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
                ProcessorResult result = wssProcessor.undecorateMessage(doc, null, null, null);
                SecurityToken[] tokens = result.getSecurityTokens();
                boolean found = false;
                for (int k = 0; k < tokens.length; k++) {
                    SecurityToken token = tokens[k];
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
      SaveException, VersionException, ResolutionParameterTooLongException, SAXException {

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
            ps.setPolicyXml(descriptor.policyXml);
            serviceAdmin.savePublishedService(ps);
        }
    }

    private static String getAttributeAssertionPolicy() {
        Assertion policy = new AllAssertion(Arrays.asList(new Assertion[]{
            // Saml Attribute statement:
            new SamlAttributeStatement(),
            // Route:
            new HttpRoutingAssertion()
        }));

        return WspWriter.getPolicyXml(policy);
    }

    private static String getAuthorizationAssertionPolicy() {
        Assertion policy = new AllAssertion(Arrays.asList(new Assertion[]{
            // Saml Authorization Statement:
            new SamlAuthorizationStatement(),
            // Route:
            new HttpRoutingAssertion()
        }));

        return WspWriter.getPolicyXml(policy);
    }

    private static String getAuthenticationAssertionPolicy() {
        Assertion policy = new AllAssertion(Arrays.asList(new Assertion[]{
            // Saml Authentication Statement:
            new SamlAuthenticationStatement(),
            // Route:
            //new HttpRoutingAssertion()
        }));

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

    /**
     * Test <code>SamlProcessingTest</code> main.
     */
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
