package com.l7tech.common.http;

import com.l7tech.util.ConfigFactory;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * <p>Represents a cookie. Can be used on the client side (bridge) or server side (gateway).</p>
 *
 * <p>A cookie can be constructed using a "SetCookie" header (in which case it is counted as "new").</p>
 */
public class HttpCookie {

    private static final boolean STRICT_COOKIE_EXPIRY_FORMAT = ConfigFactory.getBooleanProperty( "com.l7tech.common.http.strickCookieExpiryFormat", false );

    //- PUBLIC

    /**
     * Create an HttpCookie out of the specified raw header value value.
     *
     * @param headerFullValue the value of a Set-Cookie header, ie:
     *    "PREF=ID=e51:TM=686:LM=86:S=BL-w0; domain=.google.com; path=/; expires=Sun, 17-Jan-2038 19:14:07 GMT; secure".
     * @throws HttpCookie.IllegalFormatException if the header cannot be parsed
     */
    public HttpCookie(URL requestUrl, String headerFullValue)
            throws HttpCookie.IllegalFormatException {
        this(requestUrl.getHost(), requestUrl.getPath(), headerFullValue);
    }

    /**
     * Create an HttpCookie out of the specified raw header value value.
     *
     * @param headerFullValue the value of a Set-Cookie header, ie:
     *    "PREF=ID=e51:TM=686:LM=86:S=BL-w0; domain=.google.com; path=/; expires=Sun, 17-Jan-2038 19:14:07 GMT; secure".
     * @throws HttpCookie.IllegalFormatException if the header cannot be parsed
     */
    public HttpCookie(String requestDomain, String requestPath, String headerFullValue)
            throws HttpCookie.IllegalFormatException {
        // Parse cookie
        if (headerFullValue == null || "".equals(headerFullValue)) {
            throw new HttpCookie.IllegalFormatException("Cookie value is empty");
        }
        fullValue = headerFullValue;

        //fields will contain the following
        //      name=value
        //      domain
        //      path
        //      expires
        //      secure
        String[] fields = WHITESPACE.split(fullValue);
        if (fields == null || fields.length ==0) {
            throw new HttpCookie.IllegalFormatException("Cookie value is an invalid format: '" + headerFullValue + "'");
        }

        //need to split the name=value pair in fields[0]
        String[] nameValue = EQUALS.split(fields[0], 2);
        if(nameValue.length!=2) {
            throw new HttpCookie.IllegalFormatException("Cookie value is an invalid format: '" + headerFullValue + "'");
        }
        cookieName = nameValue[0];
        cookieValue = trimQuotes(nameValue[1],1);

        // now parse each field from the rest of the cookie, if present
        boolean parsedSecure = false;
        String parsedExpires = null;
        int parsedMaxAge = -1;
        String parsedDomain = null;
        String parsedPath = null;
        String parsedComment = null;
        int parsedVersion = 0;
        for (int j=1; j<fields.length; j++) {

            if ("secure".equalsIgnoreCase(fields[j])) {
                parsedSecure = true;
            } else if (fields[j].indexOf('=') > 0) {
                String[] f = EQUALS.split(fields[j], 2);
                if ("expires".equalsIgnoreCase(f[0])) {
                    parsedExpires = f[1];
                } else if ("domain".equalsIgnoreCase(f[0])) {
                    parsedDomain = f[1];
                } else if ("path".equalsIgnoreCase(f[0])) {
                    parsedPath = f[1];
                } else if ("comment".equalsIgnoreCase(f[0])) {
                    parsedComment = f[1];
                } else if ("version".equalsIgnoreCase(f[0])) {
                    parsedVersion = Integer.parseInt(trimQuotes(f[1],1));
                } else if ("max-age".equalsIgnoreCase(f[0])) {
                    parsedMaxAge = Integer.parseInt(trimQuotes(f[1],1));
                }
            }
        }

        // do defaults if necessary
        if(parsedDomain==null) {
            domain = requestDomain;
            explicitDomain = false;
        }
        else {
            domain = trimQuotes(parsedDomain, parsedVersion);
            explicitDomain = true;
        }
        if(parsedPath==null && requestPath != null) {
            int trim = requestPath.lastIndexOf('/');
            if(trim>0) {
                parsedPath = requestPath.substring(0, trim);
            }
            else {
                parsedPath = requestPath;
            }
        }

        if(parsedVersion==0 && parsedExpires!=null) {
            List<Pattern> datePatternToFormat = CookieUtils.getDatePatterns();
            boolean match = false;
            for(Pattern p: datePatternToFormat){
                Matcher m = p.matcher(parsedExpires);
                if(m.matches()){
                    try{
                        String pattern = p.pattern();
                        parsedExpires = CookieUtils.expandYear(pattern, parsedExpires);
                        SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.getDateFormat(p), Locale.US);
                        long calculatedMaxAge = expiryFormat.parse(parsedExpires).getTime() - System.currentTimeMillis();
                        if(calculatedMaxAge>1000){
                            parsedMaxAge = (int)(calculatedMaxAge/1000L);
                        }
                        else{
                            parsedMaxAge = 0;
                        }
                        match = true;
                        break;
                     }catch (ParseException e){
                        throw new HttpCookie.IllegalFormatException("Invalid expires attribute: " + e.getMessage());
                     }
                }
            }
            if(STRICT_COOKIE_EXPIRY_FORMAT && !match){
                throw new HttpCookie.IllegalFormatException("Unknown expires format in Cookie");                
            }
        }


        secure = parsedSecure;
        maxAge = parsedMaxAge;
        path = trimQuotes(parsedPath, parsedVersion);
        comment = trimQuotes(parsedComment, parsedVersion);
        version = parsedVersion;
        newcook = true;

        createdTime = System.currentTimeMillis();
        id = buildId();
    }

    /**
     * Create a cookie as though from a "Set-Cookie" header, this will be passed
     * back to the client (outgoing cookie).
     *
     * @param name the name of the cookie
     * @param value the value of the cookie
     * @param version the cookie version (0 - Netscape, 1 - RFC 2109)
     * @param path the explictly set path (version 1+ only), may be null
     * @param domain the explictly set domain (version 1+ only), may be null
     * @param maxAge the maximum age in seconds (-1 for not specified)
     * @param secure is this a secure cookie
     * @param comment the comment, may be null
     */
    public HttpCookie(String name, String value, int version, String path, String domain, int maxAge, boolean secure, String comment) {
        this(name, value, version, path, domain, maxAge, secure, comment, true);
    }

    /**
     * @param name the name of the cookie
     * @param value the value of the cookie
     * @param version the cookie version (0 - Netscape, 1 - RFC 2109)
     * @param path the explicitly set path (version 1+ only), may be null
     * @param domain the explicitly set domain (version 1+ only), may be null
     * @param maxAge the maximum age in seconds (-1 for not specified)
     * @param secure is this a secure cookie
     * @param passToClient set to true if the cookie should be passed back to the client in the response
     * @param comment the comment, may be null
     */
    public HttpCookie(String name, String value, int version, String path, String domain, int maxAge, boolean secure, String comment, boolean passToClient) {
        this.cookieName = name;
        this.cookieValue = value;
        this.version = version;
        this.path = path;
        this.domain = domain;
        this.explicitDomain = domain!=null;
        this.fullValue = null;
        this.maxAge = maxAge;
        if(version==0) {
            this.comment = null;
        }
        else {
            this.comment = comment;
        }
        this.secure = secure;
        this.newcook = passToClient;
        this.createdTime = System.currentTimeMillis();

        this.id = buildId();
    }

    /**
     * Create a cookie as though from a "Cookie" header, this should not be passed back out
     * of the gateway (incoming cookie).
     *
     * @param name the name of the cookie
     * @param value the value of the cookie
     * @param version the cookie version (0 - Netscape, 1 - RFC 2109)
     * @param path the explictly set path (version 1+ only), may be null
     * @param domain the explictly set domain (version 1+ only), may be null
     */
    public HttpCookie(String name, String value, int version, String path, String domain) {
        this.cookieName = name;
        this.cookieValue = value;
        this.version = version;
        if(version>0) {
            this.path = path;
            this.domain = domain;
            this.explicitDomain = domain!=null;
        }
        else {
            this.path = this.domain = null;
            this.explicitDomain = false;
        }

        this.fullValue = null;
        this.maxAge = -1;
        this.secure = false;
        this.newcook = false;
        this.comment = null;
        this.createdTime = System.currentTimeMillis();

        this.id = buildId();
    }

    /**
     * Create a cookie with the same values as the given cookie but altered
     * domain/path.
     *
     * @param cookie the base cookie
     * @param domain the new value for the cookie domain
     * @param path the new value for the cookie path
     */
    public HttpCookie(HttpCookie cookie, String domain, String path) {
        this.cookieName = cookie.cookieName;
        this.cookieValue = cookie.cookieValue;
        this.version = cookie.version;
        this.maxAge = cookie.maxAge;
        this.secure = cookie.secure;
        this.newcook = cookie.newcook;
        this.comment = cookie.comment;
        this.createdTime = cookie.createdTime;

        this.fullValue = null;
        this.path = path;
        this.domain = domain;
        this.explicitDomain = true;

        this.id = buildId();
    }

    public String getId() {
        return id;
    }

    public String getCookieName() {
        return cookieName;
    }

    public int getVersion() {
        return version;
    }

    /**
     * @return the value of this cookie, ie "ID=e51:TM=686:LM=86:S=BL-w0"
     */
    public String getCookieValue() {
        return cookieValue;
    }

    public String getPath() {
        return path;
    }

    public boolean isDomainExplicit() {
        return explicitDomain;
    }

    public String getDomain() {
        return domain;
    }

    public String getComment() {
        return comment;
    }

    public boolean hasExpiry() {
        return maxAge >= 0;
    }

    public long getExpiryTime() {
        return createdTime + (1000L * maxAge);
    }

    public int getMaxAge() {
        return maxAge;
    }

    /**
     * @return true iff. this cookie should be sent only if transport-layer encryption is being used.
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     *
     */
    public boolean isNew() {
        return newcook;
    }



    /**
     *
     */
    public boolean isExpired() {
        boolean expired = false;
        if(maxAge==0) {
            expired = true;
        }
        else {
            if(createdTime+(maxAge*1000L) < System.currentTimeMillis()) {
                expired = true;
            }
        }

        return expired;
    }

    public boolean isOverwritePath() {
        return overwritePath;
    }

    public void setOverwritePath(boolean overwritePath) {
        this.overwritePath = overwritePath;
    }

    /**
     *
     */
    public int hashCode() {
        return id.hashCode() * 17;
    }

    /**
     *
     */
    public boolean equals(Object other) {
        boolean equal = false;

        if(this==other) {
            equal = true;
        }
        else if(other instanceof HttpCookie) {
            HttpCookie otherCookie = (HttpCookie) other;
            equal = this.id.equals(otherCookie.id);
        }

        return equal;
    }

    /**
     *
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer(50);
        buffer.append("HttpCookie()[name='");
        buffer.append(cookieName);
        buffer.append("',value='");
        buffer.append(cookieValue);
        buffer.append("',domain='");
        buffer.append(domain);
        buffer.append("',path='");
        buffer.append(path);
        buffer.append("',version='");
        buffer.append(version);
        buffer.append("']");
        return buffer.toString();
    }

    /**
     * Get this cookie formatted as part of a version 0 (Netscape) "cookie:"
     * header.
     *
     * @return "<Name>=<Value>"
     */
    public String getV0CookieHeaderPart() {
        StringBuffer headerPart = new StringBuffer();
        headerPart.append(cookieName);
        headerPart.append('=');
        headerPart.append(quoteIfNeeded(cookieValue));
        return headerPart.toString();
    }

    /**
     * Get the cookies as a string (as in a "Cookie:" header).
     *
     * NOTE: Since we may have modified the path/domain of the cookie when
     * proxying it seems safest to drop this from the header (see RFC 2109
     * section 4.3.4)
     *
     * @param cookies The collection of HttpCookie's to add
     * @return a string like "foo=bar; baz=blat; bloo=blot".  May be empty, but never null.
     */
    public static String getCookieHeader(final Collection cookies) {
        StringBuffer sb = new StringBuffer();

        if (cookies != null) {
            for (Iterator cookIter = cookies.iterator(); cookIter.hasNext();) {
                HttpCookie cook = (HttpCookie) cookIter.next();
                // always use V0, see note above
                sb.append(cook.getV0CookieHeaderPart());
                if (cookIter.hasNext())
                    sb.append("; ");
            }
        }

        return sb.toString();
    }

    public static Collection<HttpCookie> fromCookieHeader( final URL url,
                                                           final String cookieHeader ) throws HttpCookie.IllegalFormatException {
        final Collection<HttpCookie> cookies = new ArrayList<HttpCookie>();

        for ( String cookieNVP : cookieHeader.split( ";" )) {
            cookies.add( new HttpCookie( url, cookieNVP ) );           
        }

        return cookies;
    }

    /** @return the underlying cookie data as a Cookie Spec conformant string in the format:
     *      name=value; domain=thedomain.com; path=/; expires=Sun, 17-Jan-2038 19:14:07 GMT;secure
     *  this is identical to the string that was used to construct this object.
     */
    public String toExternalForm() {
        return fullValue;
    }

    /**
     * Exception for format errors
     */
    public static class IllegalFormatException extends Exception {
        public IllegalFormatException() {}

        public IllegalFormatException(Throwable cause) {
            super(cause);
        }

        public IllegalFormatException(String message) {
            super(message);
        }

        public IllegalFormatException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    //- PRIVATE

    private static final Pattern WHITESPACE = Pattern.compile(";\\s*");
    private static final Pattern EQUALS = Pattern.compile("=");

    private static final String NON_TOKEN_CHARS = ",; ";

    //store the full initial value of the cookie so that it can be regenerated later with ease
    private final String id;
    private final String fullValue;
    private final String cookieValue;
    private final String cookieName;
    private final String path;
    private final boolean explicitDomain;
    private final String domain;
    private final String comment;
    private final long createdTime;
    private final int maxAge;
    private final int version;
    private final boolean secure;
    private final boolean newcook;

    //Control attributes for the cookie
    private boolean overwritePath = true;

    /**
     * Called when all properties have been set to generate the cookies ID
     */
    private String buildId() {
        StringBuffer idBuffer = new StringBuffer();
        idBuffer.append(cookieName);
        idBuffer.append(">>");
        idBuffer.append(domain);
        idBuffer.append(">>");
        idBuffer.append(path);
        return idBuffer.toString().toLowerCase();
    }

    private String trimQuotes(String text, int version) {
        String trimmed = text;

        if(version>0 && trimmed!=null && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1,trimmed.length()-1);
        }

        return trimmed;
    }

    /**
     * Check if the text is a "token" or if it contains any illegal characters.
     *
     * NOTE: According to the RFC all the characters below are non-token chars,
     * Tomcat just checks for ",; " and < 32 >= 127, so we are doing the same.
     *
     *    0 - 31, 127
     *     "(" | ")" | "<" | ">" | "@"
     *   | "," | ";" | ":" | "\" | <">
     *   | "/" | "[" | "]" | "?" | "="
     *   | "{" | "}" | SP | HT
     */
    private static boolean isToken(final String text) {
        boolean token = true;
        if (text != null && text.length() > 0) {
            for (int i=0; i<text.length(); i++) {
                char character = text.charAt(i);
                if (character < 32 || character >= 127) {
                    token = false;
                    break;
                } else if (NON_TOKEN_CHARS.indexOf(character) > -1) {
                    token = false;
                    break;
                }
            }
        }
        return token;
    }

    private static String escapeQuotes(final String text) {
        String escaped = text;

        if (text != null && text.indexOf('"') > -1) {
            StringBuffer buffer = new StringBuffer(text.length() + 16);

            for (int i=0; i<text.length(); i++) {
                char character = text.charAt(i);
                if (character == '"') {
                    buffer.append("\\\"");
                } else {
                    buffer.append(character);
                }
            }

            escaped = buffer.toString();
        }

        return escaped;
    }

    private static String quoteIfNeeded(final String text) {
        String quoted = text;

        if (quoted==null) {
            quoted = "";
        } else if (!isToken(quoted)) {
            quoted = "\"" + escapeQuotes(quoted) + "\"";
        }

        return quoted;
    }
}
