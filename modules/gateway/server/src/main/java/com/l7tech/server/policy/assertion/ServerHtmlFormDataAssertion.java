/**
 * Copyright (C) 2007-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.IOUtils;
import com.l7tech.common.mime.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ParseException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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
    private final Auditor _auditor;

    public ServerHtmlFormDataAssertion(final HtmlFormDataAssertion assertion, final ApplicationContext springContext) {
        super(assertion);
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
        final HttpMethod httpMethod = httpRequestKnob.getMethod();
        final ContentTypeHeader outerContentType = mimeKnob.getOuterContentType();
        if (httpMethod == HttpMethod.POST) {
            if (!(   outerContentType.matches("application", "x-www-form-urlencoded")
                  || outerContentType.matches("multipart", "form-data"))) {
                _auditor.logAndAudit(AssertionMessages.HTTP_POST_NOT_FORM_DATA, outerContentType.getMainValue());
                return AssertionStatus.NOT_APPLICABLE;
            }
        }

        // Enforces HTTP method(s) allowed:
        if ( httpMethod == HttpMethod.GET  && assertion.isAllowGet() || httpMethod == HttpMethod.POST && assertion.isAllowPost()) {
            if (_logger.isLoggable(Level.FINER)) {
                _logger.finer("Verified: HTTP request method (" + httpMethod + ")");
            }
        } else {
            _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_METHOD_NOT_ALLOWED, httpMethod.name());
            return AssertionStatus.FALSIFIED;
        }

        // Map of all parsed fields. Map key is field name.
        final Map<String, Field> fields = new HashMap<String, Field>();

        // Parses for fields in request URI.
        final HttpServletRequestKnob httpServletRequestKnob = (HttpServletRequestKnob) requestMessage.getKnob(HttpServletRequestKnob.class);
        paramsToFields(httpServletRequestKnob.getQueryParameterMap(), fields, true);

        // Parses for fields in request body.
        if (httpMethod == HttpMethod.POST) {
            if (outerContentType.matches("application", "x-www-form-urlencoded")) {
                paramsToFields(httpServletRequestKnob.getRequestBodyParameterMap(), fields, false);
            } else if (outerContentType.matches("multipart", "form-data")) {
                parseMultipartFormData(fields, mimeKnob);
            } else {
                // HTML 4.0 Spec says "Behavior for other content types is unspecified."
                // Ignore message body.
            }
        }

        // Converts array of allowed fields into a map for faster lookup.
        final Map<String, HtmlFormDataAssertion.FieldSpec> fieldSpecs = new HashMap<String, HtmlFormDataAssertion.FieldSpec>();
        for (HtmlFormDataAssertion.FieldSpec fieldSpec : assertion.getFieldSpecs()){
            fieldSpecs.put(fieldSpec.getName(), fieldSpec);
        }

        // Enforces existence of specified fields:
        for (HtmlFormDataAssertion.FieldSpec required : fieldSpecs.values()) {
            if (required.getMinOccurs() > 0 && !fields.containsKey(required.getName())) {
                _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_FIELD_NOT_FOUND, required.getName());
                return AssertionStatus.FALSIFIED;
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
                            _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_FAIL_DATATYPE, field.name, fieldValue.value, allowedDataType.getWspName());
                            return AssertionStatus.FALSIFIED;
                        }
                    } else if (allowedDataType == HtmlFormDataType.FILE) {
                        if (!fieldValue.isFile) {
                            _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_FAIL_DATATYPE, field.name, fieldValue.value, allowedDataType.getWspName());
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
                    _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_FAIL_MINOCCURS, field.name, Integer.toString(field.fieldValues.size()), Integer.toString(fieldSpec.getMinOccurs()));
                    return AssertionStatus.FALSIFIED;
                }
                if (field.fieldValues.size() > fieldSpec.getMaxOccurs()) {
                    _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_FAIL_MAXOCCURS, field.name, Integer.toString(field.fieldValues.size()), Integer.toString(fieldSpec.getMaxOccurs()));
                    return AssertionStatus.FALSIFIED;
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
                        _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_LOCATION_NOT_ALLOWED, field.name, HtmlFormDataLocation.URL.toString());
                        return AssertionStatus.FALSIFIED;
                    }
                    if (field.inBody && allowedLocation != HtmlFormDataLocation.BODY) {
                        _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_LOCATION_NOT_ALLOWED, field.name, HtmlFormDataLocation.BODY.toString());
                        return AssertionStatus.FALSIFIED;
                    }
                }
                if (_logger.isLoggable(Level.FINER)) {
                    _logger.finer("Verified: Form field location (name=" + field.name + ", location allowed=" + allowedLocation + ")");
                }

            } else {
                if (assertion.isDisallowOtherFields()) {
                    _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_UNKNOWN_FIELD_NOT_ALLOWED, field.name);
                    return AssertionStatus.FALSIFIED;
                } else {
                    _auditor.logAndAudit(AssertionMessages.HTMLFORMDATA_UNKNOWN_FIELD_ALLOWED, field.name);
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
     * @param holder    map to add results into
     * @param mimeKnob  the request MIME knob
     * @throws IOException if parsing error
     * @see <a href="http://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.2">HTML 4.0 specification for Form submission using multipart/form-data</a>
     */
    private void parseMultipartFormData(final Map<String, Field> holder, final MimeKnob mimeKnob)
            throws IOException {
        try {
            final PartIterator itor = mimeKnob.getParts();
            for (int partPosition = 0; itor.hasNext(); ++ partPosition) {
                PartInfo partInfo;
                try {
                    partInfo = itor.next();
                } catch (NoSuchElementException e) {
                    _logger.info("Multipart/form-data may have improperly terminated MIME body.");
                    break;  // This does not warrant a BAD_REQUEST.
                }

                final MimeHeader contentDispositionHeader = partInfo.getHeader("Content-Disposition");
                if (contentDispositionHeader == null) {
                    throw new IOException("Missing Content-Disposition header in a multipart/form-data part. (part position=" + partPosition + ")");
                }

                final ContentDisposition contentDisposition = new ContentDisposition(contentDispositionHeader.getFullValue());

                final String disposition = contentDisposition.getDisposition();
                if (!"form-data".equals(disposition)) {
                    // RFC 2388 says this is required.
                    throw new IOException("Content-Disposition in a multipart/form-data part is not \"form-data\". (part position=" + partPosition + ", header=" + contentDispositionHeader.getFullValue() + ")");
                }

                final String name = contentDisposition.getParameter("name");
                if (name == null) {
                    // RFC 2388 says this is required.
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
                    // This is a multipart/mixed subpart embedded within a multipart/form-data.
                    parseMultipartMixed(field, partInfo.getInputStream(false), partContentType);
                } else {
                    String value;
                    final String filename = contentDisposition.getParameter("filename");
                    final boolean isFile = filename != null;
                    if (isFile) {
                        value = filename;
                    } else {
                        final byte[] valueBytes = IOUtils.slurpStream(partInfo.getInputStream(false));
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

    /**
     * Parses for file upload in a multipart/mixed subpart embedded within a
     * multipart/form-data.
     *
     * @param field                 where to save parsed filename(s)
     * @param is                    input stream for the multipart/mixed MIME part
     * @param outerContentType      content type of the multipart/mixed
     * @throws IOException if parsing error
     */
    private void parseMultipartMixed(final Field field, final InputStream is, final ContentTypeHeader outerContentType) throws IOException {
        try {
            final MimeBody mimeBody = new MimeBody(new ByteArrayStashManager(), outerContentType, is);
            final PartIterator itor = mimeBody.iterator();
            for (int subpartPosition = 0; itor.hasNext(); ++ subpartPosition) {
                PartInfo subpartInfo;
                try {
                    subpartInfo = itor.next();
                } catch (NoSuchElementException e) {
                    _logger.info("Nested multipart/mixed may have improperly terminated MIME body.");
                    break;  // This does not warrant a BAD_REQUEST.
                }

                final MimeHeader contentDispositionHeader = subpartInfo.getHeader("Content-Disposition");
                if (contentDispositionHeader == null) {
                    throw new IOException("Missing Content-Disposition header in a multipart/mixed subpart. (subpart position=" + subpartPosition + ")");
                }

                final ContentDisposition contentDisposition = new ContentDisposition(contentDispositionHeader.getFullValue());

                final String disposition = contentDisposition.getDisposition();
                if (!"file".equals(disposition)) {
                    // RFC 2388 does not explicitly forbid otherwise.
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.fine("Ignoring non-file multipart/mixed subpart. (subpart position=" + subpartPosition + ", header=" + contentDispositionHeader.getFullValue() + ")");
                    }
                    continue;
                }

                final String filename = contentDisposition.getParameter("filename");
                if (filename == null) {
                    // RFC 2388 does not explicitly forbid otherwise.
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.fine("Ignoring a multipart/mixed file subpart without the \"filename\" parameter. (subpart position=" + subpartPosition + ", header=" + contentDispositionHeader.getFullValue() + ")");
                    }
                    continue;
                }

                field.fieldValues.add(new FieldValue(filename, true));
            }
        } catch (ParseException e) {
            throw new IOException("Cannot parse Content-Disposition of a multipart/mixed subpart. Cause: " + e.toString());
        }
    }
}
