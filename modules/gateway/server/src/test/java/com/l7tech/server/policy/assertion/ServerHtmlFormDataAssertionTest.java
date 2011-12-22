package com.l7tech.server.policy.assertion;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HtmlFormDataAssertion;
import com.l7tech.policy.assertion.HtmlFormDataLocation;
import com.l7tech.policy.assertion.HtmlFormDataType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.File;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerHtmlFormDataAssertionTest {
    private static final String FIELDNAME = "somestring";
    private static final String FIELDVALUE = "somevalue";
    private static final String KEYVALUEPAIR = FIELDNAME + "=" + FIELDVALUE;
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String FORM = "application/x-www-form-urlencoded";
    private static final HtmlFormDataAssertion.FieldSpec STRING_ANYWHERE_FIELD_SPEC = new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.STRING, 1, 1, HtmlFormDataLocation.ANYWHERE);
    private static final HtmlFormDataAssertion.FieldSpec STRING_BODY_FIELD_SPEC = new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.STRING, 1, 1, HtmlFormDataLocation.BODY);
    private static final HtmlFormDataAssertion.FieldSpec STRING_URL_FIELD_SPEC = new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.STRING, 1, 1, HtmlFormDataLocation.URL);
    private ServerHtmlFormDataAssertion serverAssertion;
    private HtmlFormDataAssertion assertion;
    private PolicyEnforcementContext context;
    private Message request;
    private MockHttpServletRequest mockRequest;
    @Mock
    private File file;


    @Before
    public void setup() {
        assertion = new HtmlFormDataAssertion();
        serverAssertion = new ServerHtmlFormDataAssertion(assertion);
        request = new Message();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        mockRequest = new MockHttpServletRequest();
    }

    @Test
    public void checkGetRequestAnywhereStringDataType() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(KEYVALUEPAIR);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkGetRequestUrlStringDataType() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_URL_FIELD_SPEC});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(KEYVALUEPAIR);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
    }

    /**
     * GET requests should not have bodies.
     */
    @Test
    public void checkGetRequestBodyStringDataType() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_BODY_FIELD_SPEC});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setContent(KEYVALUEPAIR.getBytes());
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkGetRequestStringDataTypeNotFound() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString("foo=bar");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkRequestStringDataTypeEmpty() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(FIELDNAME + "=");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkPostRequestAnywhereStringDataType() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC});
        assertion.setAllowPost(true);
        mockRequest.setMethod(POST);
        mockRequest.addHeader(CONTENT_TYPE, FORM);
        mockRequest.setContent(KEYVALUEPAIR.getBytes());
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));
        request.initialize(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED, KEYVALUEPAIR.getBytes());

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkPostRequestBodyStringDataType() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_BODY_FIELD_SPEC});
        assertion.setAllowPost(true);
        mockRequest.setMethod(POST);
        mockRequest.addHeader(CONTENT_TYPE, FORM);
        mockRequest.setContent(KEYVALUEPAIR.getBytes());
        final HttpRequestKnob knob = new HttpServletRequestKnob(mockRequest);
        request.attachHttpRequestKnob(knob);
        request.initialize(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED, KEYVALUEPAIR.getBytes());

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkPostRequestUrlStringDataType() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_URL_FIELD_SPEC});
        assertion.setAllowPost(true);
        mockRequest.setMethod(POST);
        mockRequest.addHeader(CONTENT_TYPE, FORM);
        mockRequest.setQueryString(KEYVALUEPAIR);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));
        request.initialize(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED, KEYVALUEPAIR.getBytes());

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkPostRequestStringDataTypeNotFound() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC});
        assertion.setAllowPost(true);
        mockRequest.setMethod(POST);
        mockRequest.addHeader(CONTENT_TYPE, FORM);
        mockRequest.setQueryString("foo=bar");
        mockRequest.setContent("foo=bar".getBytes());
        final HttpRequestKnob knob = new HttpServletRequestKnob(mockRequest);
        request.attachHttpRequestKnob(knob);
        request.initialize(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED, "foo=bar".getBytes());

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkPostRequestStringDataTypeFileFound() throws Exception {
        final Part[] parts = new Part []{new FilePart("part", new ByteArrayPartSource(FIELDNAME, "dummyfilecontent".getBytes()))};
        final MultipartRequestEntity entity = new MultipartRequestEntity(parts, new PostMethod().getParams());
        final ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
	    entity.writeRequest(requestContent);

        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC});
        assertion.setAllowPost(true);
        mockRequest.setMethod(POST);
        mockRequest.addHeader(CONTENT_TYPE, entity.getContentType());
        mockRequest.setContent(requestContent.toByteArray());
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));
        request.initialize(ContentTypeHeader.parseValue(entity.getContentType()), requestContent.toByteArray());

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
    }
}
