package com.l7tech.common.security.xml;

import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

import javax.mail.internet.HeaderTokenizer;
import javax.mail.internet.ParseException;
import javax.mail.internet.MimeUtility;

import com.ibm.xml.dsig.TransformException;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.mime.MimeHeaders;
import com.l7tech.common.mime.MimeHeader;

/**
 * Transform to canonicalize a MIME attachment header and content.
 *
 * <p>See Attachment-Complete Reference Transform section in Web Services
 * Security SOAP Messages with Attachments (SwA) Profile 1.0.</p>
 *
 * <p>XWSS 2.0 does not correctly canonicalize MIME headers when they are not
 * in ascending lexicographic order.</p>
 *
 * <p>XWSS 2.0 only serializes each header if it comes AFTER the the
 * preceeding (lexicographically) header in the source document (and all
 * processed headers are present).</p>
 *
 * <p>e.g. Assuming typical MIME part headers:</p>
 *
 * <pre>
 *  Content-Type: text/plain
 *  Content-ID: &lt;attachment-1>
 *  Content-transfer-encoding: binary
 * </pre>
 *
 * <P>XWSS 2.0 will not serialize any of them since the preceeding headers
 * are missing. In this case the output is just the default Content-Type
 * header.</p>
 *
 * <p>To work around this issue the client MUST include all expected headers in
 * the expected order:</p>
 *
 * <pre>
 *   Content-Description: my content
 *   Content-Disposition: attachment; filename=myfile.txt
 *   Content-ID: &lt;attachment-EF2D12CBB123FA-@layer7tech.com>
 *   Content-Location: myfile.txt
 *   Content-Type: text/plain
 * </pre>
 *
 * <p>Note that the order on the wire is NOT important, just the order that
 * the headers are seen by XWSS for canonicalization (when outbound).</p>
 *
 * <p>There is no point in us creating a "broken" implementation for
 * compatibility, since the XWSS 2.0 implementation is worthless unless the
 * header workaround described here is used.</p>
 *
 * <p>The Content-Only transform should be used if the client cannot control
 * the MIME part headers.</p>
 *
 * @author Steve Jones
 */
public class AttachmentCompleteTransform extends AttachmentContentTransform {

    //- PUBLIC

    /**
     * Get the URI that identifies this transform.
     *
     * @return The transform identifier.
     */
    public String getURI() {
        return SoapUtil.TRANSFORM_ATTACHMENT_COMPLETE;
    }

    //- PROTECTED

    /**
     */
    protected void processHeaders(final MimeHeaders headers,
                                  final OutputStream out) throws IOException, TransformException {
        canonicalizeMimeHeader(out, headers, MIME_HEADER_CONTENT_DESCRIPTION, false, null);
        canonicalizeMimeHeader(out, headers, MIME_HEADER_CONTENT_DISPOSITION, true, null);
        canonicalizeMimeHeader(out, headers, MIME_HEADER_CONTENT_ID, true, null);
        canonicalizeMimeHeader(out, headers, MIME_HEADER_CONTENT_LOCATION, true, null);
        canonicalizeMimeHeader(out, headers, MIME_HEADER_CONTENT_TYPE, true, VALUE_HEADER_CONTENT_TYPE);
        out.write(CRLF);
    }

    //- PRIVATE

    private static final byte[] HEADER_NAME_VALUE_SEPARATOR = new byte[]{':'};
    private static final byte[] PARAMETER_NAME_VALUE_SEPARATOR = new byte[]{'='};
    private static final byte[] PARAMETER_PAIR_SEPARATOR = new byte[]{';'};
    private static final byte[] PARAMETER_PREFIX = new byte[]{'"'};
    private static final byte[] PARAMETER_POSTFIX = PARAMETER_PREFIX;
    private static final String CHARSET = "UTF-8";

    // Content-Disposition, Content-ID, Content-Location, Content-Type are structured,
    // Content-Description is unstructured 
    private static final String MIME_HEADER_CONTENT_DESCRIPTION = "Content-Description";
    private static final String MIME_HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String MIME_HEADER_CONTENT_ID = "Content-ID";
    private static final String MIME_HEADER_CONTENT_LOCATION = "Content-Location";
    private static final String MIME_HEADER_CONTENT_TYPE = "Content-Type";


    // MIME header parameters whose value should be converted to lower case
    private static final String[] HEADER_PARAMS_TO_LOWERCASE = {
        "type",
        "padding",
        "charset",
        "creation-date",
        "modification-date",
        "read-date",
        "size",            
    };


    /**
     * Process the given MIME header 
     */
    private final void canonicalizeMimeHeader(final OutputStream out,
                                              final MimeHeaders headers,
                                              final String headerName,
                                              final boolean structured,
                                              final String headerDefault) throws IOException, TransformException {
        MimeHeader header = headers.get(headerName);

        String headerValue = null;
        if ( header != null ) {
            headerValue = header.getFullValue();
        }

        byte[] value;
        if (headerValue != null) {
            if ( structured ) {
                value = canonicalizeValue(headerValue, isLowerCaseValue(headerName));
            } else {
                value = headerValue.getBytes(CHARSET);
            }
        } else {
            value = headerDefault!=null ? headerDefault.getBytes(CHARSET) : null;
        }

        if ( value != null) {
            out.write(headerName.getBytes(CHARSET));
            out.write(HEADER_NAME_VALUE_SEPARATOR);
            out.write(value);
            out.write(CRLF);
        }
    }

    /**
     * Should the value be lower case for this header 
     */
    private boolean isLowerCaseValue(final String headerName) {
        boolean lowercase = false;

        if ( MIME_HEADER_CONTENT_DISPOSITION.equals(headerName) ||
             MIME_HEADER_CONTENT_TYPE.equals(headerName) ) {
            lowercase = true;
        }

        return lowercase;
    }

    /**
     * Convert value to canonical form by:
     * 
     * - Fix whitespace for structured headers
     * - Remove comments
     * - Use lowercase for media type/subtype values and disposition-type values
     * - Unquote quoted characters other than double-quote and backslash ("\") in quoted strings
     * - Order parameters in lexicographic order (ascending)
     * - Use lowercase for MIME header parameter names
     * - MIME parameter values containing RFC2184 character set, language, and continuations MUST be decoded.
     * - Case-insensitive MIME header parameter values MUST be converted to lowercase. Case-sensitive
     *     MIME header parameter values MUST be left as is with respect to case [RFC2045].
     * - Enclosing double-quotes MUST be added to MIME header parameter values that do not already
     *     contain enclosing quotes. Quoted characters other than double-quote and backslash ("\") in MIME
     *     header parameter values MUST be unquoted. Double-quote and backslash characters in MIME
     *     parameter values MUST be character encoded
     * - Canonicalization of a MIME header parameter MUST generate a UTF-8 encoded octet stream
     *     containing the following: a semi-colon (";"), the parameter name (lowercase), an equals sign ("="), and
     *     the double-quoted parameter value.
     */
    private byte[] canonicalizeValue(final String value,
                                     final boolean lowercase) throws IOException, TransformException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            // parse value
            Map<String,String> params = new TreeMap(String.CASE_INSENSITIVE_ORDER);
            String mainValue = parseValue(value, params);

            // write main value
            if (lowercase) {
                mainValue = mainValue.toLowerCase();
            }

            baos.write(mainValue.getBytes(CHARSET));

            // write parameters
            for (Map.Entry<String,String> parameter : params.entrySet()) {
                String name = parameter.getKey().toLowerCase();
                String data = escape(parameter.getValue());

                baos.write(PARAMETER_PAIR_SEPARATOR);
                baos.write(name.getBytes(CHARSET));
                baos.write(PARAMETER_NAME_VALUE_SEPARATOR);
                baos.write(PARAMETER_PREFIX);
                if ( ArrayUtils.contains(HEADER_PARAMS_TO_LOWERCASE, name) ) {
                    baos.write(data.toLowerCase().getBytes(CHARSET));
                } else {
                    baos.write(data.getBytes(CHARSET));
                }
                baos.write(PARAMETER_POSTFIX);
            }

        } catch (ParseException parseException) {
            throw new TransformException("Error processing MIME value '"+value+"', error is '"+ ExceptionUtils.getMessage(parseException)+"'.");
        }
        
        return baos.toByteArray();
    }

    /**
     * Shamelessly stolen from ContentTypeHeader#parseValue 
     */
    private String parseValue(final String headerValue, final Map<String,String> params) throws ParseException {
        String cleanHeaderValue = headerValue;

        if (cleanHeaderValue.endsWith(";")) {
            cleanHeaderValue = cleanHeaderValue.substring(0, cleanHeaderValue.length()-1);
        }

        HeaderTokenizer ht = new HeaderTokenizer(cleanHeaderValue, HeaderTokenizer.MIME, true); // comments skipped

        // process main value
        StringBuilder valueBuilder = new StringBuilder();
        HeaderTokenizer.Token token = parseMainValue(ht, headerValue, valueBuilder);

        // process parameters
        parseValueParameters(ht, token, headerValue, params);
        decodeRFC2184(params);

        return valueBuilder.toString();
    }

    /**
     *
     */
    private HeaderTokenizer.Token parseMainValue(final HeaderTokenizer ht,
                                                 final String headerValue,
                                                 final StringBuilder valueBuilder) throws ParseException {
        HeaderTokenizer.Token token;

        while (true) {
            token = ht.next();

            if (token.getType() == ';' || token.getType() == HeaderTokenizer.Token.EOF)
                break;

            // Process token
            if (token.getType() != HeaderTokenizer.Token.ATOM &&
                HeaderTokenizer.MIME.indexOf(token.getType())<0)
                throw new ParseException("MIME value is not an atom: " + headerValue);

            valueBuilder.append(token.getValue());
        }
        if ( valueBuilder.length()==0 )
            throw new ParseException("MIME value missing");

        return token;
    }

    /**
     *
     */
    private void parseValueParameters(final HeaderTokenizer ht, 
                                      final HeaderTokenizer.Token t,
                                      final String headerValue,
                                      final Map<String,String> params) throws ParseException {
        HeaderTokenizer.Token token = t;

        while (true) {
            if (token.getType() == HeaderTokenizer.Token.EOF)
                break;

            if (token.getType() != ';')
                throw new ParseException("MIME parameter is not introduced with a semicolon: " + headerValue + " at " + token.getValue());

            // Get name
            token = ht.next();
            if (token.getType() == HeaderTokenizer.Token.EOF)
                throw new ParseException("MIME parameter name missing");

            if (token.getType() != HeaderTokenizer.Token.ATOM)
                throw new ParseException("MIME parameter name is not an atom: " + headerValue);

            String name = token.getValue();
            if (params.containsKey(name))
                throw new ParseException("MIME parameter name occurs more than once: " + headerValue);

            // eat =
            token = ht.next();
            if (token.getType() != '=')
                throw new ParseException("MIME parameter name is not followed by an equals: " + headerValue);

            // Get value
            StringBuilder value = new StringBuilder();
            boolean sawQuotedString = false;
            for (;;) {
                token = ht.peek();
                int tokenType = token.getType();
                if (tokenType == HeaderTokenizer.Token.EOF || tokenType == ';')
                    break;

                token = ht.next();

                if (tokenType == HeaderTokenizer.Token.QUOTEDSTRING) {
                    if (sawQuotedString)
                        throw new ParseException("MIME parameter value has more than one quoted string: " + headerValue);
                    sawQuotedString = true;
                    value.append(token.getValue());
                    continue;
                }

                if (tokenType == HeaderTokenizer.Token.ATOM) {
                    value.append(token.getValue());
                    continue;
                }

                if (tokenType > 0 && !Character.isISOControl(tokenType)) {
                    value.append((char)tokenType);
                    continue;
                }

                throw new ParseException("MIME parameter value had unexpected token: " + token.getType() + " in: " + headerValue);
            }

            params.put(name, value.toString());

            token = ht.next();
        }
    }

    /**
     * Unescape any characters that do not require escaping.
     */
    private String escape(final String value) {
        StringBuilder builder = new StringBuilder(value.length());

        for ( int i=0; i<value.length(); i++ ) {
            char character = value.charAt(i);

            if ( character == '\\' || character == '"' ) {
                builder.append( '\\' );
            }

            builder.append( character );                
        }

        return builder.toString();
    }

    /**
     * Decode any RFC 2184 parameter continuations or language/encodings
     *
     * The param map is ordered so params with continuations should be in order.
     *
     * Decode continuations (values are already unquoted here).
     *
     *   Content-Type: message/external-body; access-type=URL; URL*0="ftp://"; URL*1="cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar"
     *
     * becomes
     *
     *   Content-Type: message/external-body; access-type=URL; URL"ftp://cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar"
     *
     * so the number of parameters is reduced and names are changed.
     *
     * Parameter values with languages look like this:
     *
     *   title*=us-ascii'en-us'This%20is%20%2A%2A%2Afun%2A%2A%2A
     *
     * so contain charset, language and (hex) encoded value
     *
     * http://www.faqs.org/rfcs/rfc2184.html
     */
    private void decodeRFC2184(final Map<String,String> params) throws ParseException {
        Map<String,String> decoded = new HashMap();

        for (Map.Entry<String,String> entry : params.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            String charset = "UTF-8"; // really ascii

            if ( name.endsWith("*") ) {
                name = name.substring(0, name.length()-1); // ignore language setting for now.

                if (name.contains("*") && !name.contains("*1"))
                    continue; // this will have already been processed.

                String[] parts = value.split("'");
                if ( parts.length != 3)
                    throw new ParseException("Illegal RFC 2184 parameter value '"+value+"'.");

                charset = parts[0];
                value = hexDecode(parts[2], charset);
            }

            int index;
            if ( (index = name.indexOf("*")) > -1 ) {
                // multiple parts
                String baseName = name.substring(0, index);
                try {
                    int partNumber = Integer.parseInt(name.substring(index+1));
                    if ( partNumber == 1 ) {
                        while (true) {
                            String partName = baseName + "*" + (++partNumber);

                            String partValue = params.get(partName);
                            if ( partValue != null ) {
                                // add unencoded part
                                value += partValue;
                            } else {
                                partValue = params.get(partName + "*");

                                if ( partValue == null)
                                    break; // no more parts

                                // add encoded part
                                value += hexDecode(partValue, charset);
                            }
                        }

                        decoded.put(baseName, value);
                    }
                } catch (NumberFormatException nfe) {
                    throw new ParseException("Illegal RFC 2184 parameter name '"+entry.getKey()+"'.");
                }
            } else {
                // single part
                decoded.put(name, value);
            }
        }

        params.clear();
        params.putAll(decoded);
    }

    /**
     * Decode a string with some hex bytes
     */
    private String hexDecode(String hex, String charset) throws ParseException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(128);

        for (int i=0; i<hex.length(); i++) {
            char character = hex.charAt(i);

            if (character == '%') {
                try {
                    baos.write(Integer.decode(("0x" + hex.charAt(++i) + hex.charAt(++i)).toUpperCase()));
                } catch (IndexOutOfBoundsException ioobe) {
                    throw new ParseException("Invalid encoding in MIME parameter value '"+hex+"'.");
                } catch (NumberFormatException nfe) {
                    throw new ParseException("Invalid encoding in MIME parameter value '"+hex+"'.");
                }
            } else {
                baos.write(character);
            }
        }

        try {
            return new String(baos.toByteArray(), MimeUtility.javaCharset(charset));
        } catch (UnsupportedEncodingException uee) {
            throw new ParseException("Unsupported encoding in MIME parameter '"+charset+"'.");
        }
    }

}
