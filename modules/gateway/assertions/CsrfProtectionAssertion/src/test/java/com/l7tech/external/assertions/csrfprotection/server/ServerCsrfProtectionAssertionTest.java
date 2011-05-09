package com.l7tech.external.assertions.csrfprotection.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.csrfprotection.CsrfProtectionAssertion;
import com.l7tech.external.assertions.csrfprotection.HttpParameterType;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpRequestKnobAdapter;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextWrapper;
import com.l7tech.test.BugNumber;
import junit.framework.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test the CsrfProtectionAssertion.
 */
public class ServerCsrfProtectionAssertionTest {
    @Test
    public void testDoubleSubmit_Fail_NoCookies_NoHeaders_Post() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.GET,
                new HashMap<String, String[]>(),
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(true);
        assertion.setCookieName("SESSION_ID");
        assertion.setParameterName("sessionID");
        assertion.setParameterType(HttpParameterType.POST);
        assertion.setEnableHttpRefererChecking(false);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void testDoubleSubmit_Fail_WithCookies_NoHeaders_Post() throws Exception {
        HashMap<String, String> cookies = new HashMap<String, String>();
        cookies.put("SESSION_ID", "1234567890ABCDEF");

        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                cookies,
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(true);
        assertion.setCookieName("SESSION_ID");
        assertion.setParameterName("sessionID");
        assertion.setParameterType(HttpParameterType.POST);
        assertion.setEnableHttpRefererChecking(false);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void testDoubleSubmit_Fail_WithBoth_ParameterMissing() throws Exception {
        HashMap<String, String> cookies = new HashMap<String, String>();
        cookies.put("SESSION_ID", "1234567890ABCDEF");
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("abc", new String[] {"123"});

        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                cookies,
                HttpMethod.POST,
                parameters,
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(true);
        assertion.setCookieName("SESSION_ID");
        assertion.setParameterName("sessionID");
        assertion.setParameterType(HttpParameterType.POST);
        assertion.setEnableHttpRefererChecking(false);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void testDoubleSubmit_Fail_Cookie_And_Parameter_Values_No_Match() throws Exception {
        HashMap<String, String> cookies = new HashMap<String, String>();
        cookies.put("SESSION_ID", "1234567890ABCDEF");
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("sessionID", new String[] {"123"});

        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                cookies,
                HttpMethod.POST,
                parameters,
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(true);
        assertion.setCookieName("SESSION_ID");
        assertion.setParameterName("sessionID");
        assertion.setParameterType(HttpParameterType.POST);
        assertion.setEnableHttpRefererChecking(false);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void testDoubleSubmit_Fail_Multiple_Parameter_Values() throws Exception {
        HashMap<String, String> cookies = new HashMap<String, String>();
        cookies.put("SESSION_ID", "1234567890ABCDEF");
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("sessionID", new String[] {"123", "456"});

        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                cookies,
                HttpMethod.POST,
                parameters,
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(true);
        assertion.setCookieName("SESSION_ID");
        assertion.setParameterName("sessionID");
        assertion.setParameterType(HttpParameterType.POST);
        assertion.setEnableHttpRefererChecking(false);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void testDoubleSubmit_Fail_Incorrect_Http_Method_Requires_Post() throws Exception {
        HashMap<String, String> cookies = new HashMap<String, String>();
        cookies.put("SESSION_ID", "1234567890ABCDEF");
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("sessionID", new String[] {"123"});

        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                cookies,
                HttpMethod.GET,
                parameters,
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(true);
        assertion.setCookieName("SESSION_ID");
        assertion.setParameterName("sessionID");
        assertion.setParameterType(HttpParameterType.POST);
        assertion.setEnableHttpRefererChecking(false);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    /**
     * Fail non supported HTTP method (e.g. PUT).
     * @throws Exception
     */
    @Test
    public void testDoubleSubmit_Fail_Http_Method_Not_Supported() throws Exception {
        HashMap<String, String> cookies = new HashMap<String, String>();
        String sessionID = "1234567890ABCDEF";
        cookies.put("SESSION_ID", sessionID);
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("sessionID", new String[] {sessionID});

        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                cookies,
                HttpMethod.PUT,  // non supported http method
                parameters,
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(true);
        assertion.setCookieName("SESSION_ID");
        assertion.setParameterName("sessionID");
        assertion.setParameterType(HttpParameterType.GET_AND_POST);
        assertion.setEnableHttpRefererChecking(false);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void testDoubleSubmit_Success_Post() throws Exception {
        HashMap<String, String> cookies = new HashMap<String, String>();
        String sessionID = "1234567890ABCDEF";
        cookies.put("SESSION_ID", sessionID);
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("sessionID", new String[] {sessionID});

        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                cookies,
                HttpMethod.POST,
                parameters,
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(true);
        assertion.setCookieName("SESSION_ID");
        assertion.setParameterName("sessionID");
        assertion.setParameterType(HttpParameterType.POST);
        assertion.setEnableHttpRefererChecking(false);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(sessionID, context.getVariable(CsrfProtectionAssertion.CTX_VAR_NAME_CSRF_VALID_TOKEN));
    }

    @Test
    public void testDoubleSubmit_Success_Get() throws Exception {
        HashMap<String, String> cookies = new HashMap<String, String>();
        String sessionID = "1234567890ABCDEF";
        cookies.put("SESSION_ID", sessionID);
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("sessionID", new String[] {sessionID});

        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                cookies,
                HttpMethod.GET,
                parameters,
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(true);
        assertion.setCookieName("SESSION_ID");
        assertion.setParameterName("sessionID");
        assertion.setParameterType(HttpParameterType.GET);
        assertion.setEnableHttpRefererChecking(false);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(sessionID, context.getVariable(CsrfProtectionAssertion.CTX_VAR_NAME_CSRF_VALID_TOKEN));
    }

    @Test
    public void testDoubleSubmit_Success_Get_BothAllowed() throws Exception {
        HashMap<String, String> cookies = new HashMap<String, String>();
        String sessionID = "1234567890ABCDEF";
        cookies.put("SESSION_ID", sessionID);
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("sessionID", new String[] {sessionID});

        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                cookies,
                HttpMethod.GET,
                parameters,
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(true);
        assertion.setCookieName("SESSION_ID");
        assertion.setParameterName("sessionID");
        assertion.setParameterType(HttpParameterType.GET_AND_POST);
        assertion.setEnableHttpRefererChecking(false);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(sessionID, context.getVariable(CsrfProtectionAssertion.CTX_VAR_NAME_CSRF_VALID_TOKEN));
    }

    @Test
    public void testReferer_Fail_Header_Missing() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowMissingOrEmptyReferer(false);
        assertion.setOnlyAllowCurrentDomain(true); // not important

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void testReferer_Fail_Header_Invalid_CurrentDomain() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[]{"Referer: http://otherdomain.com/page.html"});

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowMissingOrEmptyReferer(false);
        assertion.setOnlyAllowCurrentDomain(true);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void testReferer_Fail_Header_Invalid_TrustedList() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[]{"Referer: http://otherdomain.com/page.html"});//not in valid list

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowMissingOrEmptyReferer(false);
        assertion.setOnlyAllowCurrentDomain(false);
        ArrayList<String> trustedDomains = new ArrayList<String>();
        trustedDomains.add("layer7tech.com");
        assertion.setTrustedDomains(trustedDomains);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void testReferer_Fail_Header_Invalid_TrustedList_Relative() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                //not in valid list - this will be converted to http://localhost:8080/page.html as context is request URL when relative.
                new String[]{"Referer: /page.html"});

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowMissingOrEmptyReferer(false);
        assertion.setOnlyAllowCurrentDomain(false);
        ArrayList<String> trustedDomains = new ArrayList<String>();
        trustedDomains.add("layer7tech.com");
        assertion.setTrustedDomains(trustedDomains);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @BugNumber(10420)
    @Test
    public void testReferer_Fail_SubDomain_TrustedList() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[]{"Referer: http://sub.layer7tech.com/page.html"});

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowMissingOrEmptyReferer(false);
        assertion.setOnlyAllowCurrentDomain(false);
        ArrayList<String> trustedDomains = new ArrayList<String>();
        trustedDomains.add("layer7tech.com");
        assertion.setTrustedDomains(trustedDomains);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @BugNumber(10420)
    @Test
    public void testReferer_Fail_SubDomain_CurrentDomain() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://layer7tech:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[]{"Referer: http://sub.layer7tech.com/page.html"});

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowMissingOrEmptyReferer(false);
        assertion.setOnlyAllowCurrentDomain(true);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void testReferer_Success_Missing_Header_Allowed_CurrentDomain() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowMissingOrEmptyReferer(true);
        assertion.setOnlyAllowCurrentDomain(true);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(status, AssertionStatus.NONE);
    }

    /**
     * Same as testReferer_Success_Missing_Header_Allowed_CurrentDomain - code paths are identical.
     */
    @Test
    public void testReferer_Success_Missing_Header_Allowed_TrustedList() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowMissingOrEmptyReferer(true);
        assertion.setOnlyAllowCurrentDomain(false);
        ArrayList<String> trustedDomains = new ArrayList<String>();
        trustedDomains.add("layer7tech.com");
        assertion.setTrustedDomains(trustedDomains);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(status, AssertionStatus.NONE);
    }

    @Test
    public void testReferer_Success_CurrentDomain_Relative() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[] {"Referer: /page.html"});

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowMissingOrEmptyReferer(true);
        assertion.setOnlyAllowCurrentDomain(true);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(status, AssertionStatus.NONE);
    }

    @Test
    public void testReferer_Success_CurrentDomain() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[] {"Referer: http://localhost:8080/page.html"});

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowMissingOrEmptyReferer(true);
        assertion.setOnlyAllowCurrentDomain(true);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(status, AssertionStatus.NONE);
    }

    @Test
    public void testReferer_Success_TrustedListMatch() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[] {"Referer: http://www.layer7tech.com/page.html"});

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowMissingOrEmptyReferer(true);
        assertion.setOnlyAllowCurrentDomain(false);
        ArrayList<String> trustedDomains = new ArrayList<String>();
        trustedDomains.add("www.layer7tech.com");
        assertion.setTrustedDomains(trustedDomains);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(status, AssertionStatus.NONE);
    }

    /**
     * If setAllowEmptyOrMissingReferer(true), must succeed when referer is null or empty string
     * @throws Exception
     */
    @Test
    public void testReferer_Success_RefererPresentButEmptyOrNull() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[] {"Referer: "});   // empty string referer

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowMissingOrEmptyReferer(true);
        assertion.setOnlyAllowCurrentDomain(true);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals(status, AssertionStatus.NONE);

        context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[] {});   // null referer
        status = serverAssertion.checkRequest(context);
        Assert.assertEquals(status, AssertionStatus.NONE);
    }

    @Test
    public void testBothDoubleCookieAndRefererSuccessful() throws Exception {
        HashMap<String, String> cookies = new HashMap<String, String>();
        String sessionID = "1234567890ABCDEF";
        cookies.put("SESSION_ID", sessionID);
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("sessionID", new String[] {sessionID});

        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                cookies,
                HttpMethod.GET,
                parameters,
                new String[] {"Referer: http://localhost:8080"});

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(true);
        assertion.setCookieName("SESSION_ID");
        assertion.setParameterName("sessionID");
        assertion.setParameterType(HttpParameterType.GET_AND_POST);
        assertion.setEnableHttpRefererChecking(true);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(sessionID, context.getVariable(CsrfProtectionAssertion.CTX_VAR_NAME_CSRF_VALID_TOKEN));
    }

    private PolicyEnforcementContext createPolicyEnforcementContext(final URL url,
                                                                    final HashMap<String, String> cookies,
                                                                    final HttpMethod method,
                                                                    final HashMap<String, String[]> parameters,
                                                                    final String[] headers) {
        return createPolicyEnforcementContext(url,  cookies, method, parameters, headers, new HashMap<String, Object>());
    }

    private PolicyEnforcementContext createPolicyEnforcementContext(final URL url,
                                                                    final HashMap<String, String> cookies,
                                                                    final HttpMethod method,
                                                                    final HashMap<String, String[]> parameters,
                                                                    final String[] headers,
                                                                    final HashMap<String, Object> contextVariables) {
        final HttpRequestKnob requestKnob = new HttpRequestKnobAdapter() {
            @Override
            public HttpCookie[] getCookies() {
                HttpCookie[] httpCookies = new HttpCookie[cookies.size()];
                int i = 0;
                for(Map.Entry<String, String> entry : cookies.entrySet()) {
                    httpCookies[i++] = new HttpCookie(entry.getKey(), entry.getValue(), 1, "/", url.getHost());
                }

                return httpCookies;
            }

            @Override
            public HttpMethod getMethod() {
                return method;
            }

            @Override
            public String[] getParameterValues(String s) {
                return parameters.get(s);
            }

            @Override
            public String[] getHeaderValues(String name) {
                Pattern pattern = Pattern.compile("^" + name + ":\\s*(.*)");

                ArrayList<String> values = new ArrayList<String>();
                for(String header : headers) {
                    Matcher matcher = pattern.matcher(header);
                    if(matcher.matches()) {
                        values.add(new String(matcher.group(1).trim()));
                    }
                }

                return values.toArray(new String[values.size()]);
            }

            @Override
            public URL getRequestURL() {
                return url;
            }
        };

        return new PolicyEnforcementContextWrapper(null) {
            @Override
            public Message getRequest() {
                Message message = new Message();
                message.attachHttpRequestKnob(requestKnob);

                return message;
            }

            @Override
            public Object getVariable(String name) {
                return contextVariables.get(name);
            }

            @Override
            public void setVariable(String name, Object value) {
                contextVariables.put(name, value);
            }
        };
    }
}
