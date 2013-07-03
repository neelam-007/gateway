package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.ext.*;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.composite.ServerAllAssertion;
import com.l7tech.server.util.Injector;
import com.l7tech.util.HexUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.AccessControlException;
import java.security.GeneralSecurityException;
import java.util.*;

import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import javax.security.auth.login.FailedLoginException;
import javax.xml.parsers.ParserConfigurationException;

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

        // add sample Legacy descriptor
        final CustomAssertionDescriptor descriptorLegacy = new CustomAssertionDescriptor(
                "Test.TestLegacyCustomAssertion",
                TestLegacyCustomAssertion.class,
                TestServiceInvocation.class,
                Category.AUDIT_ALERT
        );
        descriptorLegacy.setDescription("Test CustomAssertion Description");

        // mock the new descriptors
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

    @Test
    public void testNullStatusReturn() throws Exception {
        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.ACCESS_CONTROL);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestLegacyCustomAssertion());

        assertNotNull("CustomAssertionHolder cannot be null.", customAssertionHolder);
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(customAssertionHolder, false);
        assertTrue("Is instance of ServerCustomAssertionHolder", serverAssertion instanceof ServerCustomAssertionHolder);
        final ServerCustomAssertionHolder serverCustomAssertionHolder = (ServerCustomAssertionHolder)serverAssertion;

        final PolicyEnforcementContext context = makeContext(new Message(XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE)), new Message());

        doReturn(null).when(serviceInvocation).checkRequest(Matchers.<ServiceRequest>any(), Matchers.<ServiceResponse>any());
        final AssertionStatus status = serverCustomAssertionHolder.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void test_onRequest_For_IOException_And_GeneralSecurityException() throws Exception {
        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.ACCESS_CONTROL);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestLegacyCustomAssertion());

        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseContentType("text/xml; charset=UTF-8");
        responseAssertion.setResponseStatus(HardcodedResponseAssertion.DEFAULT_STATUS);
        responseAssertion.setBase64ResponseBody(HexUtils.encodeBase64(SAMPLE_XML_INPUT_MESSAGE.getBytes()));

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
                fail("onResponse should not be called when policy is after routing assertion!");
                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(allAssertion, false);
        assertTrue("Is instance of ServerAllAssertion", serverAssertion instanceof ServerAllAssertion);
        final ServerAllAssertion serverAllAssertion = (ServerAllAssertion)serverAssertion;

        final PolicyEnforcementContext context = makeContext(new Message(XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE)), new Message());

        //noinspection deprecation
        doThrow(IOException.class).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());
        AssertionStatus status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, status);

        //noinspection deprecation
        doThrow(GeneralSecurityException.class).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.UNAUTHORIZED, status);

        //noinspection deprecation
        doThrow(FailedLoginException.class).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.AUTH_FAILED, status);

        //noinspection deprecation
        doThrow(AccessControlException.class).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.UNAUTHORIZED, status);

        //noinspection deprecation
        doThrow(ParserConfigurationException.class).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any()); // throw some other exception
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void test_onResponse_For_IOException_And_GeneralSecurityException() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseContentType("text/xml; charset=UTF-8");
        responseAssertion.setResponseStatus(HardcodedResponseAssertion.DEFAULT_STATUS);
        responseAssertion.setBase64ResponseBody(HexUtils.encodeBase64(SAMPLE_XML_INPUT_MESSAGE.getBytes()));

        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.ACCESS_CONTROL);
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

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(allAssertion, false);
        assertTrue("Is instance of ServerAllAssertion", serverAssertion instanceof ServerAllAssertion);
        final ServerAllAssertion serverAllAssertion = (ServerAllAssertion)serverAssertion;

        final PolicyEnforcementContext context = makeContext(new Message(XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE)), new Message());

        //noinspection deprecation
        doThrow(IOException.class).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());
        AssertionStatus status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, status);

        //noinspection deprecation
        doThrow(GeneralSecurityException.class).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.UNAUTHORIZED, status);

        //noinspection deprecation
        doThrow(FailedLoginException.class).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.AUTH_FAILED, status);

        //noinspection deprecation
        doThrow(AccessControlException.class).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.UNAUTHORIZED, status);

        //noinspection deprecation
        doThrow(ParserConfigurationException.class).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any()); // throw some other exception
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void testLegacyBeforeRoute() throws Exception
    {
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
    public void testLegacyAfterRoute() throws Exception
    {
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
}
