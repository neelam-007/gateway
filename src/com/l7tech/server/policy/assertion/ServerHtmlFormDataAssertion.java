/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.HttpServletRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.mime.*;
import com.l7tech.common.util.HexUtils;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ParseException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enforces the HTML Form Data Assertion.
 *
 * @author rmak
 * @since SecureSpan 3.7
 * @see <a href="http://www.w3.org/TR/html4/interact/forms.html#h-17.13">HTML 4.0 Form Submisssion specification</a>
 */
public class ServerHtmlFormDataAssertion extends AbstractServerAssertion<HtmlFormDataAssertion>
        implements ServerAssertion {

    /** An HTML Form field value. */
    private static class FieldValue {
        /** String value of the field; or file name if {@link #isFile} is true. */
        public final String value;
        public final boolean isFile;
        public FieldValue(final String value, final boolean isFile) {
            this.value = value;
            this.isFile = isFile;
        }
    }

    /** An HTML Form field. */
    private static class Field {
        public final String name;
        /** An HTML Form can submit multiple values with the same field name. */
        public final List<FieldValue> fieldValues = new ArrayList<FieldValue>();
        /** Does field has any value that exists in request message URI? */
        public boolean inUri;
        /** Does field has any value that exists in request message body? */
        public boolean inBody;
        public Field(final String name) {
            this.name = name;
        }
    }

    private static final Logger _logger = Logger.getLogger(ServerHtmlFormDataAssertion.class.getName());
    private final HtmlFormDataAssertion _assertion;
    private final Auditor _auditor;

    public ServerHtmlFormDataAssertion(final HtmlFormDataAssertion assertion, final ApplicationContext springContext) {
        super(assertion);
        _assertion = assertion;
        _auditor = new Auditor(this, springContext, _logger);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message requestMessage = context.getRequest();
        final MimeKnob mimeKnob = requestMessage.getMimeKnob();

        // Skips if not HTTP.
        final HttpRequestKnob httpRequestKnob = (HttpRequestKnob) requestMessage.getKnob(HttpRequestKnob.class);
        if (httpRequestKnob == null) {
            _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_NOT_HTTP);
            return AssertionStatus.NOT_APPLICABLE;
        }

        // Skips if HTTP POST's content type is not for HTML Form data.
        final String httpMethod = httpRequestKnob.getMethod();
        final ContentTypeHeader outerContentType = mimeKnob.getOuterContentType();
        if (httpMethod.equalsIgnoreCase(GenericHttpClient.METHOD_POST)) {
            if (!(   outerContentType.matches("application", "x-www-form-urlencoded")
                  || outerContentType.matches("multipart", "form-data"))) {
                _auditor.logAndAudit(AssertionMessages.HTTP_POST_NOT_FORM_DATA,
                        new String[]{outerContentType.getMainValue()});
                return AssertionStatus.NOT_APPLICABLE;
            }
        }

        // Enforces HTTP method(s) allowed:
        if (   httpMethod.equalsIgnoreCase(GenericHttpClient.METHOD_GET)  && _assertion.isAllowGet()
            || httpMethod.equalsIgnoreCase(GenericHttpClient.METHOD_POST) && _assertion.isAllowPost()) {
            if (_logger.isLoggable(Level.FINER)) {
                _logger.finer("Verified: HTTP request method (" + httpMethod + ")");
            }
        } else {
            _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_METHOD_NOT_ALLOWED, new String[]{httpMethod});
            return AssertionStatus.FAILED;
        }

        // Map of all parsed fields. Map key is field name.
        final Map<String, Field> fields = new HashMap<String, Field>();

        // Parses for fields in request URI.
        final HttpServletRequestKnob httpServletRequestKnob = (HttpServletRequestKnob) requestMessage.getKnob(HttpServletRequestKnob.class);
        paramsToFields((Map<String, String[]>) httpServletRequestKnob.getQueryParameterMap(), fields, true);

        // Parses for fields in request body.
        if (httpMethod.equalsIgnoreCase(GenericHttpClient.METHOD_POST)) {
            if (outerContentType.matches("application", "x-www-form-urlencoded")) {
                paramsToFields((Map<String, String[]>) httpServletRequestKnob.getRequestBodyParameterMap(), fields, false);
            } else if (outerContentType.matches("multipart", "form-data")) {
                parseMultipartFormData(fields, mimeKnob);
            } else {
                // HTML 4.0 Spec says "Behavior for other content types is unspecified."
                // Ignore message body.
            }
        }

        // Converts array of allowed fields into a map for faster lookup.
        final Map<String, HtmlFormDataAssertion.FieldSpec> fieldSpecs = new HashMap<String, HtmlFormDataAssertion.FieldSpec>();
        for (HtmlFormDataAssertion.FieldSpec fieldSpec : _assertion.getFieldSpecs()){
            fieldSpecs.put(fieldSpec.getName(), fieldSpec);
        }

        // Enforces existence of specified fields:
        for (HtmlFormDataAssertion.FieldSpec required : fieldSpecs.values()) {
            if (required.getMinOccurs() > 0 && !fields.containsKey(required.getName())) {
                _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_FIELD_NOT_FOUND, new String[]{required.getName()});
                return AssertionStatus.FAILED;
            }
        }

        // Enforces constraints on field values:
        for (Field field : fields.values()) {
            if (fieldSpecs.containsKey(field.name)) {
                final HtmlFormDataAssertion.FieldSpec fieldSpec = fieldSpecs.get(field.name);

                // Enforces data type:
                for (FieldValue fieldValue : field.fieldValues) {
                    final HtmlFormDataType allowedDataType = fieldSpec.getDataType();
                    if (allowedDataType == HtmlFormDataType.ANY) {
                        // No testing needed.
                    } else if (allowedDataType == HtmlFormDataType.NUMBER) {
                        try {
                            Double.valueOf(fieldValue.value);
                        } catch (NumberFormatException e) {
                            _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_FAIL_DATATYPE, new String[]{field.name, fieldValue.value, allowedDataType.getWspName()});
                            return AssertionStatus.FAILED;
                        }
                    } else if (allowedDataType == HtmlFormDataType.FILE) {
                        if (!fieldValue.isFile) {
                            _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_FAIL_DATATYPE, new String[]{field.name, fieldValue.value, allowedDataType.getWspName()});
                        }
                    } else {
                        throw new IllegalArgumentException("Internal Error: Missing handler for data type " + allowedDataType);
                    }
                    if (_logger.isLoggable(Level.FINER)) {
                        _logger.finer("Verified: Form field data type (name=" + field.name + ", value=" + fieldValue.value + ", data type allowed=" + fieldSpec.getDataType() + ")");
                    }
                }

                // Enforces min. and max. occurrences:
                if (field.fieldValues.size() < fieldSpec.getMinOccurs()) {
                    _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_FAIL_MINOCCURS, new String[]{field.name, Integer.toString(field.fieldValues.size()), Integer.toString(fieldSpec.getMinOccurs())});
                    return AssertionStatus.FAILED;
                }
                if (field.fieldValues.size() > fieldSpec.getMaxOccurs()) {
                    _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_FAIL_MAXOCCURS, new String[]{field.name, Integer.toString(field.fieldValues.size()), Integer.toString(fieldSpec.getMaxOccurs())});
                    return AssertionStatus.FAILED;
                }
                if (_logger.isLoggable(Level.FINER)) {
                    _logger.finer("Verified: Form field occurrences (name=" + field.name + ", occurs=" + field.fieldValues.size() + ", minOccurs=" + fieldSpec.getMinOccurs() + ", maxOccurs=" + fieldSpec.getMaxOccurs() + ")");
                }

                // Enforces location:
                final HtmlFormDataLocation allowedLocation = fieldSpec.getAllowedLocation();
                if (allowedLocation == HtmlFormDataLocation.ANYWHERE) {
                    // No need to check anything.
                } else {
                    if (field.inUri && allowedLocation != HtmlFormDataLocation.URL) {
                        _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_LOCATION_NOT_ALLOWED, new String[]{field.name, HtmlFormDataLocation.URL.toString()});
                        return AssertionStatus.FAILED;
                    }
                    if (field.inBody && allowedLocation != HtmlFormDataLocation.BODY) {
                        _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_LOCATION_NOT_ALLOWED, new String[]{field.name, HtmlFormDataLocation.BODY.toString()});
                        return AssertionStatus.FAILED;
                    }
                }
                if (_logger.isLoggable(Level.FINER)) {
                    _logger.finer("Verified: Form field location (name=" + field.name + ", location allowed=" + allowedLocation + ")");
                }

            } else {
                if (_assertion.isDisallowOtherFields()) {
                    _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_UNKNOWN_FIELD_NOT_ALLOWED, new String[]{field.name});
                    return AssertionStatus.FAILED;
                } else {
                    _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_UNKNOWN_FIELD_ALLOWED, new String[]{field.name});
                }
            }
        }

        return AssertionStatus.NONE;
    }

    /**
     * Adds elements of a parameter map into a {@link Field} map.
     *
     * @param params        parameter map
     * @param fields        <code>Field</code>s map
     * @param fromUri       whether the parameters comes from the URI
     */
    private static void paramsToFields(final Map<String, String[]> params, final Map<String, Field> fields, final boolean fromUri) {
        final boolean fromBody = !fromUri;

        for (String name : params.keySet()) {
            Field field = fields.get(name);
            if (field == null) {
                field = new Field(name);
                fields.put(name, field);
            }
            for (String value : params.get(name)) {
                field.fieldValues.add(new FieldValue(value, false));
            }
            field.inUri |= fromUri;
            field.inBody |= fromBody;
        }
    }

    /**
     * Parses multipart/form-data for fields; in particular, scan for file uploads.
     *
     * @param holder    map to put results into
     * @param mimeKnob  the request MIME knob
     * @throws IOException if parsing error
     * @see <a href="http://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.2">HTML 4.0 specification for Form submission using multipart/form-data</a>
     */
    private void parseMultipartFormData(final Map<String, Field> holder, final MimeKnob mimeKnob)
            throws IOException {
        final PartIterator itor = mimeKnob.getParts();
        try {
            for (int partPosition = 0; itor.hasNext(); ++ partPosition) {
                final PartInfo partInfo = itor.next();

                final MimeHeader contentDispositionHeader = partInfo.getHeader("Content-Disposition");
                if (contentDispositionHeader == null) {
                    throw new IOException("Missing Content-Disposition header in a multipart/form-data part. (part position=" + partPosition + ")");
                }

                final ContentDisposition contentDisposition = new ContentDisposition(contentDispositionHeader.getFullValue());

                final String disposition = contentDisposition.getDisposition();
                if (!"form-data".equals(disposition)) {
                    throw new IOException("Content-Disposition in a multipart/form-data part is not \"form-data\". (part position=" + partPosition + ", header=" + contentDispositionHeader.getFullValue() + ")");
                }

                final String name = contentDisposition.getParameter("name");
                if (name == null) {
                    throw new IOException("Content-Disposition in a multipart/form-data part is missing the \"name\" parameter. (part position=" + partPosition + ", header=" + contentDispositionHeader.getFullValue() + ")");
                }

                Field field = holder.get(name);
                if (field == null) {
                    field = new Field(name);
                    holder.put(name, field);
                }
                field.inBody = true;

                final ContentTypeHeader partContentType = partInfo.getContentType();
                if (partContentType.matches("multipart", "mixed")) {
                    throw new UnsupportedOperationException("Multipart/mixed not supported.");
                } else {
                    String value = null;
                    final String filename = contentDisposition.getParameter("filename");
                    final boolean isFile = filename != null;
                    if (isFile) {
                        value = filename;
                    } else {
                        final byte[] valueBytes = HexUtils.slurpStream(partInfo.getInputStream(false));
                        value = new String(valueBytes, partInfo.getContentType().getEncoding());
                    }

                    final FieldValue fieldValue = new FieldValue(value, isFile);
                    field.fieldValues.add(fieldValue);
                }
            } // for
        } catch (NoSuchPartException e) {
            throw new IOException("Cannot read multipart/form-data: " + e.toString());
        } catch (ParseException e) {
            throw new IOException("Cannot parse Content-Disposition of a multipart/form-data part. Cause: " + e.toString());
        }
    }
}
