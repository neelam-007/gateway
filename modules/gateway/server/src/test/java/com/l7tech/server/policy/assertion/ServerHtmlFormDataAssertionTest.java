package com.l7tech.server.policy.assertion;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HtmlFormDataAssertion;
import com.l7tech.policy.assertion.HtmlFormDataLocation;
import com.l7tech.policy.assertion.HtmlFormDataType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.util.Collections;

import static org.junit.Assert.*;

public class ServerHtmlFormDataAssertionTest {
    private static final String FIELDNAME = "somestring";
    private static final String FIELDVALUE = "somevalue";
    private static final String KEYVALUEPAIR = FIELDNAME + "=" + FIELDVALUE;
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String FORM = "application/x-www-form-urlencoded";
    private static final String FILENAME = "filename";
    private static final HtmlFormDataAssertion.FieldSpec STRING_ANYWHERE_FIELD_SPEC = new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.STRING, 1, 1, HtmlFormDataLocation.ANYWHERE, Boolean.TRUE);
    private static final HtmlFormDataAssertion.FieldSpec STRING_ANYWHERE_FIELD_SPEC_NO_URI = new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.STRING, 1, 1, HtmlFormDataLocation.BODY, Boolean.TRUE);
    private static final HtmlFormDataAssertion.FieldSpec STRING_ANYWHERE_FIELD_SPEC_MIN_AND_MAX_GT_1 = new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.STRING, 2, 3, HtmlFormDataLocation.ANYWHERE, Boolean.TRUE);
    private static final HtmlFormDataAssertion.FieldSpec STRING_BODY_FIELD_SPEC = new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.STRING, 1, 1, HtmlFormDataLocation.BODY, Boolean.TRUE);
    private static final HtmlFormDataAssertion.FieldSpec STRING_URL_FIELD_SPEC = new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.STRING, 1, 1, HtmlFormDataLocation.URL, Boolean.TRUE);
    private ServerHtmlFormDataAssertion serverAssertion;
    private HtmlFormDataAssertion assertion;
    private PolicyEnforcementContext context;
    private Message request;
    private MockHttpServletRequest mockRequest;
    private TestAudit testAudit;

    @Before
    public void setup() {
        assertion = new HtmlFormDataAssertion();
        serverAssertion = new ServerHtmlFormDataAssertion(assertion);
        request = new Message();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        mockRequest = new MockHttpServletRequest();
        testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));
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
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTMLFORMDATA_FIELD_NOT_FOUND.getMessage(), FIELDNAME)));
    }

    @Test
    public void checkGetRequestStringDataTypeNotFound() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString("foo=bar");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTMLFORMDATA_FIELD_NOT_FOUND.getMessage(), FIELDNAME)));
    }

    @Test
    public void checkGetRequestStringDataTypeEmpty() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(FIELDNAME + "=");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkGetRequestStringDataTypeEmptyNotAllowed() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.STRING, 1, 1, HtmlFormDataLocation.ANYWHERE, Boolean.FALSE)});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(FIELDNAME + "=");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkGetRequestStringDataTypeEmptyNullAllowEmpty() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.STRING, 1, 1, HtmlFormDataLocation.ANYWHERE, null)});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(FIELDNAME + "=");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkGetRequestNumberDataTypeEmpty() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.NUMBER, 1, 1, HtmlFormDataLocation.ANYWHERE, Boolean.TRUE)});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(FIELDNAME + "=");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkGetRequestNumberDataTypeEmptyNotAllowed() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.NUMBER, 1, 1, HtmlFormDataLocation.ANYWHERE, Boolean.FALSE)});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(FIELDNAME + "=");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTMLFORMDATA_EMPTY_NOT_ALLOWED.getMessage(), FIELDNAME)));
    }

    @Test
    public void checkGetRequestNumberDataTypeEmptyNullAllowEmpty() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.NUMBER, 1, 1, HtmlFormDataLocation.ANYWHERE, null)});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(FIELDNAME + "=");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTMLFORMDATA_EMPTY_NOT_ALLOWED.getMessage(), FIELDNAME)));
    }

    @Test
    public void checkGetRequestAnywhereAnyDataType() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.ANY, 1, 1, HtmlFormDataLocation.ANYWHERE, Boolean.TRUE)});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(KEYVALUEPAIR);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkGetRequestAnywhereAnyDataTypeEmpty() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.ANY, 1, 1, HtmlFormDataLocation.ANYWHERE, Boolean.TRUE)});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(FIELDNAME + "=");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkGetRequestAnywhereAnyDataTypeEmptyNotAllowed() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.ANY, 1, 1, HtmlFormDataLocation.ANYWHERE, Boolean.FALSE)});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(FIELDNAME + "=");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTMLFORMDATA_EMPTY_NOT_ALLOWED.getMessage(), FIELDNAME)));
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
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTMLFORMDATA_FIELD_NOT_FOUND.getMessage(), FIELDNAME)));
    }

    @Test
    public void checkPostRequestStringDataTypeFileFound() throws Exception {
        final Part[] parts = new Part[]{new FilePart(FIELDNAME, new ByteArrayPartSource(FILENAME, "dummyfilecontent".getBytes()))};
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
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTMLFORMDATA_FAIL_DATATYPE.getMessage(), FIELDNAME, FILENAME, HtmlFormDataType.STRING.getWspName())));
    }

    @Test
    public void checkPostRequestFileDataTypeEmptyNotAllowed() throws Exception {
        final Part[] parts = new Part[]{new FilePart(FIELDNAME, new ByteArrayPartSource("", "".getBytes()))};
        final MultipartRequestEntity entity = new MultipartRequestEntity(parts, new PostMethod().getParams());
        final ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
        entity.writeRequest(requestContent);

        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.FILE, 1, 1, HtmlFormDataLocation.ANYWHERE, Boolean.FALSE)});
        assertion.setAllowPost(true);
        mockRequest.setMethod(POST);
        mockRequest.addHeader(CONTENT_TYPE, entity.getContentType());
        mockRequest.setContent(requestContent.toByteArray());
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));
        request.initialize(ContentTypeHeader.parseValue(entity.getContentType()), requestContent.toByteArray());

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTMLFORMDATA_EMPTY_NOT_ALLOWED.getMessage(), FIELDNAME)));
    }

    @Test
    public void checkPostRequestFileDataTypeEmpty() throws Exception {
        final Part[] parts = new Part[]{new FilePart(FIELDNAME, new ByteArrayPartSource("", "".getBytes()))};
        final MultipartRequestEntity entity = new MultipartRequestEntity(parts, new PostMethod().getParams());
        final ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
        entity.writeRequest(requestContent);

        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.FILE, 1, 1, HtmlFormDataLocation.ANYWHERE, Boolean.TRUE)});
        assertion.setAllowPost(true);
        mockRequest.setMethod(POST);
        mockRequest.addHeader(CONTENT_TYPE, entity.getContentType());
        mockRequest.setContent(requestContent.toByteArray());
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));
        request.initialize(ContentTypeHeader.parseValue(entity.getContentType()), requestContent.toByteArray());

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkPostRequestFileDataTypeEmptyNullAllowEmpty() throws Exception {
        final Part[] parts = new Part[]{new FilePart(FIELDNAME, new ByteArrayPartSource("", "".getBytes()))};
        final MultipartRequestEntity entity = new MultipartRequestEntity(parts, new PostMethod().getParams());
        final ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
        entity.writeRequest(requestContent);

        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{new HtmlFormDataAssertion.FieldSpec(FIELDNAME, HtmlFormDataType.FILE, 1, 1, HtmlFormDataLocation.ANYWHERE, null)});
        assertion.setAllowPost(true);
        mockRequest.setMethod(POST);
        mockRequest.addHeader(CONTENT_TYPE, entity.getContentType());
        mockRequest.setContent(requestContent.toByteArray());
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));
        request.initialize(ContentTypeHeader.parseValue(entity.getContentType()), requestContent.toByteArray());

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
    }

    @Test
    public void validateAudit_6852() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC});
        assertion.setAllowGet(false);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(KEYVALUEPAIR);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HTMLFORMDATA_METHOD_NOT_ALLOWED));
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTMLFORMDATA_METHOD_NOT_ALLOWED.getMessage(), "GET")));
    }

    @Test
    public void validateAudit_6853() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC});
        assertion.setAllowGet(true);
        assertion.setDisallowOtherFields(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(KEYVALUEPAIR+"&two=2");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HTMLFORMDATA_UNKNOWN_FIELD_NOT_ALLOWED));
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTMLFORMDATA_UNKNOWN_FIELD_NOT_ALLOWED.getMessage(), "two")));
    }

    @Test
    public void validateAudit_6856() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC_MIN_AND_MAX_GT_1});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(KEYVALUEPAIR);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HTMLFORMDATA_FAIL_MINOCCURS));
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTMLFORMDATA_FAIL_MINOCCURS.getMessage(), FIELDNAME, "1", "2")));
    }

    @Test
    public void validateAudit_6857() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC_MIN_AND_MAX_GT_1});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(KEYVALUEPAIR + "&" + KEYVALUEPAIR + "&" + KEYVALUEPAIR + "&" + KEYVALUEPAIR);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HTMLFORMDATA_FAIL_MAXOCCURS));
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTMLFORMDATA_FAIL_MAXOCCURS.getMessage(), FIELDNAME, "4", "3")));
    }

    @Test
    public void validateAudit_6858() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC_NO_URI});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(KEYVALUEPAIR);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HTMLFORMDATA_LOCATION_NOT_ALLOWED));
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTMLFORMDATA_LOCATION_NOT_ALLOWED.getMessage(), FIELDNAME, HtmlFormDataLocation.URL.getDisplayName())));
    }

    @Test
    public void validateAudit_6850() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(KEYVALUEPAIR);

        assertEquals(AssertionStatus.NOT_APPLICABLE, serverAssertion.checkRequest(context));
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HTMLFORMDATA_NOT_HTTP));
    }

    @Test
    public void validateAudit_6851() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC});
        assertion.setAllowGet(true);
        mockRequest.setMethod(POST);
        mockRequest.setQueryString(KEYVALUEPAIR);

        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.NOT_APPLICABLE, serverAssertion.checkRequest(context));
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HTTP_POST_NOT_FORM_DATA));
        // content type originates at the mime knob, as it's not set it will have it's default value of application/octet-stream
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTTP_POST_NOT_FORM_DATA.getMessage(), "application/octet-stream")));
    }

    @Test
    public void validateAudit_6854() throws Exception {
        assertion.setFieldSpecs(new HtmlFormDataAssertion.FieldSpec[]{STRING_ANYWHERE_FIELD_SPEC});
        assertion.setAllowGet(true);
        mockRequest.setMethod(GET);
        mockRequest.setQueryString(KEYVALUEPAIR+"&notspecified=allowedthrough");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresent(AssertionMessages.HTMLFORMDATA_UNKNOWN_FIELD_ALLOWED));
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.HTMLFORMDATA_UNKNOWN_FIELD_ALLOWED.getMessage(), "notspecified")));

    }

}
