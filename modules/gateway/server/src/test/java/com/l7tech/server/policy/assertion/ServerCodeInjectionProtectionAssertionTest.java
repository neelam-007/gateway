/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.json.JSONData;
import com.l7tech.json.JSONFactory;
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

/**
 * Basic test coverage for JSON. //todo tests for other content-types are needed
 */
public class ServerCodeInjectionProtectionAssertionTest {
    private ApplicationContext appContext;
    private StashManagerFactory factory;
    private StashManager stashManager;

    @Before
    public void setUp(){
        appContext = ApplicationContexts.getTestApplicationContext();
        factory = (StashManagerFactory) appContext.getBean("stashManagerFactory");
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
                new ServerCodeInjectionProtectionAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext(null);
        context.setVariable(varName, new Message(stashManager, ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(noInvalidCharacters.getBytes()),0));

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
                new ServerCodeInjectionProtectionAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext(null);
        context.setVariable(varName, new Message(stashManager, ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(invalidCharsInKeyValue.getBytes()),0));

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
                new ServerCodeInjectionProtectionAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext(null);
        context.setVariable(varName, new Message(stashManager, ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(invalidCharactersInValue.getBytes()),0));

        AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be FALSIFIED", AssertionStatus.FALSIFIED, status);

        context.setVariable(varName, new Message(stashManager, ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(invalidCharactersInValueInArray.getBytes()),0));

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
                new ServerCodeInjectionProtectionAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext(invalidCharactersInValue);

        AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be BAD_REQUEST", AssertionStatus.BAD_REQUEST, status);
    }

    // - PRIVATE

    private PolicyEnforcementContext getContext(String requestBody) throws IOException {

        Message request = new Message();
        Message response = new Message();

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setMethod(HttpMethod.POST.name());
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        if(requestBody != null){
            request.initialize(stashManager, ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(requestBody.getBytes(Charsets.UTF8)),0);    
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
