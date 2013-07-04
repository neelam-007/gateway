package com.l7tech.server.custom.knob;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.TestCustomMessageTargetable;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionStatus;
import com.l7tech.policy.assertion.ext.message.CustomMessage;
import com.l7tech.policy.assertion.ext.message.CustomPolicyContext;
import com.l7tech.policy.assertion.ext.message.knob.CustomHttpHeadersKnob;
import com.l7tech.policy.assertion.ext.message.knob.CustomMessageKnob;
import com.l7tech.policy.assertion.ext.message.knob.NoSuchKnobException;
import com.l7tech.server.custom.CustomMessageImpl;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.ServerCustomAssertionHolder;
import com.l7tech.server.policy.custom.CustomAssertionsPolicyTestBase;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test CustomMessageKnob
 */
@RunWith(MockitoJUnitRunner.class)
public class CustomMessageKnobTest extends CustomAssertionsPolicyTestBase {

    @Before
    public void setUp() throws Exception {
        // call base init
        doInit();
    }

    private void printKnownKnobs(final CustomMessage message) throws Exception {
        System.out.println("-------------------------------------------------------------------");
        for (CustomMessageKnob knob: message.getAttachedKnobs()) {
            System.out.println("Knob; name = " + knob.getKnobName() + "; desc = " + knob.getKnobName() + ";");
        }
        System.out.println("-------------------------------------------------------------------");
    }

    @Test
    public void test_AttachingKnobs() throws Exception {

        class TestMessageKnob1 extends CustomMessageKnobBase {
            public TestMessageKnob1(@NotNull String name, @NotNull String description) { super(name, description); }
            public String method1() { return "test method1"; }
        }

        class TestMessageKnob2 extends CustomMessageKnobBase {
            public TestMessageKnob2(@NotNull String name, @NotNull String description) { super(name, description); }
            public String method2() { return "test method2"; }
        }

        // create our assertion CustomAssertionHolder
        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.ACCESS_CONTROL);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestCustomMessageTargetable("request", "response"));

        // set serviceInvocation mock to use our custom assertion
        assertTrue("Is instance of TestTargetableAssertion", customAssertionHolder.getCustomAssertion() instanceof TestCustomMessageTargetable);
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        // compile our policy
        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(customAssertionHolder, false);
        assertTrue("Is instance of ServerCustomAssertionHolder", serverAssertion instanceof ServerCustomAssertionHolder);
        final ServerCustomAssertionHolder serverCustomAssertionHolder = spy((ServerCustomAssertionHolder) serverAssertion);

        // get our CustomAssertion
        final TestCustomMessageTargetable customAssertion = (TestCustomMessageTargetable)customAssertionHolder.getCustomAssertion();

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final CustomMessageImpl message = (CustomMessageImpl) invocation.getArguments()[0];

                // add sample knobs
                message.attachKnob(TestMessageKnob1.class, new TestMessageKnob1("TestMessageKnob1", "TestMessageKnob1 desc"));
                message.attachKnob(TestMessageKnob2.class, new TestMessageKnob2("TestMessageKnob2", "TestMessageKnob2 desc"));

                return null;
            }
        }).when(serverCustomAssertionHolder).doAttachKnobs(Matchers.<CustomMessageImpl>any());

        doAnswer(new Answer<CustomAssertionStatus>() {
            @Override
            public CustomAssertionStatus answer(InvocationOnMock invocation) throws Throwable {
                assertTrue("there is only one parameter for onRequest", invocation.getArguments().length == 1);

                final Object param1 = invocation.getArguments()[0];
                assertTrue("Param is CustomPolicyContext", param1 instanceof CustomPolicyContext);
                final CustomPolicyContext policyContext = (CustomPolicyContext) param1;

                CustomMessage sourceMsg = policyContext.getTargetMessage(customAssertion.getSourceTarget());
                assertNotNull(sourceMsg);
                assertTrue(sourceMsg.getAttachedKnobs().size() == 2);
                TestMessageKnob1 knob1 = sourceMsg.getKnob(TestMessageKnob1.class);
                assertNotNull(knob1);
                assertEquals(knob1.method1(), "test method1");
                TestMessageKnob2 knob2 = sourceMsg.getKnob(TestMessageKnob2.class);
                assertNotNull(knob2);
                assertEquals(knob2.method2(), "test method2");

                printKnownKnobs(sourceMsg);

                CustomMessage destinationMsg = policyContext.getTargetMessage(customAssertion.getDestinationTarget());
                assertNotNull(destinationMsg);
                assertTrue(destinationMsg.getAttachedKnobs().size() == 2);
                knob1 = destinationMsg.getKnob(TestMessageKnob1.class);
                assertNotNull(knob1);
                assertEquals(knob1.method1(), "test method1");
                knob2 = destinationMsg.getKnob(TestMessageKnob2.class);
                assertNotNull(knob2);
                assertEquals(knob2.method2(), "test method2");

                printKnownKnobs(destinationMsg);

                return CustomAssertionStatus.NONE;
            }
        }).when(serviceInvocation).checkRequest(Matchers.<CustomPolicyContext>any());

        final PolicyEnforcementContext context = makeContext(new Message(), new Message());
        final AssertionStatus status = serverCustomAssertionHolder.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void test_NoSuchKnobException() throws Exception {

        class TestMissingKnob extends CustomMessageKnobBase {
            public TestMissingKnob(@NotNull String name, @NotNull String description) { super(name, description); }
        }

        // create our assertion CustomAssertionHolder
        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.ACCESS_CONTROL);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestCustomMessageTargetable("request", "response"));

        // set serviceInvocation mock to use our custom assertion
        assertTrue("Is instance of TestTargetableAssertion", customAssertionHolder.getCustomAssertion() instanceof TestCustomMessageTargetable);
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        // compile our policy
        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(customAssertionHolder, false);
        assertTrue("Is instance of ServerCustomAssertionHolder", serverAssertion instanceof ServerCustomAssertionHolder);
        final ServerCustomAssertionHolder serverCustomAssertionHolder = spy((ServerCustomAssertionHolder) serverAssertion);

        // get our CustomAssertion
        final TestCustomMessageTargetable customAssertion = (TestCustomMessageTargetable)customAssertionHolder.getCustomAssertion();

        doAnswer(new Answer<CustomAssertionStatus>() {
            @Override
            public CustomAssertionStatus answer(InvocationOnMock invocation) throws Throwable {
                assertTrue("there is only one parameter for onRequest", invocation.getArguments().length == 1);

                final Object param1 = invocation.getArguments()[0];
                assertTrue("Param is CustomPolicyContext", param1 instanceof CustomPolicyContext);
                final CustomPolicyContext policyContext = (CustomPolicyContext) param1;

                CustomMessage sourceMsg = policyContext.getTargetMessage(customAssertion.getSourceTarget());
                assertNotNull(sourceMsg);

                printKnownKnobs(sourceMsg);

                try {
                    sourceMsg.getKnob(TestMissingKnob.class);
                    fail("sourceMsg.getKnob should throw");
                } catch (NoSuchKnobException ignore) {}

                return CustomAssertionStatus.NONE;
            }
        }).when(serviceInvocation).checkRequest(Matchers.<CustomPolicyContext>any());

        final PolicyEnforcementContext context = makeContext(new Message(), new Message());
        final AssertionStatus status = serverCustomAssertionHolder.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void test_CustomHttpHeadersKnob() throws Exception {

        final String[] HEADER_NAMES = {"header1", "header2", "header3"};
        final String[][] HEADER_VALUES = {{"header1_value"}, {"header2_value"}, {"header3_multi_value1", "header3_multi_value2", "header3_multi_value3"}};

        // create our assertion CustomAssertionHolder
        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.ACCESS_CONTROL);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestCustomMessageTargetable("request", "response"));

        // set serviceInvocation mock to use our custom assertion
        assertTrue("Is instance of TestTargetableAssertion", customAssertionHolder.getCustomAssertion() instanceof TestCustomMessageTargetable);
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        // compile our policy
        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(customAssertionHolder, false);
        assertTrue("Is instance of ServerCustomAssertionHolder", serverAssertion instanceof ServerCustomAssertionHolder);
        final ServerCustomAssertionHolder serverCustomAssertionHolder = spy((ServerCustomAssertionHolder) serverAssertion);

        // get our CustomAssertion
        final TestCustomMessageTargetable customAssertion = (TestCustomMessageTargetable)customAssertionHolder.getCustomAssertion();

        doAnswer(new Answer<CustomAssertionStatus>() {
            @Override
            public CustomAssertionStatus answer(InvocationOnMock invocation) throws Throwable {
                assertTrue("there is only one parameter for onRequest", invocation.getArguments().length == 1);

                final Object param1 = invocation.getArguments()[0];
                assertTrue("Param is CustomPolicyContext", param1 instanceof CustomPolicyContext);
                final CustomPolicyContext policyContext = (CustomPolicyContext) param1;

                final CustomMessage sourceMsg = policyContext.getTargetMessage(customAssertion.getSourceTarget());
                assertNotNull(sourceMsg);

                printKnownKnobs(sourceMsg);

                final CustomHttpHeadersKnob knob = sourceMsg.getKnob(CustomHttpHeadersKnob.class);
                final String[] headerNames = knob.getHeaderNames();
                assertTrue(Arrays.equals(headerNames, HEADER_NAMES));
                for (int i = 0; i < headerNames.length; ++i) {
                    assertTrue(Arrays.equals(knob.getHeaderValues(headerNames[i]), HEADER_VALUES[i]));
                }

                return CustomAssertionStatus.NONE;
            }
        }).when(serviceInvocation).checkRequest(Matchers.<CustomPolicyContext>any());

        // create sample request
        final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("GET", "url");
        mockHttpServletRequest.addHeader(HEADER_NAMES[0], HEADER_VALUES[0]);
        mockHttpServletRequest.addHeader(HEADER_NAMES[1], HEADER_VALUES[1]);
        mockHttpServletRequest.addHeader(HEADER_NAMES[2], HEADER_VALUES[2]);

        // create request message
        final Message request = new Message();
        request.initialize(ContentTypeHeader.TEXT_DEFAULT, "Test Content".getBytes());
        // attach headers
        request.attachKnob(HttpRequestKnob.class, new HttpServletRequestKnob(mockHttpServletRequest));
        request.attachKnob(HttpServletRequestKnob.class, new HttpServletRequestKnob(mockHttpServletRequest));
        
        final PolicyEnforcementContext context = makeContext(request, new Message());
        final AssertionStatus status = serverCustomAssertionHolder.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);

    }
}
