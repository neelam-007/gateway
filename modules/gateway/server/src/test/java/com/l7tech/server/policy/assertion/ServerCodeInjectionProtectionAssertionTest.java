package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.CodeInjectionProtectionAssertion;
import com.l7tech.policy.assertion.CodeInjectionProtectionType;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Basic test coverage for JSON and code injections apart from HTML.
 *
 * //todo complete test coverage
 * @author darmstrong
 */
public class ServerCodeInjectionProtectionAssertionTest {
    private StashManager stashManager;

    @Before
    public void setUp() {
        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        StashManagerFactory factory = (StashManagerFactory) appContext.getBean("stashManagerFactory");
        stashManager = factory.createStashManager();
    }

    /**
     * Tests the normal case - no invalid characters
     * @throws Exception
     */
    @BugNumber(8971)
    @Test
    public void testJsonCodeInjection() throws Exception{
        CodeInjectionProtectionAssertion assertion = new CodeInjectionProtectionAssertion();
        final String varName = "jsonInput";
        assertion.setOtherTargetMessageVariable(varName);
        assertion.setProtections(new CodeInjectionProtectionType[]{
                CodeInjectionProtectionType.PHP_EVAL_INJECTION,
                CodeInjectionProtectionType.HTML_JAVASCRIPT,
                CodeInjectionProtectionType.SHELL_INJECTION});
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setIncludeRequestBody(true);//required otherwise message body will not be scanned

        ServerCodeInjectionProtectionAssertion serverAssertion =
                new ServerCodeInjectionProtectionAssertion(assertion);

        final PolicyEnforcementContext context = getContext(null, ContentTypeHeader.APPLICATION_JSON);
        context.setVariable(varName, new Message(stashManager, ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(noInvalidCharacters.getBytes())));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);
    }

    /**
     * Tests invalid characters in a key. Invalid character is ;
     * @throws Exception
     */
    @BugNumber(8971)
    @Test
    public void testJsonCodeInjection_InvalidKey() throws Exception{
        CodeInjectionProtectionAssertion assertion = new CodeInjectionProtectionAssertion();
        final String varName = "jsonInput";
        assertion.setOtherTargetMessageVariable(varName);
        assertion.setProtections(new CodeInjectionProtectionType[]{
                CodeInjectionProtectionType.PHP_EVAL_INJECTION,
                CodeInjectionProtectionType.HTML_JAVASCRIPT,
                CodeInjectionProtectionType.SHELL_INJECTION});
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setIncludeRequestBody(true);//required otherwise message body will not be scanned

        ServerCodeInjectionProtectionAssertion serverAssertion =
                new ServerCodeInjectionProtectionAssertion(assertion);

        final PolicyEnforcementContext context = getContext(null, ContentTypeHeader.APPLICATION_JSON);
        context.setVariable(varName, new Message(stashManager, ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(invalidCharsInKeyValue.getBytes())));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be FALSIFIED", AssertionStatus.FALSIFIED, status);
    }

    /**
     * Tests invalid characters in a value. Invalid character is '
     * @throws Exception
     */
    @BugNumber(8971)
    @Test
    public void testJsonCodeInjection_InvalidValue() throws Exception{
        CodeInjectionProtectionAssertion assertion = new CodeInjectionProtectionAssertion();
        final String varName = "jsonInput";
        assertion.setOtherTargetMessageVariable(varName);
        assertion.setProtections(new CodeInjectionProtectionType[]{
                CodeInjectionProtectionType.PHP_EVAL_INJECTION,
                CodeInjectionProtectionType.HTML_JAVASCRIPT,
                CodeInjectionProtectionType.SHELL_INJECTION});
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setIncludeRequestBody(true);//required otherwise message body will not be scanned

        ServerCodeInjectionProtectionAssertion serverAssertion =
                new ServerCodeInjectionProtectionAssertion(assertion);

        final PolicyEnforcementContext context = getContext(null, ContentTypeHeader.APPLICATION_JSON);
        context.setVariable(varName, new Message(stashManager, ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(invalidCharactersInValue.getBytes())));

        AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be FALSIFIED", AssertionStatus.FALSIFIED, status);

        context.setVariable(varName, new Message(stashManager, ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(invalidCharactersInValueInArray.getBytes())));

        status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be FALSIFIED", AssertionStatus.FALSIFIED, status);
    }

    /**
     * Tests that the correct status is returned when the assertion is configured to use the request.
     * @throws Exception
     */
    @BugNumber(8971)
    @Test
    public void testJsonCodeInjection_InvalidValueInRequest() throws Exception{
        CodeInjectionProtectionAssertion assertion = new CodeInjectionProtectionAssertion();
        assertion.setProtections(new CodeInjectionProtectionType[]{
                CodeInjectionProtectionType.PHP_EVAL_INJECTION,
                CodeInjectionProtectionType.HTML_JAVASCRIPT,
                CodeInjectionProtectionType.SHELL_INJECTION});
        assertion.setIncludeRequestBody(true);//required otherwise message body will not be scanned

        ServerCodeInjectionProtectionAssertion serverAssertion =
                new ServerCodeInjectionProtectionAssertion(assertion);

        final PolicyEnforcementContext context = getContext(invalidCharactersInValue, ContentTypeHeader.APPLICATION_JSON);

        AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be BAD_REQUEST", AssertionStatus.BAD_REQUEST, status);
    }

    @BugNumber(10008)
    @Test
    public void testLdapCodeInjection_Backslash() throws Exception{
        CodeInjectionProtectionAssertion assertion = new CodeInjectionProtectionAssertion();
        assertion.setProtections(new CodeInjectionProtectionType[]{
                CodeInjectionProtectionType.LDAP_DN_INJECTION});
        assertion.setIncludeRequestBody(true);

        ServerCodeInjectionProtectionAssertion serverAssertion =
                new ServerCodeInjectionProtectionAssertion(assertion);

        final PolicyEnforcementContext context = getContext("\\", ContentTypeHeader.TEXT_DEFAULT);

        AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Incorrect status returned", AssertionStatus.FALSIFIED, status);
    }

    @BugNumber(10008)
    @Test
    public void testLdapSearchInjection_Backslash() throws Exception{
        CodeInjectionProtectionAssertion assertion = new CodeInjectionProtectionAssertion();
        assertion.setProtections(new CodeInjectionProtectionType[]{
                CodeInjectionProtectionType.LDAP_SEARCH_INJECTION});
        assertion.setIncludeRequestBody(true);

        ServerCodeInjectionProtectionAssertion serverAssertion =
                new ServerCodeInjectionProtectionAssertion(assertion);

        final PolicyEnforcementContext context = getContext("\\", ContentTypeHeader.TEXT_DEFAULT);

        AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Incorrect status returned", AssertionStatus.FALSIFIED, status);
    }

    @BugNumber(10011)
    @Test
    public void testXpathInjection_SemiColon() throws Exception{
        CodeInjectionProtectionAssertion assertion = new CodeInjectionProtectionAssertion();
        assertion.setProtections(new CodeInjectionProtectionType[]{
                CodeInjectionProtectionType.XPATH_INJECTION});
        assertion.setIncludeRequestBody(true);

        ServerCodeInjectionProtectionAssertion serverAssertion =
                new ServerCodeInjectionProtectionAssertion(assertion);

        final PolicyEnforcementContext context = getContext(";", ContentTypeHeader.TEXT_DEFAULT);

        AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Incorrect status returned", AssertionStatus.FALSIFIED, status);
    }

    /**
     * Test all supported code injections, apart from HTML, for it's range of illegal characters against the request body for a text
     * document. Expected outcome for each illegal character is assertion falsified.
     */
    @Test
    public void testCodeProtections_ApartFromHTML() throws Exception{
        final CodeInjectionProtectionType[] protectionTypes = CodeInjectionProtectionType.values();
        for (CodeInjectionProtectionType protectionType : protectionTypes) {

            if(protectionType == CodeInjectionProtectionType.HTML_JAVASCRIPT) continue; //not as simple as going through its chars

            System.out.println(protectionType.getDisplayName());

            CodeInjectionProtectionAssertion assertion = new CodeInjectionProtectionAssertion();
            assertion.setProtections(new CodeInjectionProtectionType[]{protectionType});
            assertion.setIncludeRequestBody(true);

            ServerCodeInjectionProtectionAssertion serverAssertion =
                    new ServerCodeInjectionProtectionAssertion(assertion);

            final Pattern pattern = protectionType.getPattern();
            System.out.println(protectionType.getDescription());
            final String str = pattern.toString();
            final String patternString = str.substring(1, str.length() - 1);//ignore the opening and closing square brackets
            for(int i = 0; i < patternString.length(); i++){

                final String illegalString = new String(new char[]{patternString.charAt(i)});
                final PolicyEnforcementContext context = getContext(illegalString, ContentTypeHeader.TEXT_DEFAULT);

                AssertionStatus status = serverAssertion.checkRequest(context);
                Assert.assertEquals("Incorrect status returned", AssertionStatus.FALSIFIED, status);
                context.close();//close to ensure the stash manager is closed, otherwise it will continue to initialize the request with the same content!
            }
        }
    }

    // - PRIVATE

    private PolicyEnforcementContext getContext(String requestBody, ContentTypeHeader contentTypeHeader) throws IOException {

        Message request = new Message();
        Message response = new Message();

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setMethod(HttpMethod.POST.name());
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        if(requestBody != null){
            request.initialize(stashManager, contentTypeHeader, new ByteArrayInputStream(requestBody.getBytes(Charsets.UTF8)));
        }

        PolicyEnforcementContext policyEnforcementContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));

        return policyEnforcementContext;
    }

    private static String noInvalidCharacters = "{\n" +
            "   \"type\":\"object\",\n" +
            "   \"properties\": {\n" +
            "\t\t\"reportType\": {\"type\":\"string\"},\n" +
            "\t\t\"entityType\": {\"type\":\"string\"},\n" +
            "\t\t\"isIgnoringPagination\": {\"type\":\"boolean\"},\n" +
            "\t\t\"entities\": {\n" +
            "\t\t\t\"type\":\"array\",\n" +
            "\t\t\t\"items\": {\n" +
            "\t\t\t\t\"type\":\"object\",\n" +
            "\t\t\t\t\"properties\":{\"clusterId\":{\"type\":\"string\"}},\n" +
            "\t\t\t\t\"additionalProperties\":false\n" +
            "\t\t\t}\n" +
            "\t\t},\n" +
            "\t\t\"summaryChart\": {\"type\":\"boolean\"},\n" +
            "\t\t\"summaryReport\": {\"type\":\"boolean\"},\n" +
            "\t\t\"reportName\": {\"type\":\"string\"}\n" +
            "\t},\n" +
            "\t\"additionalProperties\":false\n" +
            "}";

    private static String invalidCharsInKeyValue = "{\n" +
            "   \"type\":\"object\",\n" +
            "   \"properties\": {\n" +
            "\t\t\"reportType\": {\"type\":\"string\"},\n" +
            "\t\t\"entit;yType\": {\"type\":\"string\"},\n" +
            "\t\t\"isIgnoringPagination\": {\"type\":\"boolean\"},\n" +
            "\t\t\"entities\": {\n" +
            "\t\t\t\"type\":\"array\",\n" +
            "\t\t\t\"items\": {\n" +
            "\t\t\t\t\"type\":\"object\",\n" +
            "\t\t\t\t\"properties\":{\"clusterId\":{\"type\":\"string\"}},\n" +
            "\t\t\t\t\"additionalProperties\":false\n" +
            "\t\t\t}\n" +
            "\t\t},\n" +
            "\t\t\"summaryChart\": {\"type\":\"boolean\"},\n" +
            "\t\t\"summaryReport\": {\"type\":\"boolean\"},\n" +
            "\t\t\"reportName\": {\"type\":\"string\"}\n" +
            "\t},\n" +
            "\t\"additionalProperties\":false\n" +
            "}";

    private static String invalidCharactersInValue = "{\n" +
            "   \"type\":\"object\",\n" +
            "   \"properties\": {\n" +
            "\t\t\"reportType\": {\"type\":\"string\"},\n" +
            "\t\t\"entityType\": {\"type\":\"string\"},\n" +
            "\t\t\"isIgnoringPagination\": {\"type\":\"boolean\"},\n" +
            "\t\t\"entities\": {\n" +
            "\t\t\t\"type\":\"array\",\n" +
            "\t\t\t\"items\": {\n" +
            "\t\t\t\t\"type\":\"object\",\n" +
            "\t\t\t\t\"properties\":{\"clusterId\":{\"type\":\"string\"}},\n" +
            "\t\t\t\t\"additionalProperties\":false\n" +
            "\t\t\t}\n" +
            "\t\t},\n" +
            "\t\t\"summaryChart\": {\"type\":\"boolean\"},\n" +
            "\t\t\"summaryReport\": {\"type\":\"b'oolean\"},\n" +
            "\t\t\"reportName\": {\"type\":\"string\"}\n" +
            "\t},\n" +
            "\t\"additionalProperties\":false\n" +
            "}";

    private static String invalidCharactersInValueInArray = "{\n" +
            "   \"type\":\"object\",\n" +
            "   \"properties\": {\n" +
            "\t\t\"reportType\": {\"type\":\"string\"},\n" +
            "\t\t\"entityType\": {\"type\":\"string\"},\n" +
            "\t\t\"isIgnoringPagination\": {\"type\":\"boolean\"},\n" +
            "\t\t\"entities\": {\n" +
            "\t\t\t\"type\":\"array\",\n" +
            "\t\t\t\"items\": {\n" +
            "\t\t\t\t\"type\":\"object\",\n" +
            "\t\t\t\t\"properties\":{\"clusterId\":{\"type\":\"st'ring\"}},\n" +
            "\t\t\t\t\"additionalProperties\":false\n" +
            "\t\t\t}\n" +
            "\t\t},\n" +
            "\t\t\"summaryChart\": {\"type\":\"boolean\"},\n" +
            "\t\t\"summaryReport\": {\"type\":\"b'oolean\"},\n" +
            "\t\t\"reportName\": {\"type\":\"string\"}\n" +
            "\t},\n" +
            "\t\"additionalProperties\":false\n" +
            "}";

}
