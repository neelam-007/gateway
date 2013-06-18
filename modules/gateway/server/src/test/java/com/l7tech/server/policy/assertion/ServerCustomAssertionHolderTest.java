package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.TestCustomMessageTargetable;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.ext.*;
import com.l7tech.policy.assertion.ext.message.*;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetableSupport;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.composite.ServerAllAssertion;
import com.l7tech.server.util.Injector;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;

import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import static org.mockito.Mockito.*;
import static junit.framework.Assert.*;

/**
 * Test ServerCustomAssertionHolder
 *
 * @author tveninov
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerCustomAssertionHolderTest
{
    static private final String SAMPLE_XML_INPUT_MESSAGE = "<?xml version=\"1.0\" encoding=\"utf-8\"?><a><b>1</b><c>2</c></a>";
    static private final String SAMPLE_XML_OUT_MESSAGE = "<test>output message</test>";
    static private final String INPUT_VAR = "inputVariable";
    static private final String OUTPUT_VAR = "outputVariable";
    static private final String HARDCODED_VARIABLE = "result is:\n\n${" + OUTPUT_VAR + ".mainpart}";
    static private final String HARDCODED_OUTPUT_VALUE = "result is:\n\n" + SAMPLE_XML_OUT_MESSAGE;

    static private final String XML_SOURCE_VARIABLE = "xmlSource";
    static private final String XML_SOURCE = "<?xml version=\"1.0\" encoding=\"utf-8\"?><xmlSource>input</xmlSource>";
    static private final String XML_SOURCE_SET = "<xmlSource>output</xmlSource>";
    static private final String JSON_SOURCE_VARIABLE = "jsonSource";
    static private final String JSON_SOURCE = "{\n" +
            "\"input\": [\n" +
            "{ \"firstName\":\"John\" , \"lastName\":\"Doe\" }, \n" +
            "{ \"firstName\":\"Anna\" , \"lastName\":\"Smith\" }, \n" +
            "{ \"firstName\":\"Peter\" , \"lastName\":\"Jones\" }\n" +
            "]\n" +
            "}";
    static private final String JSON_SOURCE_SET = "{\n" +
            "\"output\": [\n" +
            "{ \"firstName\":\"Gregg\" , \"lastName\":\"Kline\" }, \n" +
            "{ \"firstName\":\"Letitia\" , \"lastName\":\"Commons\" }, \n" +
            "{ \"firstName\":\"Gary\" , \"lastName\":\"Tatum\" }\n" +
            "{ \"firstName\":\"Elenore\" , \"lastName\":\"Commons\" }, \n" +
            "]\n" +
            "}";
    static private final String SOAP_SOURCE_VARIABLE = "soapSource";
    static private final String SOAP_SOURCE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
            "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "   <soap:Body>\n" +
            "       <mns:SoapSourceInput " +
            "xmlns:mns=\"http://warehouse.acme.com/ws\" " +
            "soap:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "           <bstrParam1 xsi:type=\"xsd:string\">param1</bstrParam1>\n" +
            "           <bstrParam2 xsi:type=\"xsd:string\">param2</bstrParam2>\n" +
            "       </mns:SoapSourceInput>\n" +
            "   </soap:Body>\n" +
            "</soap:Envelope>";
    static private final String SOAP_SOURCE_SET = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
            "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "   <soap:Body>\n" +
            "       <mns:SoapSourceOutput " +
            "xmlns:mns=\"http://warehouse.acme.com/ws\" " +
            "soap:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "           <param1 xsi:type=\"xsd:string\">param1</param1>\n" +
            "       </mns:SoapSourceOutput>\n" +
            "   </soap:Body>\n" +
            "</soap:Envelope>";
    static private final String TEXT_SOURCE_VARIABLE = "textSource";
    static private final String TEXT_SOURCE = "text source test";
    static private final String TEXT_SOURCE_SET = "set test source text";
    static private final String BINARY_SOURCE_VARIABLE = "binSource";
    static private final byte[] BINARY_SOURCE = "binary source test".getBytes();
    static private final byte[] BINARY_SOURCE_SET = "set binary source test".getBytes();
    static private final String INPUT_STREAM_SOURCE_VARIABLE = "iStreamSource";
    static private final byte[] INPUT_STREAM_SOURCE_BYTES = "input stream source test".getBytes();
    static private final byte[] INPUT_STREAM_SOURCE_BYTES_SET = "set input stream source test".getBytes();

    static private final String CRLF = "\r\n";

    // SOAP/XML
    static private final String MULTIPART_SOURCE_SOAP_PART_CONTENT_ID = "-76394136.13454";
    static private final String MULTIPART_SOURCE_SOAP_PART_HEADER = "Content-Transfer-Encoding: 8bit" + CRLF +
            "Content-Type: text/xml; charset=utf-8" + CRLF +
            "Content-ID: " + MULTIPART_SOURCE_SOAP_PART_CONTENT_ID + CRLF;
    static private final String MULTIPART_SOURCE_SOAP_PART = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<env:Envelope xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "    xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "  <env:Body>\n" +
            "    <n1:echoOne xmlns:n1=\"urn:EchoAttachmentsService\"\n" +
            "        env:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "      <file href=\"cid:-76392836.13454\"></file>\n" +
            "    </n1:echoOne>\n" +
            "  </env:Body>\n" +
            "</env:Envelope>\n";

    // app/octet
    static private final String MULTIPART_SOURCE_APP_OCTET_PART_CONTENT_ID = "-76392836.13454";
    static private final String MULTIPART_SOURCE_APP_OCTET_PART_HEADER = "Content-Transfer-Encoding: 8bit" + CRLF +
            "Content-Type: application/octet-stream" + CRLF +
            "Content-ID: " + MULTIPART_SOURCE_APP_OCTET_PART_CONTENT_ID + CRLF;
    static private final String MULTIPART_SOURCE_APP_OCTET_PART = "require 'soap/rpc/driver'\n" +
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
            "\n";

    // json
    static private final String MULTIPART_SOURCE_JSON_PART_CONTENT_ID = "-76392001.13454";
    static private final String MULTIPART_SOURCE_JSON_PART_HEADER = "Content-Transfer-Encoding: 8bit" + CRLF +
            "Content-Type: application/json; charset=utf-8" + CRLF +
            "Content-ID: " + MULTIPART_SOURCE_JSON_PART_CONTENT_ID + CRLF;
    static private final String MULTIPART_SOURCE_JSON_PART = JSON_SOURCE + "\n";

    static private final String MULTIPART_SOURCE_BOUNDARY = "----=Part_-763936460.00306951464153826";

    // multipart: first part soap
    static private final String MULTIPART_SOURCE_FIRST_PART_SOAP_VARIABLE = "mpartSOAPSource";
    static private final String MULTIPART_SOURCE_FIRST_PART_SOAP_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
            MULTIPART_SOURCE_BOUNDARY + "\"; start=\"" + MULTIPART_SOURCE_SOAP_PART_CONTENT_ID + "\"";
    static private final String MULTIPART_SOURCE_FIRST_PART_SOAP = "--" + MULTIPART_SOURCE_BOUNDARY + CRLF +
            MULTIPART_SOURCE_SOAP_PART_HEADER + CRLF +
            MULTIPART_SOURCE_SOAP_PART + CRLF +
            "--" + MULTIPART_SOURCE_BOUNDARY + CRLF +
            MULTIPART_SOURCE_APP_OCTET_PART_HEADER + CRLF +
            MULTIPART_SOURCE_APP_OCTET_PART + CRLF +
            "--" + MULTIPART_SOURCE_BOUNDARY + CRLF +
            MULTIPART_SOURCE_JSON_PART_HEADER + CRLF +
            MULTIPART_SOURCE_JSON_PART + CRLF +
            "--" + MULTIPART_SOURCE_BOUNDARY + "--" + CRLF;
    static private final String MULTIPART_SOURCE_FIRST_PART_EMPTY_SOAP = "--" + MULTIPART_SOURCE_BOUNDARY + CRLF +
            MULTIPART_SOURCE_SOAP_PART_HEADER + CRLF +
            CRLF +
            "--" + MULTIPART_SOURCE_BOUNDARY + CRLF +
            MULTIPART_SOURCE_APP_OCTET_PART_HEADER + CRLF +
            MULTIPART_SOURCE_APP_OCTET_PART + CRLF +
            "--" + MULTIPART_SOURCE_BOUNDARY + CRLF +
            MULTIPART_SOURCE_JSON_PART_HEADER + CRLF +
            MULTIPART_SOURCE_JSON_PART + CRLF +
            "--" + MULTIPART_SOURCE_BOUNDARY + "--" + CRLF;

    // multipart: first part json
    static private final String MULTIPART_SOURCE_FIRST_PART_JSON_VARIABLE = "mpartJsonSource";
    static private final String MULTIPART_SOURCE_FIRST_PART_JSON_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
            MULTIPART_SOURCE_BOUNDARY + "\"; start=\"" + MULTIPART_SOURCE_JSON_PART_CONTENT_ID + "\"";
    static private final String MULTIPART_SOURCE_FIRST_PART_JSON = "--" + MULTIPART_SOURCE_BOUNDARY + CRLF +
            MULTIPART_SOURCE_JSON_PART_HEADER + CRLF +
            MULTIPART_SOURCE_JSON_PART + CRLF +
            "--" + MULTIPART_SOURCE_BOUNDARY + CRLF +
            MULTIPART_SOURCE_APP_OCTET_PART_HEADER + CRLF +
            MULTIPART_SOURCE_APP_OCTET_PART + CRLF +
            "--" + MULTIPART_SOURCE_BOUNDARY + CRLF +
            MULTIPART_SOURCE_SOAP_PART_HEADER + CRLF +
            MULTIPART_SOURCE_SOAP_PART + CRLF +
            "--" + MULTIPART_SOURCE_BOUNDARY + "--" + CRLF;
    static private final String MULTIPART_SOURCE_FIRST_PART_EMPTY_JSON = "--" + MULTIPART_SOURCE_BOUNDARY + CRLF +
            MULTIPART_SOURCE_JSON_PART_HEADER + CRLF +
            CRLF +
            "--" + MULTIPART_SOURCE_BOUNDARY + CRLF +
            MULTIPART_SOURCE_APP_OCTET_PART_HEADER + CRLF +
            MULTIPART_SOURCE_APP_OCTET_PART + CRLF +
            "--" + MULTIPART_SOURCE_BOUNDARY + CRLF +
            MULTIPART_SOURCE_SOAP_PART_HEADER + CRLF +
            MULTIPART_SOURCE_SOAP_PART + CRLF +
            "--" + MULTIPART_SOURCE_BOUNDARY + "--" + CRLF;

    /**
     * Our ServiceInvocation implementation for testing
     * added getCustomAssertion method so that we can have access to the customAssertion.
     */
    private class TestServiceInvocation extends ServiceInvocation {
        public CustomAssertion getCustomAssertion() {
            return (this.customAssertion);
        }
    }

    /**
     * Test Legacy CustomAssertion
     */
    public static class TestLegacyCustomAssertion implements CustomAssertion {
        private static final long serialVersionUID = 7349491450019520261L;
        @Override
        public String getName() {
            return "My Legacy CustomAssertion";
        }
    }

    @Mock(name = "applicationContext")
    private ApplicationContext mockApplicationContext;

    @InjectMocks
    private final ServerPolicyFactory serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new Injector() {
        @Override
        public void inject( final Object target ) {
            // Since we already have mocked ServiceInvocation object (serviceInvocation)
            // we need to inject that instance into newly created ServerCustomAssertionHolder.
            // for our test purposes
            //
            // !!!WARNING!!!
            // In the future ServerCustomAssertionHolder.serviceInvocation field should not be renamed
            // otherwise this unit test will fail
            if (target instanceof ServerCustomAssertionHolder) {
                try {
                    Field field = target.getClass().getDeclaredField("serviceInvocation");
                    field.setAccessible(true);
                    field.set(target, serviceInvocation);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    fail("Failed to inject ServerCustomAssertionHolder#serviceInvocation field.");
                }
            }
        }
    });

    @Spy
    private final TestServiceInvocation serviceInvocation = new TestServiceInvocation();

    @Mock
    private CustomAssertionsRegistrar mockRegistrar;

    @Mock
    private TrustedCertManager mockTrustedCertManager;

    private final AssertionRegistry assertionRegistry = new AssertionRegistry();

    @Before
    public void setUp() throws Exception
    {
        // mock getBean to return appropriate mock classes for CustomAssertionRegistrar and TrustedCertManager
        // which are used by ServerCustomAssertionHolder
        when(mockApplicationContext.getBean("customAssertionRegistrar")).thenReturn(mockRegistrar);
        when(mockApplicationContext.getBean("trustedCertManager")).thenReturn(mockTrustedCertManager);

        // mock getBean to return appropriate mock classes for policyFactory
        when(mockApplicationContext.getBean("policyFactory", ServerPolicyFactory.class)).thenReturn(serverPolicyFactory);
        when(mockApplicationContext.getBean("policyFactory")).thenReturn(serverPolicyFactory);

        // mock getBean to return appropriate mock classes for assertionRegistry
        assertionRegistry.afterPropertiesSet();
        when(mockApplicationContext.getBean("assertionRegistry", AssertionRegistry.class)).thenReturn(assertionRegistry);
        when(mockApplicationContext.getBean("assertionRegistry")).thenReturn(assertionRegistry);

        // mock getBean to return appropriate stashManagerFactory used for HardcodedResponseAssertion
        final StashManagerFactory stashManagerFactory = new StashManagerFactory() {
            @Override
            public StashManager createStashManager() {
                return new ByteArrayStashManager();
            }
        };
        when(mockApplicationContext.getBean("stashManagerFactory")).thenReturn(stashManagerFactory);
        when(mockApplicationContext.getBean("stashManagerFactory", StashManagerFactory.class)).thenReturn(stashManagerFactory);

        // add sample MessageTargetable descriptor
        final CustomAssertionDescriptor descriptorTargetable = new CustomAssertionDescriptor(
                "Test.TestCustomMessageTargetable",
                TestCustomMessageTargetable.class,
                null, // for now do not use UI class
                TestServiceInvocation.class,
                Category.AUDIT_ALERT,
                "Test Message Targetable CustomAssertion Description",
                null, // for now do not use allowed packages
                null, // for now do not use allowed resources
                null // nodeNames
        );

        // add sample Legacy descriptor
        final CustomAssertionDescriptor descriptorLegacy = new CustomAssertionDescriptor(
                "Test.TestLegacyCustomAssertion",
                TestLegacyCustomAssertion.class,
                null, // for now do not use UI class
                TestServiceInvocation.class,
                Category.AUDIT_ALERT,
                "Test CustomAssertion Description",
                null, // for now do not use allowed packages
                null, // for now do not use allowed resources
                null // nodeNames
        );

        // mock the new descriptors
        when(mockRegistrar.getDescriptor(TestCustomMessageTargetable.class)).then(new Returns(descriptorTargetable));
        when(mockRegistrar.getDescriptor(TestLegacyCustomAssertion.class)).then(new Returns(descriptorLegacy));

        // Register needed assertions here
        assertionRegistry.registerAssertion(SetVariableAssertion.class);
        assertionRegistry.registerAssertion(HardcodedResponseAssertion.class);
        assertionRegistry.registerAssertion(CustomAssertionHolder.class);
    }

    private PolicyEnforcementContext makeContext(final Message request, final Message response) {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private Assertion makePolicy(final List<Assertion> assertions) {
        final AllAssertion allAssertion = new AllAssertion();
        for (Assertion assertion: assertions) {
            allAssertion.addChild(assertion);
        }
        return allAssertion;
    }

    /**
     * Basic "smoke-test" for checkRequest
     */
    @Test
    public void testBasicCheckRequest() throws Exception {
        // CustomAssertion
        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.AUDIT_ALERT);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestCustomMessageTargetable(
                new CustomMessageTargetableSupport(CustomMessageTargetableSupport.TARGET_REQUEST), // source target
                new CustomMessageTargetableSupport(CustomMessageTargetableSupport.TARGET_RESPONSE) // destination target
        ));

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(customAssertionHolder, false);
        assertTrue("Is of ServerCustomAssertionHolder", serverAssertion instanceof ServerCustomAssertionHolder);
        final ServerCustomAssertionHolder holder = (ServerCustomAssertionHolder)serverAssertion;

        final PolicyEnforcementContext context = makeContext(new Message(), new Message());

        //doReturn(CustomAssertionStatus.NONE).when(serviceInvocation).checkRequest(Matchers.<ServiceRequest>any(), Matchers.<ServiceResponse>any());
        when(serviceInvocation.checkRequest(Matchers.<ServiceRequest>any(), Matchers.<ServiceResponse>any())).thenReturn(CustomAssertionStatus.NONE);
        AssertionStatus assertionStatus = holder.checkRequest(context);
        assertEquals(AssertionStatus.NONE, assertionStatus);

        when(serviceInvocation.checkRequest(Matchers.<ServiceRequest>any(), Matchers.<ServiceResponse>any())).thenReturn(CustomAssertionStatus.AUTH_FAILED);
        assertionStatus = holder.checkRequest(context);
        assertEquals(AssertionStatus.AUTH_FAILED, assertionStatus);
    }

    @Test
    public void testTargetableUsingPolicy() throws Exception {
        // SetVariableAssertion: Variable: INPUT_VAR Value: SAMPLE_XML_INPUT_MESSAGE
        final SetVariableAssertion setVariableAssertion = new SetVariableAssertion();
        setVariableAssertion.setVariableToSet(INPUT_VAR);
        setVariableAssertion.setDataType(DataType.MESSAGE);
        setVariableAssertion.setContentType("text/xml; charset=utf-8");
        setVariableAssertion.setExpression(SAMPLE_XML_INPUT_MESSAGE);

        // CustomAssertion
        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.AUDIT_ALERT);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestCustomMessageTargetable(
                new CustomMessageTargetableSupport(INPUT_VAR), // source target
                new CustomMessageTargetableSupport(OUTPUT_VAR) // destination target
        ));

        // HardcodedResponseAssertion: #OUTPUT_VAR
        final HardcodedResponseAssertion hardcodedResponseAssertion = new HardcodedResponseAssertion();
        hardcodedResponseAssertion.setResponseContentType("text/plain; charset=UTF-8");
        hardcodedResponseAssertion.setResponseStatus(HardcodedResponseAssertion.DEFAULT_STATUS);
        hardcodedResponseAssertion.setBase64ResponseBody(HexUtils.encodeBase64(HARDCODED_VARIABLE.getBytes()));

        final Assertion ass = makePolicy(Arrays.<Assertion>asList(setVariableAssertion, customAssertionHolder, hardcodedResponseAssertion));
        assertTrue("Is AllAssertion", ass instanceof AllAssertion);
        final AllAssertion allAssertion = (AllAssertion)ass;

        assertNotNull("CustomAssertionHolder cannot be null.", customAssertionHolder);
        assertNotNull("CustomAssertion cannot be null.", customAssertionHolder.getCustomAssertion());
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        assertEquals("Source Target is OTHER", customAssertionHolder.getTargetName(), "${" + INPUT_VAR + "}");

        final CustomAssertion ca = serviceInvocation.getCustomAssertion();
        assertTrue("CustomAssertion is of type TestCustomMessageTargetable", ca instanceof TestCustomMessageTargetable);
        final TestCustomMessageTargetable customAssertion = (TestCustomMessageTargetable)ca;

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                fail("onRequest should not be called when checkRequest is not returning null!");
                return null;
            }
        }).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                fail("onResponse should not be called when checkRequest is not returning null!");
                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        doAnswer(new Answer<CustomAssertionStatus>() {
            @Override
            public CustomAssertionStatus answer(final InvocationOnMock invocation) throws Throwable {
                assertTrue("there is only one parameter for checkRequest", invocation.getArguments().length == 2);

                Object param1 = invocation.getArguments()[0];
                assertTrue("Param1 is ServiceRequest", param1 instanceof ServiceRequest);
                ServiceRequest request = (ServiceRequest) param1;

                Object param2 = invocation.getArguments()[1];
                assertNull("Param2 is null", param2);

                final Document inputDoc = XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE);
                inputDoc.normalizeDocument();
                Document doc = (Document) request.getMessageData(customAssertion.getSourceTarget(), CustomMessageFormat.XML).getData();
                assertNotNull("Document not NULL", doc);
                doc.normalizeDocument();
                assertTrue("Source Target Message Document is same as TargetMessage document", inputDoc.isEqualNode(doc));

                assertEquals("Destination Target is OTHER", customAssertion.getDestinationTarget().getTargetName(), "${" + OUTPUT_VAR + "}");
                request.setBytes(customAssertion.getDestinationTarget(), request.createContentType(CustomContentHeader.Type.XML), SAMPLE_XML_OUT_MESSAGE.getBytes());

                return CustomAssertionStatus.NONE;
            }
        }).when(serviceInvocation).checkRequest(Matchers.<ServiceRequest>any(), Matchers.<ServiceResponse>any());

        final PolicyEnforcementContext context = makeContext(new Message(), new Message());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(allAssertion, false);
        assertTrue("Is of ServerAllAssertion", serverAssertion instanceof ServerAllAssertion);
        final ServerAllAssertion serverAllAssertion = (ServerAllAssertion)serverAssertion;

        AssertionStatus status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);

        final Message xmlMessageOut = context.getTargetMessage(new MessageTargetableSupport(OUTPUT_VAR));
        final Document outDocDestinationMessage = xmlMessageOut.getXmlKnob().getDocumentReadOnly();
        outDocDestinationMessage.normalizeDocument();
        final Document outputDoc = XmlUtil.stringToDocument(SAMPLE_XML_OUT_MESSAGE);
        outputDoc.normalizeDocument();
        assertTrue("Destination Target Message (destinationTarget) is properly set from assertion", outputDoc.isEqualNode(outDocDestinationMessage));

        final Message responseMsg = context.getResponse();
        final InputStream responseStream = responseMsg.getMimeKnob().getEntireMessageBodyAsInputStream(true);
        final String inputText = new String(com.l7tech.util.IOUtils.slurpStream(responseStream));
        assertEquals("Destination Target Message (RESPONSE) is properly set from assertion", inputText, HARDCODED_OUTPUT_VALUE);
    }

    @Test
    public void testTargetableBeforeRoutePolicy() throws Exception {

        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.AUDIT_ALERT);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestCustomMessageTargetable(
                new CustomMessageTargetableSupport("request"), // source target
                new CustomMessageTargetableSupport(OUTPUT_VAR) // destination target
        ));

        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseContentType("text/plain; charset=UTF-8");
        responseAssertion.setResponseStatus(HardcodedResponseAssertion.DEFAULT_STATUS);
        responseAssertion.setBase64ResponseBody(HexUtils.encodeBase64(HARDCODED_VARIABLE.getBytes()));

        final Assertion ass = makePolicy(Arrays.<Assertion>asList(customAssertionHolder, responseAssertion));
        assertTrue("Is AllAssertion", ass instanceof AllAssertion);
        final AllAssertion allAssertion = (AllAssertion)ass;

        assertNotNull("CustomAssertionHolder cannot be null.", customAssertionHolder);
        assertNotNull("CustomAssertion cannot be null.", customAssertionHolder.getCustomAssertion());
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        assertEquals("Source Target is Request", customAssertionHolder.getTargetName(), "Request");

        final CustomAssertion ca = serviceInvocation.getCustomAssertion();
        assertTrue("CustomAssertion is of type TestCustomMessageTargetable", ca instanceof TestCustomMessageTargetable);
        final TestCustomMessageTargetable customAssertion = (TestCustomMessageTargetable)ca;

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                fail("onRequest should not be called when checkRequest is not returning null!");
                return null;
            }
        }).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                fail("onResponse should not be called when checkRequest is not returning null!");
                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        doAnswer(new Answer<CustomAssertionStatus>() {
            @Override
            public CustomAssertionStatus answer(final InvocationOnMock invocation) throws Throwable
            {
                assertTrue("there is only one parameter for checkRequest", invocation.getArguments().length == 2);

                Object param1 = invocation.getArguments()[0];
                assertTrue("Param1 is ServiceRequest", param1 instanceof ServiceRequest);
                ServiceRequest request = (ServiceRequest)param1;

                Object param2 = invocation.getArguments()[1];
                assertNull("Param2 is null", param2);

                final Document inputDoc = XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE);
                inputDoc.normalizeDocument();
                Document doc = (Document)request.getMessageData(CustomMessageFormat.XML).getData(); // should be request by default
                assertNotNull("Document not NULL", doc);
                doc.normalizeDocument();
                assertTrue("Source Target Message Document is same as TargetMessage document", inputDoc.isEqualNode(doc));

                assertEquals("Destination Target is OTHER", customAssertion.getDestinationTarget().getTargetName(), "${" + OUTPUT_VAR + "}");
                request.setBytes(customAssertion.getDestinationTarget(), request.createContentType(CustomContentHeader.Type.XML), SAMPLE_XML_OUT_MESSAGE.getBytes());

                return CustomAssertionStatus.NONE;
            }
        }).when(serviceInvocation).checkRequest(Matchers.<ServiceRequest>any(), Matchers.<ServiceResponse>any());

        final PolicyEnforcementContext context = makeContext(new Message(XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE)), new Message());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(allAssertion, false);
        assertTrue("Is of ServerAllAssertion", serverAssertion instanceof ServerAllAssertion);
        final ServerAllAssertion serverAllAssertion = (ServerAllAssertion)serverAssertion;

        AssertionStatus status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);

        final Message xmlMessageOut = context.getTargetMessage(new MessageTargetableSupport(OUTPUT_VAR));
        final Document outDocDestinationMessage = xmlMessageOut.getXmlKnob().getDocumentReadOnly();
        outDocDestinationMessage.normalizeDocument();
        final Document outputDoc = XmlUtil.stringToDocument(SAMPLE_XML_OUT_MESSAGE);
        outputDoc.normalizeDocument();
        assertTrue("Destination Target Message (destinationTarget) is properly set from assertion", outputDoc.isEqualNode(outDocDestinationMessage));

        final Message responseMsg = context.getResponse();
        final InputStream responseStream = responseMsg.getMimeKnob().getEntireMessageBodyAsInputStream(true);
        final String inputText = new String(com.l7tech.util.IOUtils.slurpStream(responseStream));
        assertEquals("Destination Target Message (RESPONSE) is properly set from assertion", inputText, HARDCODED_OUTPUT_VALUE);
    }

    @Test
    public void testTargetableAfterRoutePolicy() throws Exception {

        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseContentType("text/xml; charset=UTF-8");
        responseAssertion.setResponseStatus(HardcodedResponseAssertion.DEFAULT_STATUS);
        responseAssertion.setBase64ResponseBody(HexUtils.encodeBase64(SAMPLE_XML_INPUT_MESSAGE.getBytes()));

        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.AUDIT_ALERT);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestCustomMessageTargetable(
                new CustomMessageTargetableSupport("response"), // source target
                new CustomMessageTargetableSupport(OUTPUT_VAR)   // destination target
        ));

        final Assertion ass = makePolicy(Arrays.<Assertion>asList(responseAssertion, customAssertionHolder));
        assertTrue("Is AllAssertion", ass instanceof AllAssertion);
        final AllAssertion allAssertion = (AllAssertion)ass;

        assertNotNull("CustomAssertionHolder cannot be null.", customAssertionHolder);
        assertNotNull("CustomAssertion cannot be null.", customAssertionHolder.getCustomAssertion());
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        assertEquals("Source Target is Response", customAssertionHolder.getTargetName(), "Response");

        final CustomAssertion ca = serviceInvocation.getCustomAssertion();
        assertTrue("CustomAssertion is of type TestCustomMessageTargetable", ca instanceof TestCustomMessageTargetable);
        final TestCustomMessageTargetable customAssertion = (TestCustomMessageTargetable)ca;

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                fail("onRequest should not be called when checkRequest is not returning null!");
                return null;
            }
        }).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                fail("onResponse should not be called when checkRequest is not returning null!");
                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        doAnswer(new Answer<CustomAssertionStatus>() {
            @Override
            public CustomAssertionStatus answer(final InvocationOnMock invocation) throws Throwable
            {
                assertTrue("there is only one parameter for checkRequest", invocation.getArguments().length == 2);

                Object param1 = invocation.getArguments()[0];
                assertTrue("Param1 is ServiceRequest", param1 instanceof ServiceRequest);
                ServiceRequest request = (ServiceRequest)param1;

                Object param2 = invocation.getArguments()[1];
                assertTrue("Param2 is ServiceResponse", param2 instanceof ServiceResponse);
                ServiceResponse response = (ServiceResponse)param2;

                final Document requestInputDoc = XmlUtil.stringToDocument(XML_SOURCE);
                requestInputDoc.normalizeDocument();
                Document reqDoc = (Document)request.getMessageData(CustomMessageFormat.XML).getData(); // should be request by default
                assertNotNull("Request Document not NULL", reqDoc);
                reqDoc.normalizeDocument();
                assertTrue("Input request document is matching request target", requestInputDoc.isEqualNode(reqDoc));

                final Document responseInputDoc = XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE);
                responseInputDoc.normalizeDocument();
                Document doc = (Document)response.getMessageData(CustomMessageFormat.XML).getData(); // should be response by default
                assertNotNull("Response Document not NULL", doc);
                doc.normalizeDocument();
                assertTrue("Response document, after routing, is matching response target", responseInputDoc.isEqualNode(doc));

                assertEquals("Destination Target is OTHER", customAssertion.getDestinationTarget().getTargetName(), "${" + OUTPUT_VAR + "}");
                response.setBytes(customAssertion.getDestinationTarget(), request.parseContentTypeValue("text/xml; charset=utf-8"), SAMPLE_XML_OUT_MESSAGE.getBytes());

                return CustomAssertionStatus.NONE;
            }
        }).when(serviceInvocation).checkRequest(Matchers.<ServiceRequest>any(), Matchers.<ServiceResponse>any());

        final PolicyEnforcementContext context = makeContext(new Message(XmlUtil.stringToDocument(XML_SOURCE)), new Message());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(allAssertion, false);
        assertTrue("Is of ServerAllAssertion", serverAssertion instanceof ServerAllAssertion);
        final ServerAllAssertion serverAllAssertion = (ServerAllAssertion)serverAssertion;

        AssertionStatus status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);

        final Message xmlMessageOut = context.getTargetMessage(new MessageTargetableSupport(OUTPUT_VAR));
        final Document outDocDestinationMessage = xmlMessageOut.getXmlKnob().getDocumentReadOnly();
        outDocDestinationMessage.normalizeDocument();
        final Document outputDoc = XmlUtil.stringToDocument(SAMPLE_XML_OUT_MESSAGE);
        outputDoc.normalizeDocument();
        assertTrue("Destination Target Message (destinationTarget) is properly set from assertion", outputDoc.isEqualNode(outDocDestinationMessage));
    }

    @Test
    public void testLegacyBeforeRoute() throws Exception {

        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.AUDIT_ALERT);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestLegacyCustomAssertion());

        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseContentType("text/xml; charset=UTF-8");
        responseAssertion.setResponseStatus(HardcodedResponseAssertion.DEFAULT_STATUS);
        responseAssertion.setBase64ResponseBody(HexUtils.encodeBase64(SAMPLE_XML_OUT_MESSAGE.getBytes()));

        final Assertion ass = makePolicy(Arrays.<Assertion>asList(customAssertionHolder, responseAssertion));
        assertTrue("Is AllAssertion", ass instanceof AllAssertion);
        final AllAssertion allAssertion = (AllAssertion)ass;

        assertNotNull("CustomAssertionHolder cannot be null.", customAssertionHolder);
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        final CustomAssertion customAssertion = serviceInvocation.getCustomAssertion();
        assertFalse("CustomAssertion is of type CustomMessageTargetable", customAssertion instanceof CustomMessageTargetable);

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                assertTrue("there is only one parameter for onRequest", invocation.getArguments().length == 1);

                Object param1 = invocation.getArguments()[0];
                assertTrue("Param is ServiceRequest", param1 instanceof ServiceRequest);
                ServiceRequest request = (ServiceRequest)param1;

                final Document inputDoc = XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE);
                inputDoc.normalizeDocument();
                //noinspection deprecation
                final Document reqDoc = request.getDocument();
                assertNotNull("Document not NULL", reqDoc);
                reqDoc.normalizeDocument();
                assertTrue("Source Target Message Document is same as TargetMessage document", inputDoc.isEqualNode(reqDoc));

                return null;
            }
        }).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                fail("onResponse should not be called when policy is before routing assertion!");
                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(allAssertion, false);
        assertTrue("Is instance of ServerAllAssertion", serverAssertion instanceof ServerAllAssertion);
        final ServerAllAssertion serverAllAssertion = (ServerAllAssertion)serverAssertion;

        final PolicyEnforcementContext context = makeContext(new Message(XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE)), new Message());
        AssertionStatus status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);

        // just to be persistent make sure response is properly set
        final Document outDoc = XmlUtil.stringToDocument(SAMPLE_XML_OUT_MESSAGE);
        outDoc.normalizeDocument();
        final Document resDoc = context.getResponse().getXmlKnob().getDocumentReadOnly();
        assertNotNull("Response is not NULL", resDoc);
        resDoc.normalizeDocument();
        assertTrue("Source Target Message Document is same as TargetMessage document", outDoc.isEqualNode(resDoc));
    }

    @Test
    public void testLegacyAfterRoute() throws Exception {

        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseContentType("text/xml; charset=UTF-8");
        responseAssertion.setResponseStatus(HardcodedResponseAssertion.DEFAULT_STATUS);
        responseAssertion.setBase64ResponseBody(HexUtils.encodeBase64(SAMPLE_XML_INPUT_MESSAGE.getBytes()));

        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.AUDIT_ALERT);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestLegacyCustomAssertion());

        final Assertion ass = makePolicy(Arrays.<Assertion>asList(responseAssertion, customAssertionHolder));
        assertTrue("Is AllAssertion", ass instanceof AllAssertion);
        final AllAssertion allAssertion = (AllAssertion)ass;

        assertNotNull("CustomAssertionHolder cannot be null.", customAssertionHolder);
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        final CustomAssertion customAssertion = serviceInvocation.getCustomAssertion();
        assertFalse("CustomAssertion is of type CustomMessageTargetable", customAssertion instanceof CustomMessageTargetable);

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                fail("onRequest should not be called when policy is after routing assertion!");
                return null;
            }
        }).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                assertTrue("there is only one parameter for onResponse", invocation.getArguments().length == 1);

                Object param1 = invocation.getArguments()[0];
                assertTrue("Param is ServiceResponse", param1 instanceof ServiceResponse);
                ServiceResponse response = (ServiceResponse)param1;

                final Document inputDoc = XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE);
                inputDoc.normalizeDocument();
                //noinspection deprecation
                final Document resDoc = response.getDocument();
                assertNotNull("Document not NULL", resDoc);
                resDoc.normalizeDocument();
                assertTrue("Source Target Message Document is same as TargetMessage document", inputDoc.isEqualNode(resDoc));

                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(allAssertion, false);
        assertTrue("Is instance of ServerAllAssertion", serverAssertion instanceof ServerAllAssertion);
        final ServerAllAssertion serverAllAssertion = (ServerAllAssertion)serverAssertion;

        final PolicyEnforcementContext context = makeContext(new Message(), new Message());
        AssertionStatus status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);
    }

    private enum TestMessageType {
        XML,
        JSON,
        SOAP,
        TEXT,
        BINARY,
        INPUT_STREAM,
        MULTIPART_FIRST_PART_SOAP,
        MULTIPART_FIRST_PART_JSON,
    }

    private void doTestRequestResponseExtractMessage(final DataExtractor extractor,
                                                     final TestMessageType messageType,
                                                     final ContentTypeHeader expectedContentHeader,
                                                     final Object dataBody
    ) throws Exception {
        CustomMessageData data;
        if (messageType == TestMessageType.XML || messageType == TestMessageType.SOAP || messageType == TestMessageType.MULTIPART_FIRST_PART_SOAP) {
            assertTrue(extractor.isMessageDataOfType(CustomMessageFormat.XML));
            assertFalse(extractor.isMessageDataOfType(CustomMessageFormat.JSON));

            assertNotNull(data = extractor.getMessageData(CustomMessageFormat.XML));
            if (messageType == TestMessageType.MULTIPART_FIRST_PART_SOAP) {
                assertTrue(((Document) data.getData()).isEqualNode(XmlUtil.stringToDocument(MULTIPART_SOURCE_SOAP_PART)));
                assertEquals(data.getContentType().getFullValue(), ContentTypeHeader.XML_DEFAULT.getFullValue());
            } else {
                assertTrue(((Document) data.getData()).isEqualNode(XmlUtil.stringToDocument((String) dataBody)));
                assertEquals(data.getContentType().getFullValue(), expectedContentHeader.getFullValue());
            }

            assertNull(extractor.getMessageData(CustomMessageFormat.JSON));

        } else if (messageType == TestMessageType.JSON || messageType == TestMessageType.MULTIPART_FIRST_PART_JSON) {
            assertFalse(extractor.isMessageDataOfType(CustomMessageFormat.XML));
            assertTrue(extractor.isMessageDataOfType(CustomMessageFormat.JSON));

            assertNull(extractor.getMessageData(CustomMessageFormat.XML));

            assertNotNull(data = extractor.getMessageData(CustomMessageFormat.JSON));
            if (messageType == TestMessageType.MULTIPART_FIRST_PART_JSON) {
                assertEquals(((CustomJsonData) data.getData()).getJsonData(), MULTIPART_SOURCE_JSON_PART);
                assertEquals(data.getContentType().getFullValue(), ContentTypeHeader.APPLICATION_JSON.getFullValue());
            } else {
                assertEquals(((CustomJsonData) data.getData()).getJsonData(), (String) dataBody);
                assertEquals(data.getContentType().getFullValue(), expectedContentHeader.getFullValue());
            }
        } else if (messageType == TestMessageType.TEXT || messageType == TestMessageType.BINARY || messageType == TestMessageType.INPUT_STREAM) {
            assertFalse(extractor.isMessageDataOfType(CustomMessageFormat.XML));
            assertFalse(extractor.isMessageDataOfType(CustomMessageFormat.JSON));
        }

        assertTrue(extractor.isMessageDataOfType(CustomMessageFormat.BYTES));
        assertTrue(extractor.isMessageDataOfType(CustomMessageFormat.INPUT_STREAM));

        assertNotNull(data = extractor.getMessageData(CustomMessageFormat.BYTES));
        assertEquals(data.getContentType().getFullValue(), expectedContentHeader.getFullValue());
        if (messageType == TestMessageType.MULTIPART_FIRST_PART_SOAP || messageType == TestMessageType.MULTIPART_FIRST_PART_JSON) {
            assertEquals((new String((byte[])data.getData())).trim(), ((String) dataBody).trim());
        } else if (messageType == TestMessageType.BINARY || messageType == TestMessageType.INPUT_STREAM) {
            assertTrue(Arrays.equals((byte[])data.getData(), (byte[]) dataBody));
        } else {
            assertTrue(Arrays.equals((byte[])data.getData(), ((String) dataBody).getBytes()));
        }

        assertNotNull(data = extractor.getMessageData(CustomMessageFormat.INPUT_STREAM));
        assertEquals(data.getContentType().getFullValue(), expectedContentHeader.getFullValue());
        if (messageType == TestMessageType.MULTIPART_FIRST_PART_SOAP || messageType == TestMessageType.MULTIPART_FIRST_PART_JSON) {
            assertEquals((new String(IOUtils.slurpStream((InputStream)data.getData()))).trim(), ((String) dataBody).trim());
        } else if (messageType == TestMessageType.BINARY || messageType == TestMessageType.INPUT_STREAM) {
            assertTrue(Arrays.equals(IOUtils.slurpStream((InputStream)data.getData()), (byte[]) dataBody));
        } else {
            assertTrue(Arrays.equals(IOUtils.slurpStream((InputStream)data.getData()), ((String) dataBody).getBytes()));
        }
    }

    private void doTestExtractMessage(final ServerCustomAssertionHolder holder,
                                      final TestMessageType requestMessageType,
                                      final Message requestMessage,
                                      final ContentTypeHeader requestExpectedContentHeader,
                                      final Object requestData,
                                      final TestMessageType responseMessageType,
                                      final Message responseMessage,
                                      final ContentTypeHeader responseExpectedContentHeader,
                                      final Object responseData
    ) throws Exception {

        doAnswer(new Answer<CustomAssertionStatus>() {
            @Override
            public CustomAssertionStatus answer(final InvocationOnMock invocation) throws Throwable {
                assertTrue("both parameters are present for checkRequest", invocation.getArguments().length == 2);

                Object param1 = invocation.getArguments()[0];
                assertTrue("Param1 is never null and is ServiceRequest", param1 instanceof ServiceRequest);
                final ServiceRequest request = (ServiceRequest)param1;

                if (requestData == null) {
                    assertFalse(request.isMessageDataOfType(CustomMessageFormat.XML));
                    assertFalse(request.isMessageDataOfType(CustomMessageFormat.JSON));
                    assertFalse(request.isMessageDataOfType(CustomMessageFormat.BYTES));
                    assertFalse(request.isMessageDataOfType(CustomMessageFormat.INPUT_STREAM));

                    assertNull(request.getMessageData(CustomMessageFormat.XML));
                    assertNull(request.getMessageData(CustomMessageFormat.JSON));
                    assertNull(request.getMessageData(CustomMessageFormat.BYTES));
                    assertNull(request.getMessageData(CustomMessageFormat.INPUT_STREAM));
                } else {
                    doTestRequestResponseExtractMessage(request, requestMessageType, requestExpectedContentHeader, requestData);
                }

                Object param2 = invocation.getArguments()[1];
                if (responseData == null) {
                    assertNull("Param2 is null", param2);
                } else {
                    assertTrue("Param2 is never null and is ServiceResponse", param2 instanceof ServiceResponse);
                    final ServiceResponse response = (ServiceResponse)param2;
                    doTestRequestResponseExtractMessage(response, responseMessageType, responseExpectedContentHeader, responseData);
                }

                return CustomAssertionStatus.FAILED;
            }
        }).when(serviceInvocation).checkRequest(Matchers.<ServiceRequest>any(), Matchers.<ServiceResponse>any());

        PolicyEnforcementContext context = makeContext(requestMessage, responseMessage);
        AssertionStatus assertionStatus = holder.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void testExtractMessage() throws Exception {
        // CustomAssertion
        final CustomAssertionHolder assertion = new CustomAssertionHolder();
        assertion.setCategory(Category.AUDIT_ALERT);
        assertion.setDescriptionText("Test Custom Assertion");
        assertion.setCustomAssertion(new TestLegacyCustomAssertion());

        assertNotNull("CustomAssertionHolder cannot be null.", assertion);
        assertNotNull("CustomAssertion cannot be null.", assertion.getCustomAssertion());
        serviceInvocation.setCustomAssertion(assertion.getCustomAssertion());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(assertion, false);
        assertTrue("Is of ServerCustomAssertionHolder", serverAssertion instanceof ServerCustomAssertionHolder);
        final ServerCustomAssertionHolder holder = (ServerCustomAssertionHolder)serverAssertion;

        final CustomAssertion ca = serviceInvocation.getCustomAssertion();
        assertTrue("CustomAssertion is of type TestLegacyCustomAssertion", ca instanceof TestLegacyCustomAssertion);
        final TestLegacyCustomAssertion customAssertion = (TestLegacyCustomAssertion)ca;
        assertNotNull(customAssertion);

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                fail("onRequest should not be called when checkRequest is not returning null!");
                return null;
            }
        }).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                fail("onResponse should not be called when checkRequest is not returning null!");
                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        // no request and no response
        doTestExtractMessage(holder,
                TestMessageType.XML, new Message(), null, null,
                TestMessageType.XML, new Message(), null, null);

        // request, no response
        doTestExtractMessage(holder,
                TestMessageType.XML, new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(XML_SOURCE.getBytes())), ContentTypeHeader.XML_DEFAULT, XML_SOURCE,
                TestMessageType.XML, new Message(), null, null);
        doTestExtractMessage(holder,
                TestMessageType.JSON, new Message(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(JSON_SOURCE.getBytes())), ContentTypeHeader.APPLICATION_JSON, JSON_SOURCE,
                TestMessageType.XML, new Message(), null, null);
        doTestExtractMessage(holder,
                TestMessageType.SOAP, new Message(new ByteArrayStashManager(), ContentTypeHeader.SOAP_1_2_DEFAULT, new ByteArrayInputStream(SOAP_SOURCE.getBytes())), ContentTypeHeader.SOAP_1_2_DEFAULT, SOAP_SOURCE,
                TestMessageType.XML, new Message(), null, null);
        doTestExtractMessage(holder,
                TestMessageType.TEXT, new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream(TEXT_SOURCE.getBytes())), ContentTypeHeader.TEXT_DEFAULT, TEXT_SOURCE,
                TestMessageType.XML, new Message(), null, null);
        doTestExtractMessage(holder,
                TestMessageType.BINARY, new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(BINARY_SOURCE)), ContentTypeHeader.OCTET_STREAM_DEFAULT, BINARY_SOURCE,
                TestMessageType.XML, new Message(), null, null);
        doTestExtractMessage(holder,
                TestMessageType.INPUT_STREAM, new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(INPUT_STREAM_SOURCE_BYTES)), ContentTypeHeader.OCTET_STREAM_DEFAULT, INPUT_STREAM_SOURCE_BYTES,
                TestMessageType.XML, new Message(), null, null);
        doTestExtractMessage(holder,
                TestMessageType.MULTIPART_FIRST_PART_SOAP, new Message(new ByteArrayStashManager(), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_SOAP_CONTENT_TYPE), new ByteArrayInputStream(MULTIPART_SOURCE_FIRST_PART_SOAP.getBytes())), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_SOAP_CONTENT_TYPE), MULTIPART_SOURCE_FIRST_PART_SOAP,
                TestMessageType.XML, new Message(), null, null);
        doTestExtractMessage(holder,
                TestMessageType.MULTIPART_FIRST_PART_JSON, new Message(new ByteArrayStashManager(), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_JSON_CONTENT_TYPE), new ByteArrayInputStream(MULTIPART_SOURCE_FIRST_PART_JSON.getBytes())), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_JSON_CONTENT_TYPE), MULTIPART_SOURCE_FIRST_PART_JSON,
                TestMessageType.XML, new Message(), null, null);

        // no request, response
        doTestExtractMessage(holder,
                TestMessageType.XML, new Message(), null, null,
                TestMessageType.XML, new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(XML_SOURCE.getBytes())), ContentTypeHeader.XML_DEFAULT, XML_SOURCE);
        doTestExtractMessage(holder,
                TestMessageType.XML, new Message(), null, null,
                TestMessageType.JSON, new Message(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(JSON_SOURCE.getBytes())), ContentTypeHeader.APPLICATION_JSON, JSON_SOURCE);
        doTestExtractMessage(holder,
                TestMessageType.XML, new Message(), null, null,
                TestMessageType.SOAP, new Message(new ByteArrayStashManager(), ContentTypeHeader.SOAP_1_2_DEFAULT, new ByteArrayInputStream(SOAP_SOURCE.getBytes())), ContentTypeHeader.SOAP_1_2_DEFAULT, SOAP_SOURCE);
        doTestExtractMessage(holder,
                TestMessageType.XML, new Message(), null, null,
                TestMessageType.TEXT, new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream(TEXT_SOURCE.getBytes())), ContentTypeHeader.TEXT_DEFAULT, TEXT_SOURCE);
        doTestExtractMessage(holder,
                TestMessageType.XML, new Message(), null, null,
                TestMessageType.BINARY, new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(BINARY_SOURCE)), ContentTypeHeader.OCTET_STREAM_DEFAULT, BINARY_SOURCE);
        doTestExtractMessage(holder,
                TestMessageType.XML, new Message(), null, null,
                TestMessageType.INPUT_STREAM, new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(INPUT_STREAM_SOURCE_BYTES)), ContentTypeHeader.OCTET_STREAM_DEFAULT, INPUT_STREAM_SOURCE_BYTES);
        doTestExtractMessage(holder,
                TestMessageType.XML, new Message(), null, null,
                TestMessageType.MULTIPART_FIRST_PART_SOAP, new Message(new ByteArrayStashManager(), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_SOAP_CONTENT_TYPE), new ByteArrayInputStream(MULTIPART_SOURCE_FIRST_PART_SOAP.getBytes())), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_SOAP_CONTENT_TYPE), MULTIPART_SOURCE_FIRST_PART_SOAP);
        doTestExtractMessage(holder,
                TestMessageType.XML, new Message(), null, null,
                TestMessageType.MULTIPART_FIRST_PART_JSON, new Message(new ByteArrayStashManager(), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_JSON_CONTENT_TYPE), new ByteArrayInputStream(MULTIPART_SOURCE_FIRST_PART_JSON.getBytes())), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_JSON_CONTENT_TYPE), MULTIPART_SOURCE_FIRST_PART_JSON);

        // request and response
        doTestExtractMessage(holder,
                TestMessageType.XML, new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(XML_SOURCE.getBytes())), ContentTypeHeader.XML_DEFAULT, XML_SOURCE,
                TestMessageType.JSON, new Message(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(JSON_SOURCE.getBytes())), ContentTypeHeader.APPLICATION_JSON, JSON_SOURCE);
        doTestExtractMessage(holder,
                TestMessageType.JSON, new Message(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(JSON_SOURCE.getBytes())), ContentTypeHeader.APPLICATION_JSON, JSON_SOURCE,
                TestMessageType.MULTIPART_FIRST_PART_SOAP, new Message(new ByteArrayStashManager(), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_SOAP_CONTENT_TYPE), new ByteArrayInputStream(MULTIPART_SOURCE_FIRST_PART_SOAP.getBytes())), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_SOAP_CONTENT_TYPE), MULTIPART_SOURCE_FIRST_PART_SOAP);
        doTestExtractMessage(holder,
                TestMessageType.SOAP, new Message(new ByteArrayStashManager(), ContentTypeHeader.SOAP_1_2_DEFAULT, new ByteArrayInputStream(SOAP_SOURCE.getBytes())), ContentTypeHeader.SOAP_1_2_DEFAULT, SOAP_SOURCE,
                TestMessageType.INPUT_STREAM, new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(INPUT_STREAM_SOURCE_BYTES)), ContentTypeHeader.OCTET_STREAM_DEFAULT, INPUT_STREAM_SOURCE_BYTES);
        doTestExtractMessage(holder,
                TestMessageType.TEXT, new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream(TEXT_SOURCE.getBytes())), ContentTypeHeader.TEXT_DEFAULT, TEXT_SOURCE,
                TestMessageType.BINARY, new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(BINARY_SOURCE)), ContentTypeHeader.OCTET_STREAM_DEFAULT, BINARY_SOURCE);
        doTestExtractMessage(holder,
                TestMessageType.BINARY, new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(BINARY_SOURCE)), ContentTypeHeader.OCTET_STREAM_DEFAULT, BINARY_SOURCE,
                TestMessageType.XML, new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(XML_SOURCE.getBytes())), ContentTypeHeader.XML_DEFAULT, XML_SOURCE);
        doTestExtractMessage(holder,
                TestMessageType.INPUT_STREAM, new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(INPUT_STREAM_SOURCE_BYTES)), ContentTypeHeader.OCTET_STREAM_DEFAULT, INPUT_STREAM_SOURCE_BYTES,
                TestMessageType.MULTIPART_FIRST_PART_JSON, new Message(new ByteArrayStashManager(), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_JSON_CONTENT_TYPE), new ByteArrayInputStream(MULTIPART_SOURCE_FIRST_PART_JSON.getBytes())), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_JSON_CONTENT_TYPE), MULTIPART_SOURCE_FIRST_PART_JSON);
        doTestExtractMessage(holder,
                TestMessageType.MULTIPART_FIRST_PART_SOAP, new Message(new ByteArrayStashManager(), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_SOAP_CONTENT_TYPE), new ByteArrayInputStream(MULTIPART_SOURCE_FIRST_PART_SOAP.getBytes())), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_SOAP_CONTENT_TYPE), MULTIPART_SOURCE_FIRST_PART_SOAP,
                TestMessageType.TEXT, new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream(TEXT_SOURCE.getBytes())), ContentTypeHeader.TEXT_DEFAULT, TEXT_SOURCE);
        doTestExtractMessage(holder,
                TestMessageType.MULTIPART_FIRST_PART_JSON, new Message(new ByteArrayStashManager(), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_JSON_CONTENT_TYPE), new ByteArrayInputStream(MULTIPART_SOURCE_FIRST_PART_JSON.getBytes())), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_JSON_CONTENT_TYPE), MULTIPART_SOURCE_FIRST_PART_JSON,
                TestMessageType.SOAP, new Message(new ByteArrayStashManager(), ContentTypeHeader.SOAP_1_2_DEFAULT, new ByteArrayInputStream(SOAP_SOURCE.getBytes())), ContentTypeHeader.SOAP_1_2_DEFAULT, SOAP_SOURCE);
    }

    private void doTestRequestResponseEmptyContent(final DataExtractor extractor,
                                                   final TestMessageType messageType,
                                                   final ContentTypeHeader expectedContentHeader
    ) throws Exception {
        CustomMessageData data;
        if (messageType == TestMessageType.XML || messageType == TestMessageType.SOAP || messageType == TestMessageType.MULTIPART_FIRST_PART_SOAP) {
            assertTrue(extractor.isMessageDataOfType(CustomMessageFormat.XML));
            assertFalse(extractor.isMessageDataOfType(CustomMessageFormat.JSON));

            assertNotNull(data = extractor.getMessageData(CustomMessageFormat.XML));
            if (messageType == TestMessageType.MULTIPART_FIRST_PART_SOAP) {
                assertEquals(data.getContentType().getFullValue(), ContentTypeHeader.XML_DEFAULT.getFullValue());
            } else {
                assertEquals(data.getContentType().getFullValue(), expectedContentHeader.getFullValue());
            }
            assertNull(data.getData());

            assertNull(extractor.getMessageData(CustomMessageFormat.JSON));

        } else if (messageType == TestMessageType.JSON || messageType == TestMessageType.MULTIPART_FIRST_PART_JSON) {
            assertFalse(extractor.isMessageDataOfType(CustomMessageFormat.XML));
            assertTrue(extractor.isMessageDataOfType(CustomMessageFormat.JSON));

            assertNull(extractor.getMessageData(CustomMessageFormat.XML));

            assertNotNull(data = extractor.getMessageData(CustomMessageFormat.JSON));
            if (messageType == TestMessageType.MULTIPART_FIRST_PART_JSON) {
                assertEquals(data.getContentType().getFullValue(), ContentTypeHeader.APPLICATION_JSON.getFullValue());
            } else {
                assertEquals(data.getContentType().getFullValue(), expectedContentHeader.getFullValue());
            }
            assertNotNull(data.getData());
            final String jsonDataString = ((CustomJsonData)data.getData()).getJsonData();
            assertTrue(jsonDataString.isEmpty() ||
                    jsonDataString.equals("bad json") ||
                    jsonDataString.trim().equals(MULTIPART_SOURCE_FIRST_PART_EMPTY_JSON.trim())
            );
        } else if (messageType == TestMessageType.TEXT || messageType == TestMessageType.BINARY || messageType == TestMessageType.INPUT_STREAM) {
            assertFalse(extractor.isMessageDataOfType(CustomMessageFormat.XML));
            assertFalse(extractor.isMessageDataOfType(CustomMessageFormat.JSON));
        }

        assertTrue(extractor.isMessageDataOfType(CustomMessageFormat.BYTES));
        assertTrue(extractor.isMessageDataOfType(CustomMessageFormat.INPUT_STREAM));

        assertNotNull(data = extractor.getMessageData(CustomMessageFormat.BYTES));
        assertEquals(data.getContentType().getFullValue(), expectedContentHeader.getFullValue());
        final byte[] bytes1 = (byte[])data.getData();
        assertTrue(bytes1.length == 0 ||
                new String(bytes1).equals("bad xml") ||
                new String(bytes1).equals("bad json") ||
                new String(bytes1).trim().equals(MULTIPART_SOURCE_FIRST_PART_EMPTY_SOAP.trim()) ||
                new String(bytes1).trim().equals(MULTIPART_SOURCE_FIRST_PART_EMPTY_JSON.trim())
        );

        assertNotNull(data = extractor.getMessageData(CustomMessageFormat.INPUT_STREAM));
        assertEquals(data.getContentType().getFullValue(), expectedContentHeader.getFullValue());
        final byte[] bytes2 = IOUtils.slurpStream(((InputStream) data.getData()));
        assertTrue(bytes2.length == 0 ||
                new String(bytes2).equals("bad xml") ||
                new String(bytes2).equals("bad json") ||
                new String(bytes2).trim().equals(MULTIPART_SOURCE_FIRST_PART_EMPTY_SOAP.trim()) ||
                new String(bytes2).trim().equals(MULTIPART_SOURCE_FIRST_PART_EMPTY_JSON.trim())
        );
    }

    private void doTestEmptyContent(final ServerCustomAssertionHolder holder,
                                    final TestMessageType requestMessageType,
                                    final Message requestMessage,
                                    final ContentTypeHeader requestExpectedContentHeader,
                                    final TestMessageType responseMessageType,
                                    final Message responseMessage,
                                    final ContentTypeHeader responseExpectedContentHeader
    ) throws Exception {

        doAnswer(new Answer<CustomAssertionStatus>() {
            @Override
            public CustomAssertionStatus answer(final InvocationOnMock invocation) throws Throwable {
                assertTrue("both parameters are present for checkRequest", invocation.getArguments().length == 2);

                Object param1 = invocation.getArguments()[0];
                assertTrue("Param1 is never null and is ServiceRequest", param1 instanceof ServiceRequest);
                final ServiceRequest request = (ServiceRequest)param1;

                doTestRequestResponseEmptyContent(request, requestMessageType, requestExpectedContentHeader);

                Object param2 = invocation.getArguments()[1];
                assertTrue("Param2 is never null and is ServiceResponse", param2 instanceof ServiceResponse);
                final ServiceResponse response = (ServiceResponse)param2;
                doTestRequestResponseEmptyContent(response, responseMessageType, responseExpectedContentHeader);

                return CustomAssertionStatus.FAILED;
            }
        }).when(serviceInvocation).checkRequest(Matchers.<ServiceRequest>any(), Matchers.<ServiceResponse>any());

        PolicyEnforcementContext context = makeContext(requestMessage, responseMessage);
        AssertionStatus assertionStatus = holder.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void testEmptyContent() throws Exception {
        // CustomAssertion
        final CustomAssertionHolder assertion = new CustomAssertionHolder();
        assertion.setCategory(Category.AUDIT_ALERT);
        assertion.setDescriptionText("Test Custom Assertion");
        assertion.setCustomAssertion(new TestLegacyCustomAssertion());

        assertNotNull("CustomAssertionHolder cannot be null.", assertion);
        assertNotNull("CustomAssertion cannot be null.", assertion.getCustomAssertion());
        serviceInvocation.setCustomAssertion(assertion.getCustomAssertion());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(assertion, false);
        assertTrue("Is of ServerCustomAssertionHolder", serverAssertion instanceof ServerCustomAssertionHolder);
        final ServerCustomAssertionHolder holder = (ServerCustomAssertionHolder)serverAssertion;

        final CustomAssertion ca = serviceInvocation.getCustomAssertion();
        assertTrue("CustomAssertion is of type TestLegacyCustomAssertion", ca instanceof TestLegacyCustomAssertion);
        final TestLegacyCustomAssertion customAssertion = (TestLegacyCustomAssertion)ca;
        assertNotNull(customAssertion);

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                fail("onRequest should not be called when checkRequest is not returning null!");
                return null;
            }
        }).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                fail("onResponse should not be called when checkRequest is not returning null!");
                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        doTestEmptyContent(holder,
                TestMessageType.XML, new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(new byte[0])), ContentTypeHeader.XML_DEFAULT,
                TestMessageType.JSON, new Message(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream("bad json".getBytes())), ContentTypeHeader.APPLICATION_JSON);
        doTestEmptyContent(holder,
                TestMessageType.JSON, new Message(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(new byte[0])), ContentTypeHeader.APPLICATION_JSON,
                TestMessageType.MULTIPART_FIRST_PART_SOAP, new Message(new ByteArrayStashManager(), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_SOAP_CONTENT_TYPE), new ByteArrayInputStream(MULTIPART_SOURCE_FIRST_PART_EMPTY_SOAP.getBytes())), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_SOAP_CONTENT_TYPE));
        doTestEmptyContent(holder,
                TestMessageType.SOAP, new Message(new ByteArrayStashManager(), ContentTypeHeader.SOAP_1_2_DEFAULT, new ByteArrayInputStream(new byte[0])), ContentTypeHeader.SOAP_1_2_DEFAULT,
                TestMessageType.INPUT_STREAM, new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(new byte[0])), ContentTypeHeader.OCTET_STREAM_DEFAULT);
        doTestEmptyContent(holder,
                TestMessageType.TEXT, new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream(new byte[0])), ContentTypeHeader.TEXT_DEFAULT,
                TestMessageType.BINARY, new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(new byte[0])), ContentTypeHeader.OCTET_STREAM_DEFAULT);
        doTestEmptyContent(holder,
                TestMessageType.BINARY, new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(new byte[0])), ContentTypeHeader.OCTET_STREAM_DEFAULT,
                TestMessageType.XML, new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream("bad xml".getBytes())), ContentTypeHeader.XML_DEFAULT);
        doTestEmptyContent(holder,
                TestMessageType.INPUT_STREAM, new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(new byte[0])), ContentTypeHeader.OCTET_STREAM_DEFAULT,
                TestMessageType.MULTIPART_FIRST_PART_JSON, new Message(new ByteArrayStashManager(), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_JSON_CONTENT_TYPE), new ByteArrayInputStream(MULTIPART_SOURCE_FIRST_PART_EMPTY_JSON.getBytes())), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_JSON_CONTENT_TYPE));
        doTestEmptyContent(holder,
                TestMessageType.MULTIPART_FIRST_PART_SOAP, new Message(new ByteArrayStashManager(), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_SOAP_CONTENT_TYPE), new ByteArrayInputStream(MULTIPART_SOURCE_FIRST_PART_EMPTY_SOAP.getBytes())), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_SOAP_CONTENT_TYPE),
                TestMessageType.TEXT, new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream(new byte[0])), ContentTypeHeader.TEXT_DEFAULT);
        doTestEmptyContent(holder,
                TestMessageType.MULTIPART_FIRST_PART_JSON, new Message(new ByteArrayStashManager(), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_JSON_CONTENT_TYPE), new ByteArrayInputStream(MULTIPART_SOURCE_FIRST_PART_EMPTY_JSON.getBytes())), ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_JSON_CONTENT_TYPE),
                TestMessageType.SOAP, new Message(new ByteArrayStashManager(), ContentTypeHeader.SOAP_1_2_DEFAULT, new ByteArrayInputStream(new byte[0])), ContentTypeHeader.SOAP_1_2_DEFAULT);
    }

    private void doTestContentType(@NotNull final ServiceRequest request,
                                   @Nullable final CustomMessageTargetableSupport targetable,
                                   @NotNull TestMessageType expectedMessageFormat,
                                   @NotNull final Object expectedData,
                                   @NotNull final ContentTypeHeader expectedContentType) throws Exception
    {
        // test for xmlSource
        final CustomMessageData xmlSource = targetable == null ? request.getMessageData(CustomMessageFormat.XML) : request.getMessageData(targetable, CustomMessageFormat.XML);
        // soap payload is as well xml
        if (expectedMessageFormat == TestMessageType.XML || expectedMessageFormat == TestMessageType.SOAP) {
            assertNotNull("xmlSource is not null", xmlSource);
            assertNotNull("xmlSource content-type is not null", xmlSource.getContentType());
            assertNotNull("xmlSource data is not null", xmlSource.getData());
            assertEquals("xmlSource content-type is of expected type", xmlSource.getContentType().getFullValue(), expectedContentType.getFullValue());
            assertTrue("xmlSource data is of Document type", xmlSource.getData() instanceof Document);
            assertTrue("xmlSource Document is properly set", ((Document)xmlSource.getData()).isEqualNode((Document)expectedData));
        } else if (expectedMessageFormat == TestMessageType.MULTIPART_FIRST_PART_SOAP){
            assertNotNull("xmlSource for multipart is not null", xmlSource);
            assertNotNull("xmlSource for multipart content-type is not null", xmlSource.getContentType());
            assertNotNull("xmlSource for multipart data is not null", xmlSource.getData());
            assertEquals("xmlSource for multipart content-type is of expected type", xmlSource.getContentType().getFullValue(), ContentTypeHeader.XML_DEFAULT.getFullValue());
            assertTrue("xmlSource for multipart data is of Document type", xmlSource.getData() instanceof Document);
            assertTrue("xmlSource for multipart Document is properly set", ((Document)xmlSource.getData()).isEqualNode(XmlUtil.stringToDocument(MULTIPART_SOURCE_SOAP_PART)));
        } else {
            assertNull("xmlSource is not expected and should be null", xmlSource);
        }

        // test for jsonSource
        final CustomMessageData jsonSource = targetable == null ? request.getMessageData(CustomMessageFormat.JSON) : request.getMessageData(targetable, CustomMessageFormat.JSON);
        if (expectedMessageFormat == TestMessageType.JSON || expectedMessageFormat == TestMessageType.MULTIPART_FIRST_PART_JSON) {
            assertNotNull("jsonSource is not null", jsonSource);
            assertNotNull("jsonSource content-type is not null", jsonSource.getContentType());
            assertNotNull("jsonSource data is not null", jsonSource.getData());
            if (expectedMessageFormat == TestMessageType.MULTIPART_FIRST_PART_JSON) {
                assertEquals("jsonSource content-type is of expected type", jsonSource.getContentType().getFullValue(), ContentTypeHeader.APPLICATION_JSON.getFullValue());
            } else {
                assertEquals("jsonSource content-type is of expected type", jsonSource.getContentType().getFullValue(), expectedContentType.getFullValue());
            }
            assertTrue("jsonSource data is of CustomJsonData type", jsonSource.getData() instanceof CustomJsonData);

            CustomJsonData jsonData = (CustomJsonData) jsonSource.getData();
            if (expectedMessageFormat == TestMessageType.MULTIPART_FIRST_PART_JSON) {
                assertEquals("jsonSource data string is properly set", jsonData.getJsonData(), MULTIPART_SOURCE_JSON_PART);
            } else {
                assertEquals("jsonSource data string is properly set", jsonData.getJsonData(), (String)expectedData);
            }

            // Go through the jason object maps
            assertTrue("jsonSource root object is of Map type", jsonData.getJsonObject() instanceof Map);   // Map<String, ArrayList<Map<String, String>>>
            Map jsonObjRoot = (Map)jsonData.getJsonObject();

            assertSame("jsonSource root have only one element", jsonObjRoot.size(), 1);
            assertNotNull("jsonSource input element is not null", jsonObjRoot.get("input"));
            assertTrue("jsonSource input element is of List type", jsonObjRoot.get("input") instanceof List);
            List jsonObjInput = (List)jsonObjRoot.get("input");

            assertSame("jsonSource input element have 3 child elements", jsonObjInput.size(), 3);
            assertTrue("jsonSource input element child(0) is of type Map", jsonObjInput.get(0) instanceof Map);
            assertTrue("jsonSource input element child(1) is of type Map", jsonObjInput.get(1) instanceof Map);
            assertTrue("jsonSource input element child(2) is of type Map", jsonObjInput.get(2) instanceof Map);

            Map jsonObjInputChild0 = (Map)jsonObjInput.get(0);
            assertSame(jsonObjInputChild0.size(), 2);
            assertEquals(jsonObjInputChild0.get("firstName"), "John");
            assertEquals(jsonObjInputChild0.get("lastName"), "Doe");

            Map jsonObjInputChild1 = (Map)jsonObjInput.get(1);
            assertSame(jsonObjInputChild1.size(), 2);
            assertEquals(jsonObjInputChild1.get("firstName"), "Anna");
            assertEquals(jsonObjInputChild1.get("lastName"), "Smith");

            Map jsonObjInputChild2 = (Map)jsonObjInput.get(2);
            assertSame(jsonObjInputChild2.size(), 2);
            assertEquals(jsonObjInputChild2.get("firstName"), "Peter");
            assertEquals(jsonObjInputChild2.get("lastName"), "Jones");
        } else {
            assertNull("jsonSource is not expected and should be null", jsonSource);
        }

        // test for binary data. Note, for binary all content-types are supported, so test them all
        final CustomMessageData sourceBytes = targetable == null ? request.getMessageData(CustomMessageFormat.BYTES) : request.getMessageData(targetable, CustomMessageFormat.BYTES);
        assertNotNull("sourceBytes is not null", sourceBytes);
        assertNotNull("sourceBytes content-type is not null", sourceBytes.getContentType());
        assertNotNull("sourceBytes data is not null", sourceBytes.getData());
        assertEquals("sourceBytes content-type is of expected type", sourceBytes.getContentType().getFullValue(), expectedContentType.getFullValue());
        assertTrue("sourceBytes data is of byte[] type", sourceBytes.getData() instanceof byte[]);
        // test for all content-types
        if (expectedMessageFormat == TestMessageType.XML) {
            final Document docFromBytes = XmlUtil.stringToDocument(new String(((byte[])sourceBytes.getData())));
            docFromBytes.normalizeDocument();
            assertTrue("sourceBytes XML Document is properly set", docFromBytes.isEqualNode((Document)expectedData));
        } else if (expectedMessageFormat == TestMessageType.JSON) {
            assertEquals("sourceBytes JSON Data is properly set", new String((byte[])sourceBytes.getData()), (String)expectedData);
        } else if (expectedMessageFormat == TestMessageType.SOAP) {
            assertTrue("sourceBytes SOAP is properly set", ((Document)expectedData).isEqualNode(XmlUtil.stringToDocument(new String((byte[])sourceBytes.getData()))));
        } else if (expectedMessageFormat == TestMessageType.TEXT) {
            assertEquals("sourceBytes Text is properly set", new String((byte[])sourceBytes.getData()), (String)expectedData);
        } else if (expectedMessageFormat == TestMessageType.BINARY) {
            assertTrue("sourceBytes bytes array (bytes[]) is properly set", Arrays.equals((byte[])sourceBytes.getData(), (byte[])expectedData));
        } else if (expectedMessageFormat == TestMessageType.INPUT_STREAM) {
            assertTrue("sourceBytes InputStream is properly set", Arrays.equals((byte[])sourceBytes.getData(), (byte[])expectedData));
        } else if (expectedMessageFormat == TestMessageType.MULTIPART_FIRST_PART_SOAP || expectedMessageFormat == TestMessageType.MULTIPART_FIRST_PART_JSON) {
            // for some reason it adds extra CRLF, so trimming both values
            assertEquals("sourceBytes multipart is properly set", (new String((byte[])sourceBytes.getData())).trim(), ((String)expectedData).trim());
        }

        // test for input stream data. Note, for input stream all content-types are supported, so test them all
        final CustomMessageData requestDataInputStream = targetable == null ? request.getMessageData(CustomMessageFormat.INPUT_STREAM) : request.getMessageData(targetable, CustomMessageFormat.INPUT_STREAM);
        assertNotNull("sourceInputStream is not null", requestDataInputStream);
        assertNotNull("sourceInputStream content-type is not null", requestDataInputStream.getContentType());
        assertNotNull("sourceInputStream data is not null", requestDataInputStream.getData());
        assertEquals("sourceInputStream content-type is of expected type", requestDataInputStream.getContentType().getFullValue(), expectedContentType.getFullValue());
        assertTrue("sourceInputStream data is of InputStream type", requestDataInputStream.getData() instanceof InputStream);
        if (expectedMessageFormat == TestMessageType.XML) {
            final Document docFromInputStream = XmlUtil.stringToDocument(new String(IOUtils.slurpStream((InputStream)requestDataInputStream.getData())));
            docFromInputStream.normalizeDocument();
            assertTrue("sourceInputStream XML Document is properly set", docFromInputStream.isEqualNode((Document)expectedData));
        } else if (expectedMessageFormat == TestMessageType.JSON) {
            assertEquals("sourceInputStream JSON Data is properly set", new String(IOUtils.slurpStream((InputStream)requestDataInputStream.getData())), (String)expectedData);
        } else if (expectedMessageFormat == TestMessageType.SOAP) {
            assertTrue("sourceInputStream SOAP is properly set", ((Document) expectedData).isEqualNode(XmlUtil.stringToDocument(new String((byte[]) sourceBytes.getData()))));
        } else if (expectedMessageFormat == TestMessageType.TEXT) {
            assertEquals("sourceInputStream Text is properly set", new String(IOUtils.slurpStream((InputStream)requestDataInputStream.getData())), (String)expectedData);
        } else if (expectedMessageFormat == TestMessageType.BINARY) {
            assertTrue("sourceInputStream bytes array (bytes[]) is properly set", Arrays.equals(IOUtils.slurpStream((InputStream) requestDataInputStream.getData()), (byte[])expectedData));
        } else if (expectedMessageFormat == TestMessageType.INPUT_STREAM) {
            assertTrue("sourceInputStream InputStream is properly set", Arrays.equals(IOUtils.slurpStream((InputStream) requestDataInputStream.getData()), (byte[])expectedData));
        } else if (expectedMessageFormat == TestMessageType.MULTIPART_FIRST_PART_SOAP || expectedMessageFormat == TestMessageType.MULTIPART_FIRST_PART_JSON) {
            // for some reason it adds extra CRLF, so trimming both values
            assertEquals("sourceBytes multipart is properly set", (new String(IOUtils.slurpStream((InputStream) requestDataInputStream.getData()))).trim(), ((String)expectedData).trim());
        }
    }

    private Assertion makeSetVariable(final String name, final String contentType, final Object value) {
        final SetVariableAssertion variableAssertion = new SetVariableAssertion();
        variableAssertion.setVariableToSet(name);
        variableAssertion.setDataType(DataType.MESSAGE);
        variableAssertion.setContentType(contentType);
        if (value instanceof String) {
            variableAssertion.setExpression((String)value);
        } else if (value instanceof byte[]) {
            variableAssertion.setBase64Expression(HexUtils.encodeBase64((byte[])value));
        }
        return variableAssertion;
    }

    @Test
    public void testAllContentType() throws Exception
    {
        final String ALLCONTENTTYPE_RESULT = "result is:\n\n" +
                "XML:\n${" + XML_SOURCE_VARIABLE + ".mainpart}\n\n\n" +
                "JSON:\n${" + JSON_SOURCE_VARIABLE + ".mainpart}\n\n\n" +
                "SOAP:\n${" + SOAP_SOURCE_VARIABLE + ".mainpart}\n\n\n" +
                "TEXT:\n${" + TEXT_SOURCE_VARIABLE +".mainpart}\n\n\n" +
                "BINARY:\n${" + BINARY_SOURCE_VARIABLE + ".mainpart}\n\n\n" +
                "INPUT STREAM:\n${" + INPUT_STREAM_SOURCE_VARIABLE + ".mainpart}\n\n\n" +
                "end result";
        final String ALLCONTENTTYPE_RESULT_VALUE = "result is:\n\n" +
                "XML:\n" + XmlUtil.nodeToString(XmlUtil.stringToDocument(XML_SOURCE_SET)) + "\n\n\n" +
                "JSON:\n" + JSON_SOURCE_SET + "\n\n\n" +
                "SOAP:\n" + XmlUtil.nodeToString(XmlUtil.stringToDocument(SOAP_SOURCE_SET)) + "\n\n\n" +
                "TEXT:\n" + TEXT_SOURCE_SET +"\n\n\n" +
                "BINARY:\n" + new String(BINARY_SOURCE_SET) + "\n\n\n" +
                "INPUT STREAM:\n" + new String(INPUT_STREAM_SOURCE_BYTES_SET) + "\n\n\n" +
                "end result";

        // test class holding all content-types
        final class TestDifferentContentTypesAssertion implements CustomAssertion, CustomMessageTargetable {
            private static final long serialVersionUID = -9001784333894453876L;
            private final CustomMessageTargetableSupport defaultSource = new CustomMessageTargetableSupport(CustomMessageTargetableSupport.TARGET_REQUEST);
            private final CustomMessageTargetableSupport defaultDestination = new CustomMessageTargetableSupport(OUTPUT_VAR);
            private final CustomMessageTargetableSupport xmlSource = new CustomMessageTargetableSupport(XML_SOURCE_VARIABLE, true);
            private final CustomMessageTargetableSupport jsonSource = new CustomMessageTargetableSupport(JSON_SOURCE_VARIABLE, true);
            private final CustomMessageTargetableSupport soapSource = new CustomMessageTargetableSupport(SOAP_SOURCE_VARIABLE, true);
            private final CustomMessageTargetableSupport textSource = new CustomMessageTargetableSupport(TEXT_SOURCE_VARIABLE, true);
            private final CustomMessageTargetableSupport binSource = new CustomMessageTargetableSupport(BINARY_SOURCE_VARIABLE, true);
            private final CustomMessageTargetableSupport iStreamSource = new CustomMessageTargetableSupport(INPUT_STREAM_SOURCE_VARIABLE, true);
            private final CustomMessageTargetableSupport multiPartSoapSource = new CustomMessageTargetableSupport(MULTIPART_SOURCE_FIRST_PART_SOAP_VARIABLE, true);
            private final CustomMessageTargetableSupport multiPartJsonSource = new CustomMessageTargetableSupport(MULTIPART_SOURCE_FIRST_PART_JSON_VARIABLE, true);

            @Override
            public String getName() {
                return "Test different content-types assertion";
            }

            @Override
            public String getTargetMessageVariable() {
                return defaultSource.getTargetMessageVariable();
            }

            @Override
            public void setTargetMessageVariable(String otherMessageVariable) {
                defaultSource.setTargetMessageVariable(otherMessageVariable);
            }

            @Override
            public String getTargetName() {
                return defaultSource.getTargetName();
            }

            @Override
            public boolean isTargetModifiedByGateway() {
                return defaultSource.isTargetModifiedByGateway();
            }

            @Override
            public VariableMetadata[] getVariablesSet() {
                return new VariableMetadata[]{
                        new VariableMetadata(XML_SOURCE_VARIABLE, true, false, null, false),
                        new VariableMetadata(JSON_SOURCE_VARIABLE, true, false, null, false),
                        new VariableMetadata(SOAP_SOURCE_VARIABLE, true, false, null, false),
                        new VariableMetadata(TEXT_SOURCE_VARIABLE, true, false, null, false),
                        new VariableMetadata(BINARY_SOURCE_VARIABLE, true, false, null, false),
                        new VariableMetadata(INPUT_STREAM_SOURCE_VARIABLE, true, false, null, false),
                        new VariableMetadata(MULTIPART_SOURCE_FIRST_PART_SOAP_VARIABLE, true, false, null, false),
                        new VariableMetadata(MULTIPART_SOURCE_FIRST_PART_JSON_VARIABLE, true, false, null, false),
                };
            }

            @Override
            public String[] getVariablesUsed() {
                return new String[] {
                        XML_SOURCE_VARIABLE,
                        JSON_SOURCE_VARIABLE,
                        SOAP_SOURCE_VARIABLE,
                        TEXT_SOURCE_VARIABLE,
                        BINARY_SOURCE_VARIABLE,
                        INPUT_STREAM_SOURCE_VARIABLE,
                        MULTIPART_SOURCE_FIRST_PART_SOAP_VARIABLE,
                        MULTIPART_SOURCE_FIRST_PART_JSON_VARIABLE
                };
            }

            public CustomMessageTargetableSupport getXmlSource() {
                return xmlSource;
            }

            public CustomMessageTargetableSupport getJsonSource() {
                return jsonSource;
            }

            CustomMessageTargetableSupport getDefaultDestination() {
                return defaultDestination;
            }

            CustomMessageTargetableSupport getDefaultSource() {
                return defaultSource;
            }

            public CustomMessageTargetableSupport getSoapSource() {
                return soapSource;
            }

            public CustomMessageTargetableSupport getTextSource() {
                return textSource;
            }

            public CustomMessageTargetableSupport getBinarySource() {
                return binSource;
            }

            public CustomMessageTargetableSupport getInputStreamSource() {
                return iStreamSource;
            }

            CustomMessageTargetableSupport getMultiPartSoapSource() {
                return multiPartSoapSource;
            }

            CustomMessageTargetableSupport getMultiPartJsonSource() {
                return multiPartJsonSource;
            }
        }

        // add sample Legacy descriptor
        final CustomAssertionDescriptor descriptor = new CustomAssertionDescriptor(
                "Test.TestDifferentContentTypesAssertion",
                TestDifferentContentTypesAssertion.class,
                null, // for now do not use UI class
                TestServiceInvocation.class,
                Category.AUDIT_ALERT,
                "Test All Content-Types CustomAssertion Description",
                null, // for now do not use allowed packages
                null, // for now do not use allowed resources
                null // nodeNames
        );

        // mock the new descriptors
        when(mockRegistrar.getDescriptor(TestDifferentContentTypesAssertion.class)).then(new Returns(descriptor));

        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.AUDIT_ALERT);
        customAssertionHolder.setDescriptionText("Test Content-Types Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestDifferentContentTypesAssertion());

        final HardcodedResponseAssertion hardcodedResponseAssertion = new HardcodedResponseAssertion();
        hardcodedResponseAssertion.setResponseContentType("text/plain; charset=UTF-8");
        hardcodedResponseAssertion.setResponseStatus(HardcodedResponseAssertion.DEFAULT_STATUS);
        hardcodedResponseAssertion.setBase64ResponseBody(HexUtils.encodeBase64(ALLCONTENTTYPE_RESULT.getBytes()));

        final Assertion ass = makePolicy(Arrays.<Assertion>asList(
                makeSetVariable(XML_SOURCE_VARIABLE, ContentTypeHeader.XML_DEFAULT.getFullValue(), XML_SOURCE),
                makeSetVariable(JSON_SOURCE_VARIABLE, ContentTypeHeader.APPLICATION_JSON.getFullValue(), JSON_SOURCE),
                makeSetVariable(SOAP_SOURCE_VARIABLE, ContentTypeHeader.SOAP_1_2_DEFAULT.getFullValue(), SOAP_SOURCE),
                makeSetVariable(TEXT_SOURCE_VARIABLE, ContentTypeHeader.TEXT_DEFAULT.getFullValue(), TEXT_SOURCE),
                makeSetVariable(BINARY_SOURCE_VARIABLE, ContentTypeHeader.OCTET_STREAM_DEFAULT.getFullValue(), BINARY_SOURCE),  // let this be octet content-type
                makeSetVariable(INPUT_STREAM_SOURCE_VARIABLE, ContentTypeHeader.TEXT_DEFAULT.getFullValue(), INPUT_STREAM_SOURCE_BYTES), // let this be text content-type
                customAssertionHolder,
                hardcodedResponseAssertion
        ));
        assertTrue("Is AllAssertion", ass instanceof AllAssertion);
        final AllAssertion allAssertion = (AllAssertion)ass;

        assertNotNull("CustomAssertionHolder cannot be null.", customAssertionHolder);
        assertNotNull("CustomAssertion cannot be null.", customAssertionHolder.getCustomAssertion());
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        assertEquals("Source Target is Request", customAssertionHolder.getTargetName(), "Request");

        final CustomAssertion ca = serviceInvocation.getCustomAssertion();
        assertTrue("CustomAssertion is of type TestDifferentContentTypesAssertion", ca instanceof TestDifferentContentTypesAssertion);
        final TestDifferentContentTypesAssertion customAssertion = (TestDifferentContentTypesAssertion)ca;

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                fail("onRequest should not be called when checkRequest is not returning null!");
                return null;
            }
        }).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                fail("onResponse should not be called when checkRequest is not returning null!");
                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        doAnswer(new Answer<CustomAssertionStatus>() {
            @Override
            public CustomAssertionStatus answer(final InvocationOnMock invocation) throws Throwable
            {
                assertTrue("there is only one parameter for checkRequest", invocation.getArguments().length == 2);

                Object param1 = invocation.getArguments()[0];
                assertTrue("Param1 is ServiceRequest", param1 instanceof ServiceRequest);
                ServiceRequest request = (ServiceRequest)param1;

                Object param2 = invocation.getArguments()[1];
                assertNull("Param2 is null", param2);

                // test the default request
                doTestContentType(request,                                    // ServiceRequest object
                        null,                                                 // targetable (null since we are testing default request)
                        TestMessageType.XML,                                  // expected message format
                        XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE),   // expected XML Document
                        ContentTypeHeader.XML_DEFAULT                         // expected content-type
                );

                // test the default source (again request)
                doTestContentType(request,                                    // ServiceRequest object
                        customAssertion.getDefaultSource(),                   // targetable: default source (should be request)
                        TestMessageType.XML,                                  // expected message format
                        XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE),   // expected XML Document
                        ContentTypeHeader.XML_DEFAULT                         // expected content-type
                );

                // test the xml source
                doTestContentType(request,                                    // ServiceRequest object
                        customAssertion.getXmlSource(),                       // targetable: xmlSource
                        TestMessageType.XML,                                  // expected message format
                        XmlUtil.stringToDocument(XML_SOURCE),                 // expected XML Document
                        ContentTypeHeader.XML_DEFAULT                         // expected content-type
                );

                // test the json source
                doTestContentType(request,                                    // ServiceRequest object
                        customAssertion.getJsonSource(),                      // targetable: jsonSource
                        TestMessageType.JSON,                                 // expected message format
                        JSON_SOURCE,                                          // expected JSON Data
                        ContentTypeHeader.APPLICATION_JSON                    // expected content-type
                );

                // test the soap source
                doTestContentType(request,                                    // ServiceRequest object
                        customAssertion.getSoapSource(),                      // targetable: soapSource
                        TestMessageType.SOAP,                                 // expected message format
                        XmlUtil.stringToDocument(SOAP_SOURCE),                // expected SOAP payload (as XML)
                        ContentTypeHeader.SOAP_1_2_DEFAULT                    // expected content-type
                );

                // test the text source
                doTestContentType(request,                                    // ServiceRequest object
                        customAssertion.getTextSource(),                      // targetable: textSource
                        TestMessageType.TEXT,                                 // expected message format
                        TEXT_SOURCE,                                          // expected SOAP payload
                        ContentTypeHeader.TEXT_DEFAULT                        // expected content-type
                );

                // test the binary source
                doTestContentType(request,                                    // ServiceRequest object
                        customAssertion.getBinarySource(),                    // targetable: binSource
                        TestMessageType.BINARY,                               // expected message format
                        BINARY_SOURCE,                                        // expected bytes array
                        ContentTypeHeader.OCTET_STREAM_DEFAULT                // expected content-type
                );

                // test the input stream source
                doTestContentType(request,                                    // ServiceRequest object
                        customAssertion.getInputStreamSource(),               // targetable: iStreamSource
                        TestMessageType.INPUT_STREAM,                         // expected message format
                        INPUT_STREAM_SOURCE_BYTES,                            // expected bytes array
                        ContentTypeHeader.TEXT_DEFAULT                        // expected content-type
                );

                // test the multipart message (soap first part)
                doTestContentType(request,                                                          // ServiceRequest object
                        customAssertion.getMultiPartSoapSource(),                                   // targetable: mpartSoapSource
                        TestMessageType.MULTIPART_FIRST_PART_SOAP,                                  // expected message format
                        MULTIPART_SOURCE_FIRST_PART_SOAP,                                           // expected bytes array
                        ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_SOAP_CONTENT_TYPE) // expected content-type
                );

                // test the multipart message (json first part)
                doTestContentType(request,                                                          // ServiceRequest object
                        customAssertion.getMultiPartJsonSource(),                                   // targetable: mpartJsonSource
                        TestMessageType.MULTIPART_FIRST_PART_JSON,                                  // expected message format
                        MULTIPART_SOURCE_FIRST_PART_JSON,                                           // expected bytes array
                        ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_JSON_CONTENT_TYPE) // expected content-type
                );

                // set the variables to new values
                request.setDOM(customAssertion.getXmlSource(), XmlUtil.stringToDocument(XML_SOURCE_SET));
                request.setJson(customAssertion.getJsonSource(), JSON_SOURCE_SET);
                request.setBytes(customAssertion.getTextSource(), request.createContentType(CustomContentHeader.Type.TEXT), TEXT_SOURCE_SET.getBytes());
                request.setDOM(customAssertion.getSoapSource(), XmlUtil.stringToDocument(SOAP_SOURCE_SET));
                request.setBytes(customAssertion.getBinarySource(), request.createContentType(CustomContentHeader.Type.TEXT), BINARY_SOURCE_SET);
                request.setEntireMessageBodyFromInputStream(customAssertion.getInputStreamSource(), request.createContentType(CustomContentHeader.Type.TEXT), new ByteArrayInputStream(INPUT_STREAM_SOURCE_BYTES_SET));

                assertEquals("Destination Target is OTHER", customAssertion.getDefaultDestination().getTargetName(), "${" + OUTPUT_VAR + "}");
                request.setBytes(customAssertion.getDefaultDestination(), request.createContentType(CustomContentHeader.Type.XML), SAMPLE_XML_OUT_MESSAGE.getBytes());

                return CustomAssertionStatus.NONE;
            }
        }).when(serviceInvocation).checkRequest(Matchers.<ServiceRequest>any(), Matchers.<ServiceResponse>any());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(allAssertion, false);
        assertTrue("Is of ServerAllAssertion", serverAssertion instanceof ServerAllAssertion);
        final ServerAllAssertion serverAllAssertion = (ServerAllAssertion)serverAssertion;

        final PolicyEnforcementContext context = makeContext(new Message(XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE)), new Message());

        // create multipart message whit soap first part (much easier like this, compared to using set variable assertion)
        Message multiPartSoapMessage = context.getOrCreateTargetMessage(new MessageTargetableSupport(MULTIPART_SOURCE_FIRST_PART_SOAP_VARIABLE), false);
        multiPartSoapMessage.initialize(new ByteArrayStashManager(),
                ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_SOAP_CONTENT_TYPE),
                new ByteArrayInputStream(MULTIPART_SOURCE_FIRST_PART_SOAP.getBytes()), 0);

        // create multipart message whit json first part (much easier like this, compared to using set variable assertion)
        Message multiPartJsonMessage = context.getOrCreateTargetMessage(new MessageTargetableSupport(MULTIPART_SOURCE_FIRST_PART_JSON_VARIABLE), false);
        multiPartJsonMessage.initialize(new ByteArrayStashManager(),
                ContentTypeHeader.parseValue(MULTIPART_SOURCE_FIRST_PART_JSON_CONTENT_TYPE),
                new ByteArrayInputStream(MULTIPART_SOURCE_FIRST_PART_JSON.getBytes()), 0);

        AssertionStatus status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);

        final Message xmlMessageOut = context.getTargetMessage(new MessageTargetableSupport(OUTPUT_VAR));
        final Document outDocDestinationMessage = xmlMessageOut.getXmlKnob().getDocumentReadOnly();
        outDocDestinationMessage.normalizeDocument();
        final Document outputDoc = XmlUtil.stringToDocument(SAMPLE_XML_OUT_MESSAGE);
        outputDoc.normalizeDocument();
        assertTrue("Destination Target Message (destinationTarget) is properly set from assertion", outputDoc.isEqualNode(outDocDestinationMessage));

        final Message responseMsg = context.getResponse();
        final InputStream responseStream = responseMsg.getMimeKnob().getEntireMessageBodyAsInputStream(true);
        final String inputText = new String(com.l7tech.util.IOUtils.slurpStream(responseStream));
        assertEquals("Destination Target Message (RESPONSE) is properly set from assertion", inputText, ALLCONTENTTYPE_RESULT_VALUE);
    }
}
