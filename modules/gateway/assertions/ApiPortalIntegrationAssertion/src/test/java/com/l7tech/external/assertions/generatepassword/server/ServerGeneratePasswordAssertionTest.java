package com.l7tech.external.assertions.generatepassword.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.generatepassword.GeneratePasswordAssertion;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.util.MockInjector;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.util.ConfigFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import java.util.HashMap;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author rraquepo, 4/9/14
 */
public class ServerGeneratePasswordAssertionTest {
    private PolicyEnforcementContext peCtx;
    private GeneratePasswordAssertion assertion;
    private ServerPolicyFactory serverPolicyFactory;
    private Message request;
    private Message response;

    @Before
    public void setUp() throws Exception {
        assertion = new GeneratePasswordAssertion();

        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("serverConfig", ConfigFactory.getCachedConfig());
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
    }

    @Test
    public void testGeneratePassword() throws Exception {
        request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
        response = new Message(XmlUtil.stringAsDocument("<myresponse/>"));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/blah");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_LOWERCASE_CHARACTERS_LENGTH), "2");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_UPPERCASE__CHARACTERS_LENGTH), "2");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_SPECIAL_CHARACTERS), "2");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_NUMBERS_CHARACTERS_LENGTH), "2");

        ServerGeneratePasswordAssertion sass = (ServerGeneratePasswordAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, status);
        String password = peCtx.getVariable(GeneratePasswordAssertion.RESPONSE_PASSWORD).toString();
        assertTrue(password.length() == 8);
    }

    @Test
    public void testGeneratePassword_maxPasswordLength() throws Exception {
        request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
        response = new Message(XmlUtil.stringAsDocument("<myresponse/>"));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/blah");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_LOWERCASE_CHARACTERS_LENGTH), "100");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_UPPERCASE__CHARACTERS_LENGTH), "100");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_SPECIAL_CHARACTERS), "100");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_NUMBERS_CHARACTERS_LENGTH), "100");

        ServerGeneratePasswordAssertion sass = (ServerGeneratePasswordAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, status);
        String password = peCtx.getVariable(GeneratePasswordAssertion.RESPONSE_PASSWORD).toString();
        assertTrue(password.length() == GeneratePassword.MAX_PASSWORD_LENGTH);
    }

    @Test
    public void testGeneratePasswordWithInvalidParam() throws Exception {
        request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
        response = new Message(XmlUtil.stringAsDocument("<myresponse/>"));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/blah");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_LOWERCASE_CHARACTERS_LENGTH), "2");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_UPPERCASE__CHARACTERS_LENGTH), "2");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_SPECIAL_CHARACTERS), "2");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_NUMBERS_CHARACTERS_LENGTH), "blah");

        ServerGeneratePasswordAssertion sass = (ServerGeneratePasswordAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, status);
        String password = peCtx.getVariable(GeneratePasswordAssertion.RESPONSE_PASSWORD).toString();
        assertTrue(password.length() == 6);
    }

    @Test
    public void testGeneratePassword_lowerCaseOnly() throws Exception {
        request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
        response = new Message(XmlUtil.stringAsDocument("<myresponse/>"));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/blah");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_LOWERCASE_CHARACTERS_LENGTH), "10");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_UPPERCASE__CHARACTERS_LENGTH), "0");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_SPECIAL_CHARACTERS), "0");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_NUMBERS_CHARACTERS_LENGTH), "0");

        ServerGeneratePasswordAssertion sass = (ServerGeneratePasswordAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, status);
        String password = peCtx.getVariable(GeneratePasswordAssertion.RESPONSE_PASSWORD).toString();
        assertTrue(password.length() == 10);
        //validate that the generated password belong to that char group
        int lastChar = -1;
        for (char ch : password.toCharArray()) {
            assertTrue(GeneratePassword.VALID_LOWERCASE_CHARS.indexOf(ch) >= 0);
            if (lastChar != -1) {
                assertFalse(lastChar == ch);
            }
            lastChar = ch;
        }
    }

    @Test
    public void testGeneratePassword_upperCaseOnly() throws Exception {
        request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
        response = new Message(XmlUtil.stringAsDocument("<myresponse/>"));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/blah");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_LOWERCASE_CHARACTERS_LENGTH), "0");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_UPPERCASE__CHARACTERS_LENGTH), "10");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_SPECIAL_CHARACTERS), "0");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_NUMBERS_CHARACTERS_LENGTH), "0");

        ServerGeneratePasswordAssertion sass = (ServerGeneratePasswordAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, status);
        String password = peCtx.getVariable(GeneratePasswordAssertion.RESPONSE_PASSWORD).toString();
        assertTrue(password.length() == 10);
        //validate that the generated password belong to that char group
        int lastChar = -1;
        for (char ch : password.toCharArray()) {
            assertTrue(GeneratePassword.VALID_UPPERCASE_CHARS.indexOf(ch) >= 0);
            if (lastChar != -1) {
                assertFalse(lastChar == ch);
            }
            lastChar = ch;
        }
    }

    @Test
    public void testGeneratePassword_numbersOnly() throws Exception {
        request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
        response = new Message(XmlUtil.stringAsDocument("<myresponse/>"));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/blah");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_LOWERCASE_CHARACTERS_LENGTH), "0");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_UPPERCASE__CHARACTERS_LENGTH), "0");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_SPECIAL_CHARACTERS), "0");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_NUMBERS_CHARACTERS_LENGTH), "10");

        ServerGeneratePasswordAssertion sass = (ServerGeneratePasswordAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, status);
        String password = peCtx.getVariable(GeneratePasswordAssertion.RESPONSE_PASSWORD).toString();
        assertTrue(password.length() == 10);
        //validate that the generated password belong to that char group
        int lastChar = -1;
        for (char ch : password.toCharArray()) {
            assertTrue(GeneratePassword.VALID_NUMBER_CHARS.indexOf(ch) >= 0);
            if (lastChar != -1) {
                assertFalse(lastChar == ch);
            }
            lastChar = ch;
        }
    }

    @Test
    public void testGeneratePassword_specialCharOnly() throws Exception {
        request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
        response = new Message(XmlUtil.stringAsDocument("<myresponse/>"));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/blah");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_LOWERCASE_CHARACTERS_LENGTH), "0");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_UPPERCASE__CHARACTERS_LENGTH), "0");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_SPECIAL_CHARACTERS), "10");
        peCtx.setVariable(getAsVariableName(GeneratePasswordAssertion.PARAM_NUMBERS_CHARACTERS_LENGTH), "0");

        ServerGeneratePasswordAssertion sass = (ServerGeneratePasswordAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, status);
        String password = peCtx.getVariable(GeneratePasswordAssertion.RESPONSE_PASSWORD).toString();
        assertTrue(password.length() == 10);
        //validate that the generated password belong to that char group
        int lastChar = -1;
        for (char ch : password.toCharArray()) {
            assertTrue(GeneratePassword.VALID_SPECIAL_CHARS.indexOf(ch) >= 0);
            if (lastChar != -1) {
                assertFalse(lastChar == ch);
            }
            lastChar = ch;
        }
    }

    protected static String getAsVariableName(String varName) {
        if (varName.startsWith("${")) {
            return varName.substring(2, varName.length() - 1);
        }
        return varName;
    }
}
