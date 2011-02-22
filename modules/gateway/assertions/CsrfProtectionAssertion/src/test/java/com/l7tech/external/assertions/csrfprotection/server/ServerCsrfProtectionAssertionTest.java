package com.l7tech.external.assertions.csrfprotection.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.csrfprotection.CsrfProtectionAssertion;
import com.l7tech.external.assertions.csrfprotection.HttpParameterType;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpRequestKnobAdapter;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextWrapper;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test the CsrfProtectionAssertion.
 */
public class ServerCsrfProtectionAssertionTest extends TestCase {

    private static final Logger log = Logger.getLogger(ServerCsrfProtectionAssertionTest.class.getName());
    private static ApplicationContext applicationContext;
    private static ServerConfigStub serverConfig;

    public ServerCsrfProtectionAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerCsrfProtectionAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testFailDoubleSubmit1() throws Exception {
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

        assertEquals(status, AssertionStatus.FAILED);
    }

    public void testFailDoubleSubmit2() throws Exception {
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

        assertEquals(status, AssertionStatus.FAILED);
    }

    public void testFailDoubleSubmit3() throws Exception {
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

        assertEquals(status, AssertionStatus.FAILED);
    }

    public void testFailDoubleSubmit4() throws Exception {
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

        assertEquals(status, AssertionStatus.FAILED);
    }

    public void testFailDoubleSubmit5() throws Exception {
        HashMap<String, String> cookies = new HashMap<String, String>();
        cookies.put("SESSION_ID", "1234567890ABCDEF");
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("sessionID", new String[] {"123", "1234567890ABCDEF"});

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

        assertEquals(status, AssertionStatus.FAILED);
    }

    public void testSucceedDoubleSubmit1() throws Exception {
        HashMap<String, String> cookies = new HashMap<String, String>();
        cookies.put("SESSION_ID", "1234567890ABCDEF");
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("sessionID", new String[] {"1234567890ABCDEF"});

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

        assertEquals(status, AssertionStatus.NONE);
    }

    public void testSucceedDoubleSubmit2() throws Exception {
        HashMap<String, String> cookies = new HashMap<String, String>();
        cookies.put("SESSION_ID", "1234567890ABCDEF");
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("sessionID", new String[] {"1234567890ABCDEF"});

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

        assertEquals(status, AssertionStatus.NONE);
    }

    public void testSucceedDoubleSubmit3() throws Exception {
        HashMap<String, String> cookies = new HashMap<String, String>();
        cookies.put("SESSION_ID", "1234567890ABCDEF");
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("sessionID", new String[] {"1234567890ABCDEF"});

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

        assertEquals(status, AssertionStatus.NONE);
    }

    public void testFailRefererCheck1() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowEmptyReferer(false);
        assertion.setOnlyAllowCurrentDomain(true);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(status, AssertionStatus.FAILED);
    }

    public void testFailRefererCheck2() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowEmptyReferer(false);
        assertion.setOnlyAllowCurrentDomain(false);
        ArrayList<String> trustedDomains = new ArrayList<String>();
        trustedDomains.add("layer7tech.com");
        assertion.setTrustedDomains(trustedDomains);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(status, AssertionStatus.FAILED);
    }

    public void testFailRefererCheck3() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[] {"http://www.layer7tech.com/page.html"});

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowEmptyReferer(false);
        assertion.setOnlyAllowCurrentDomain(true);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(status, AssertionStatus.FAILED);
    }

    public void testFailRefererCheck4() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[] {"http://www.layer7tech.com/page.html"});

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowEmptyReferer(false);
        assertion.setOnlyAllowCurrentDomain(false);
        ArrayList<String> trustedDomains = new ArrayList<String>();
        trustedDomains.add("l7tech.com");
        assertion.setTrustedDomains(trustedDomains);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(status, AssertionStatus.FAILED);
    }

    public void testFailRefererCheck5() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[] {"/page.html"});

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowEmptyReferer(false);
        assertion.setOnlyAllowCurrentDomain(false);
        ArrayList<String> trustedDomains = new ArrayList<String>();
        trustedDomains.add("l7tech.com");
        assertion.setTrustedDomains(trustedDomains);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(status, AssertionStatus.FAILED);
    }

    public void testSucceedRefererCheck1() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowEmptyReferer(true);
        assertion.setOnlyAllowCurrentDomain(true);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(status, AssertionStatus.NONE);
    }

    public void testSucceedRefererCheck2() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[0]);

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowEmptyReferer(true);
        assertion.setOnlyAllowCurrentDomain(false);
        ArrayList<String> trustedDomains = new ArrayList<String>();
        trustedDomains.add("layer7tech.com");
        assertion.setTrustedDomains(trustedDomains);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(status, AssertionStatus.NONE);
    }

    public void testSucceedRefererCheck3() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[] {"Referer: /page.html"});

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowEmptyReferer(true);
        assertion.setOnlyAllowCurrentDomain(true);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(status, AssertionStatus.NONE);
    }

    public void testSucceedRefererCheck4() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext(
                new URL("http://localhost:8080/test"),
                new HashMap<String, String>(),
                HttpMethod.POST,
                new HashMap<String, String[]>(),
                new String[] {"http://www.layer7tech.com/page.html"});

        CsrfProtectionAssertion assertion = new CsrfProtectionAssertion();
        assertion.setEnableDoubleSubmitCookieChecking(false);
        assertion.setEnableHttpRefererChecking(true);
        assertion.setAllowEmptyReferer(true);
        assertion.setOnlyAllowCurrentDomain(false);
        ArrayList<String> trustedDomains = new ArrayList<String>();
        trustedDomains.add("layer7tech.com");
        assertion.setTrustedDomains(trustedDomains);

        ServerCsrfProtectionAssertion serverAssertion = new ServerCsrfProtectionAssertion(assertion, null);
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(status, AssertionStatus.NONE);
    }

    private PolicyEnforcementContext createPolicyEnforcementContext(final URL url,
                                                                    final HashMap<String, String> cookies,
                                                                    final HttpMethod method,
                                                                    final HashMap<String, String[]> parameters,
                                                                    final String[] headers)
    {
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
                        values.add(matcher.group(1).trim());
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
        };
    }

}
