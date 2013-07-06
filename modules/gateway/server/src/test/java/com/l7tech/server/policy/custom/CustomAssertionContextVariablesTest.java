package com.l7tech.server.policy.custom;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.*;
import com.l7tech.policy.assertion.ext.message.CustomPolicyContext;
import com.l7tech.policy.variable.ContextVariablesUtils;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.ServerCustomAssertionHolder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.Map;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomAssertionContextVariablesTest {

    @Spy
    private ServiceInvocation serviceInvocation = new TestServiceInvocation();

    @Mock
    private ApplicationContext applicationContextMock;

    @Mock
    private CustomAssertionsRegistrar customAssertionRegistrarMock;

    @Mock
    private CustomAssertionDescriptor descriptorMock;

    @Mock
    private Auditor auditorMock;

    private class TestServiceInvocation extends ServiceInvocation {
        // Extend abstract ServiceInvocation class.
    }

    private class TestCustomAssertion implements CustomAssertion {
        @Override
        public String getName() {
            return "Test Custom Assertion";
        }
    }

    @Before
    public void setup() {
        when(applicationContextMock.getBean("customAssertionRegistrar")).thenReturn(customAssertionRegistrarMock);
        when(customAssertionRegistrarMock.getDescriptor(Matchers.<Class<?>>any())).thenReturn(descriptorMock);
        when(descriptorMock.getServerAssertion()).thenReturn(TestServiceInvocation.class);
    }

    @Test
    public void testGetReferenceNames() {
        String input = "${var1}, ${var1[0]}, ${var1|!}, ${var1|}, ${var2}, var3";
        String[] expected = {"var1", "var1", "var1", "var1", "var2"};

        String[] actual = ContextVariablesUtils.getReferencedNames(input);
        assertArrayEquals(actual, expected);

        // Test against results from Syntax.getReferencedNames(s) method.
        //
        expected = Syntax.getReferencedNames(input);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testExpandVariableUsingDefaultVariableMap() throws Exception {

        doAnswer(new Answer<CustomAssertionStatus>() {
            @Override
            public CustomAssertionStatus answer(final InvocationOnMock invocation) throws Throwable {
                CustomPolicyContext customPolicyContext = (CustomPolicyContext) invocation.getArguments()[0];

                // Not a context variable.
                //
                String actual = customPolicyContext.expandVariable("test");
                assertEquals("test", actual);

                // Not defined context variable.
                //
                actual = customPolicyContext.expandVariable("${test}");
                assertEquals("", actual);

                // Single-value context variable.
                //
                actual = customPolicyContext.expandVariable("${var1}");
                assertEquals("value_variable1", actual);

                // Multi-value context variable.
                //
                actual = customPolicyContext.expandVariable("${var2}");
                assertEquals("value_variable2_1, value_variable2_2, value_variable2_3", actual);

                actual = customPolicyContext.expandVariable("${var2[0]}");
                assertEquals("value_variable2_1", actual);

                actual = customPolicyContext.expandVariable("${var2[1]}");
                assertEquals("value_variable2_2", actual);

                actual = customPolicyContext.expandVariable("${var2[2]}");
                assertEquals("value_variable2_3", actual);

                actual = customPolicyContext.expandVariable("${var2[3]}"); // index out of range
                assertEquals("", actual);

                // Multi-value context variable with user specified delimiter
                //
                actual = customPolicyContext.expandVariable("${var2|!}");
                assertEquals("value_variable2_1!value_variable2_2!value_variable2_3", actual);

                // Multi-value context variable without a delimiter
                //
                actual = customPolicyContext.expandVariable("${var2|}");
                assertEquals("value_variable2_1value_variable2_2value_variable2_3", actual);

                return CustomAssertionStatus.NONE;
            }
        }).when(serviceInvocation).checkRequest(Matchers.<CustomPolicyContext>any());

        PolicyEnforcementContext pec = createPolicyEnforcementContext();
        ServerCustomAssertionHolder serverCustomAssertionHolder = this.createServerCustomAssertionHolder();
        AssertionStatus status = serverCustomAssertionHolder.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testExpandVariableUsingUserVariableMap() throws Exception {

        doAnswer(new Answer<CustomAssertionStatus>() {
            @Override
            public CustomAssertionStatus answer(final InvocationOnMock invocation) throws Throwable {
                CustomPolicyContext customPolicyContext = (CustomPolicyContext) invocation.getArguments()[0];

                Map<String, Object> vars = customPolicyContext.getVariableMap(new String[]{"var1", "var2"});

                // Not a context variable.
                //
                String actual = customPolicyContext.expandVariable("test", vars);
                assertEquals("test", actual);

                // Not defined context variable.
                //
                actual = customPolicyContext.expandVariable("${test}", vars);
                assertEquals("", actual);

                // Single-value context variable.
                //
                actual = customPolicyContext.expandVariable("${var1}", vars);
                assertEquals("value_variable1", actual);

                // Multi-value context variable.
                //
                actual = customPolicyContext.expandVariable("${var2}", vars);
                assertEquals("value_variable2_1, value_variable2_2, value_variable2_3", actual);

                actual = customPolicyContext.expandVariable("${var2[0]}", vars);
                assertEquals("value_variable2_1", actual);

                actual = customPolicyContext.expandVariable("${var2[1]}", vars);
                assertEquals("value_variable2_2", actual);

                actual = customPolicyContext.expandVariable("${var2[2]}", vars);
                assertEquals("value_variable2_3", actual);

                actual = customPolicyContext.expandVariable("${var2[3]}", vars); // index out of range
                assertEquals("", actual);

                // Multi-value context variable with user specified delimiter
                //
                actual = customPolicyContext.expandVariable("${var2|!}", vars);
                assertEquals("value_variable2_1!value_variable2_2!value_variable2_3", actual);

                // Multi-value context variable without a delimiter
                //
                actual = customPolicyContext.expandVariable("${var2|}", vars);
                assertEquals("value_variable2_1value_variable2_2value_variable2_3", actual);

                return CustomAssertionStatus.NONE;
            }
        }).when(serviceInvocation).checkRequest(Matchers.<CustomPolicyContext>any());

        PolicyEnforcementContext pec = createPolicyEnforcementContext();
        ServerCustomAssertionHolder serverCustomAssertionHolder = this.createServerCustomAssertionHolder();
        AssertionStatus status = serverCustomAssertionHolder.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, status);
    }

    private PolicyEnforcementContext createPolicyEnforcementContext() throws Exception {
        Message request = new Message();
        Message response = new Message();
        response.initialize(ContentTypeHeader.parseValue("text/xml; charset=utf-8"), "<data>response</data>".getBytes());

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        // Set context variables for testing.
        //
        pec.setVariable("var1", "value_variable1");
        pec.setVariable("var2", new String[]{"value_variable2_1", "value_variable2_2", "value_variable2_3"});

        return pec;
    }

    private ServerCustomAssertionHolder createServerCustomAssertionHolder() {
        CustomAssertionHolder assertion = new CustomAssertionHolder();
        assertion.setCategories(Category.AUDIT_ALERT);
        assertion.setDescriptionText("Test Custom Assertion");
        assertion.setCustomAssertion(new TestCustomAssertion());

        ServerCustomAssertionHolder serverCustomAssertionHolder = new ServerCustomAssertionHolder(assertion, applicationContextMock);

        // Use reflection to set serviceInvocation.
        try {
            Field field = serverCustomAssertionHolder.getClass().getDeclaredField("serviceInvocation");
            field.setAccessible(true);
            field.set(serverCustomAssertionHolder, serviceInvocation);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to inject ServerCustomAssertionHolder#serviceInvocation field.");
        }

        return serverCustomAssertionHolder;
    }
}