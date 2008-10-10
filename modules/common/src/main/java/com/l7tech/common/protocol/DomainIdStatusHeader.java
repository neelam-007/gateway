package com.l7tech.common.protocol;

import com.l7tech.common.mime.MimeHeader;
import static com.l7tech.common.protocol.SecureSpanConstants.HttpHeaders.HEADER_DOMAINIDSTATUS;
import com.l7tech.util.CausedIOException;

import javax.mail.internet.HeaderTokenizer;
import javax.mail.internet.ParseException;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class to parse, create, and represent a Domain ID Injection status header, as specified by
 * {@link SecureSpanConstants.HttpHeaders#HEADER_DOMAINIDSTATUS}.
 */
public class DomainIdStatusHeader extends MimeHeader {
    private final DomainIdStatusCode status;

    protected DomainIdStatusHeader(String value, Map<String, String> params) throws IOException {
        super(HEADER_DOMAINIDSTATUS, value, params);
        try {
            status = DomainIdStatusCode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IOException("Unrecognized domain ID status code: " + value);
        }
    }

    /**
     * @return the status represented by this header (ie, INCLUDED, DECLINED, etc).  Never null.
     */
    public DomainIdStatusCode getStatus() {
        return status;
    }


    /**
     * Parse an L7-Domain-ID-Status: header, not including the header name and colon.
     * Example: <code>{@link #parseValue}("INCLUDED; username=X-Injected-User-Name")</code>
     *
     * @param fullValue the header value to parse
     * @return a DomainIdStatusHeader instance.  Never null.
     * @throws java.io.IOException  if the specified header value was missing, empty, or syntactically invalid
     */
    public static DomainIdStatusHeader parseValue(String fullValue) throws IOException {
        if (fullValue == null || fullValue.length() < 1)
            throw new IOException("L7-Domain-Id-Status header missing or empty");

        if (fullValue.endsWith(";")) {
            fullValue = fullValue.substring(0, fullValue.length()-1);
        }

        HeaderTokenizer ht = new HeaderTokenizer(fullValue, HeaderTokenizer.MIME, true);
        HeaderTokenizer.Token token;
        try {
            // Get status
            token = ht.next();
            if (token.getType() == HeaderTokenizer.Token.EOF)
                throw new IOException("L7-Domain-Id-Status status value missing");
            if (token.getType() != HeaderTokenizer.Token.ATOM)
                throw new IOException("L7-Domain-Id-Status status value is not an atom: " + fullValue);

            String status = token.getValue();

            // Check for parameters
            Map<String, String> params = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            for (;;) {
                token = ht.next();

                if (token.getType() == HeaderTokenizer.Token.EOF)
                    break;

                if (token.getType() != ';')
                    throw new IOException("L7-Domain-Id-Status parameter is not introduced with a semicolon: " + fullValue);

                // Get name
                token = ht.next();
                if (token.getType() == HeaderTokenizer.Token.EOF)
                    throw new IOException("L7-Domain-Id-Status parameter name missing");

                if (token.getType() != HeaderTokenizer.Token.ATOM)
                    throw new IOException("L7-Domain-Id-Status parameter name is not an atom: " + fullValue);

                String name = token.getValue();
                if (params.containsKey(name))
                    throw new IOException("L7-Domain-Id-Status parameter name occurs more than once: " + fullValue);

                // eat =
                token = ht.next();
                if (token.getType() != '=')
                    throw new IOException("L7-Domain-Id-Status parameter name is not followed by an equals: " + fullValue);

                // Get value
                StringBuffer value = new StringBuffer();
                boolean sawQuotedString = false;
                for (;;) {
                    token = ht.peek();
                    int tokenType = token.getType();
                    if (tokenType == HeaderTokenizer.Token.EOF || tokenType == ';')
                        break;

                    token = ht.next();

                    if (tokenType == HeaderTokenizer.Token.QUOTEDSTRING) {
                        if (sawQuotedString)
                            throw new IOException("L7-Domain-Id-Status parameter value has more than one quoted string: " + fullValue);
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

                    throw new IOException("L7-Domain-Id-Status parameter value had unexpected token: " + token.getType() + " in: " + fullValue);
                }

                params.put(name, value.toString());
            }

            return new DomainIdStatusHeader(status, params);
        } catch (ParseException e) {
            throw new CausedIOException("Unable to parse L7-Domain-Id-Status header", e);
        }
    }
}


