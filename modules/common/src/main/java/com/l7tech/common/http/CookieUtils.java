package com.l7tech.common.http;

import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for working with HttpCookies.
 * <p/>
 * User: steve
 * Date: Sep 27, 2005
 * Time: 1:18:52 PM
 * $Id$
 */
public class CookieUtils {

    //- PUBLIC

    /**
     * Cookie name prefix for cookies managed (owned) by the gateway.
     */
    public static final String PREFIX_GATEWAY_MANAGED = "l7-gmc-";

    private static final Map<Pattern, String> datePatternToFormat;

    /*NETSCAPE_RFC850_DATEFORMAT matches both netscape and rfc1123 date formats for parsing dates*/
    public static final String NETSCAPE_RFC850_DATEFORMAT = "EEE, dd-MMM-yy HH:mm:ss z";
    /*RFC1123_RFC1036_RFC822_DATEFORMAT matches all rfc's in it's name. Wont work if setLenient(false)
    * is called on the SimpleDateFormat instance used to parse*/
    public static final String RFC1123_RFC1036_RFC822_DATEFORMAT = "EEE, dd MMM yyyy HH:mm:ss z";
    public static final String ANSI_C_DATEFORMAT = "EEE MMM dd HH:mm:ss yyyy";
    public static final String AMAZON_DATEFORMAT = "EEE MMM dd HH:mm:ss yyyy z";

    /*datePatterns is a list which is ordered in the static initializer below.
    * No other mechanism is proivded for iteration so a client will go through our List in the order we specify here
    * This way we ensure that the dates are checked with the most common formats first
    */
    private static List<Pattern> datePatterns;

    public static final String NETSCAPE_PATTERN = "[a-zA-Z]{3},\\s[0-9]{2}-[a-zA-Z]{3}-[0-9]{4}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}\\s[a-zA-Z]{3}";
    public static final String RFC850_PATTERN = "(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday){1},\\s[0-9]{2}-[a-zA-Z]{3}-[0-9]{2}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}\\s[a-zA-Z]{3}";
    public static final String RFC1123_PATTERN = "[a-zA-Z]{3},\\s[0-9]{2}\\s[a-zA-Z]{3}\\s[0-9]{4}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}\\s[a-zA-Z]{3}";
    public static final String RFC1036_RFC822_PATTERN = "[a-zA-Z]{3},\\s[0-9]{2}\\s[a-zA-Z]{3}\\s[0-9]{2}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}\\s[a-zA-Z]{3}";
    public static final String ANSI_C_PATTERN = "[a-zA-Z]{3}\\s[a-zA-Z]{3}\\s([0-9]{2}|\\s\\d){1}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}\\s[0-9]{4}";
    public static final String AMAZON_PATTERN = "[a-zA-Z]{3}\\s[a-zA-Z]{3}\\s[0-9]{2}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}\\s[0-9]{4}\\s[a-zA-Z]{3}";

    public static final String DOMAIN = "Domain";
    public static final String PATH = "Path";
    public static final String COMMENT = "Comment";
    public static final String VERSION = "Version";
    public static final String MAX_AGE = "Max-Age";
    public static final String SECURE = "Secure";
    public static final String HTTP_ONLY = "HttpOnly";
    public static final String EXPIRES = "Expires";
    public static final String ATTRIBUTE_DELIMITER = "; ";
    public static final String EQUALS = "=";
    private static final int UNSPECIFIED_MAX_AGE = -1;

    private static final String NON_TOKEN_CHARS = ",; ";

    private static Calendar calendar = Calendar.getInstance();

    static {
        //Order is based on most likely to least likely
        //This could live somewhere else, or driven from configuration. Either way compile the patterns once and reuse
        datePatternToFormat = new HashMap<Pattern, String>();
        datePatterns = new ArrayList<Pattern>();

        Pattern netscapeDatePattern = Pattern.compile(NETSCAPE_PATTERN);
        datePatterns.add(netscapeDatePattern);
        datePatternToFormat.put(netscapeDatePattern, NETSCAPE_RFC850_DATEFORMAT);

        Pattern rfc850DatePattern = Pattern.compile(RFC850_PATTERN);
        datePatterns.add(rfc850DatePattern);
        datePatternToFormat.put(rfc850DatePattern, NETSCAPE_RFC850_DATEFORMAT);

        Pattern rfc1123DatePattern = Pattern.compile(RFC1123_PATTERN);
        datePatterns.add(rfc1123DatePattern);
        datePatternToFormat.put(rfc1123DatePattern, RFC1123_RFC1036_RFC822_DATEFORMAT);

        Pattern rfc1036AndRfc822DatePattern = Pattern.compile(RFC1036_RFC822_PATTERN);
        datePatterns.add(rfc1036AndRfc822DatePattern);
        datePatternToFormat.put(rfc1036AndRfc822DatePattern, RFC1123_RFC1036_RFC822_DATEFORMAT);

        Pattern ansiCDatePattern = Pattern.compile(ANSI_C_PATTERN);
        datePatterns.add(ansiCDatePattern);
        datePatternToFormat.put(ansiCDatePattern, ANSI_C_DATEFORMAT);

        Pattern amazonDatePattern = Pattern.compile(AMAZON_PATTERN);
        datePatterns.add(amazonDatePattern);
        datePatternToFormat.put(amazonDatePattern, AMAZON_DATEFORMAT);
    }

    /*
    * @return an unmodifiable list of all the Pattern's we support for Cookie date formats
    * */
    public static List<Pattern> getDatePatterns() {
        return Collections.unmodifiableList(datePatterns);
    }

    /*
    * Using the List<Pattern> returned from getDatePatterns, look up the
    * String format to use with SimpleDateFormat.
    * @param p the Pattern we want a String format for
    * @return the String which can be used to a parse a date with the pattern p
    * */
    public static String getDateFormat(Pattern p) {
        return datePatternToFormat.get(p);
    }

    /*
    * SimpleDateFormat must calculates 2 digit years relative to some century
    * It does this by adjusting the date to be within 80 years before and 20 years after the date
    * As many cookies are set with expiries well into the future we need to check if the year
    * is two digits. If it is expand it with the first two digits of the current century
    * */
    public static String expandYear(String pattern, String expiry) {
        if (pattern.equals(CookieUtils.RFC850_PATTERN)) {
            //Sunday, 06-Nov-38 08:49:37 GMT expand to Sunday, 06-Nov-2038 08:49:37
            int start = expiry.lastIndexOf("-");
            String newDate = expiry.substring(0, start + 1);
            int year = calendar.get(Calendar.YEAR);
            String yearStr = (new Integer(year)).toString();
            String twoDigits = yearStr.substring(0, 2);
            newDate += twoDigits + expiry.substring(start + 1, expiry.length());
            return newDate;
        } else if (pattern.equals(CookieUtils.RFC1036_RFC822_PATTERN)) {
            //Sun, 06 Nov 38 08:49:37 GMT expand to Sun, 06 Nov 2038 08:49:37 GMT
            int yearStart = 12;
            String newDate = expiry.substring(0, yearStart);
            int year = calendar.get(Calendar.YEAR);
            String yearStr = (new Integer(year)).toString();
            String twoDigits = yearStr.substring(0, 2);
            newDate += twoDigits + expiry.substring(yearStart, expiry.length());
            return newDate;
        }
        return expiry;
    }

    /**
     * <p>Is the given cookie a gateway managed cookie?</p>
     * <p/>
     * <p>If a cookie is gateway managed it should not be passed through the gateway.</p>
     *
     * @param cookie the cookie to check.
     * @return true if gateway managed
     */
    public static boolean isGatewayManagedCookie(HttpCookie cookie) {
        boolean managed = false;
        String name = cookie == null ? null : cookie.getCookieName();

        if (name != null) {
            if (name.startsWith(PREFIX_GATEWAY_MANAGED)) managed = true;
        }

        return managed;
    }

    /**
     * Should the given cookie be passed through the gateway?
     *
     * @param cookie the cookie to check.
     * @return true if passed though
     */
    public static boolean isPassThroughCookie(HttpCookie cookie) {
        boolean passthrough;

        passthrough = !isGatewayManagedCookie(cookie);

        return passthrough;
    }

    /**
     * <p>Ensures that the given cookie is valid to be returned from the given domain and path.</p>
     * <p/>
     * <p>If the cookie is valid then it is returned, else a new cookie is created with the same
     * values but a modified domain and/or path.</p>
     *
     * @param cookie the cookie to check
     * @param domain the cookies target domain. If null, the cookie domain will be used for the return cookie domain.
     * @param path   the cookies target path (not that the path is trimmed up to and including the last /)
     *               If null, the cookie path will be used for the return cookie path.
     * @return a valid cookie
     */
    public static HttpCookie ensureValidForDomainAndPath(HttpCookie cookie, String domain, String path) {
        HttpCookie result = cookie;

        if (result != null) {
            String cookieDomain = cookie.getDomain();
            String cookiePath = cookie.getPath();

            String calcPath = path;
            if (calcPath == null) {
                calcPath = cookiePath;
            }

            if (calcPath != null) {
                int trim = calcPath.lastIndexOf('/');
                if (trim > 0) {
                    calcPath = calcPath.substring(0, trim);
                }
            }

            if ((cookieDomain != null && domain != null && !domain.endsWith(cookieDomain))
                    || (cookiePath != null && calcPath != null && !calcPath.startsWith(cookiePath))) {
                result = new HttpCookie(cookie, domain != null ? domain : cookieDomain, calcPath);
            }
        }

        return result;
    }

    public static String replaceCookieDomainAndPath(String value, String domain, String path, boolean modify) {
        String s = value;
        if(StringUtils.isNotBlank(s)) {
            if (domain != null) {
                String cookieDomain = findMatchingValue(domainPattern.matcher(s));
                if(cookieDomain != null && !domain.endsWith(cookieDomain)) {
                    s = replaceMatchedValue(domainPattern.matcher(s), s, DOMAIN + EQUALS + domain, modify);
                }
            }
            if (path != null) {
                String calcPath = path;
                String cookiePath = findMatchingValue(pathPattern.matcher(s));
                if(calcPath == null) {
                    calcPath = cookiePath;
                }

                if (calcPath != null) {
                    int trim = calcPath.lastIndexOf('/');
                    if (trim > 0) {
                        calcPath = calcPath.substring(0, trim);
                    }
                }
                if(cookiePath != null && calcPath != null && !calcPath.startsWith(cookiePath)) {
                    s = replaceMatchedValue(pathPattern.matcher(s), s, PATH + EQUALS + calcPath, modify);
                }
            }
        }
        return s;
    }

    private static String findMatchingValue(@NotNull Matcher matcher) {
        String value = null;

        if(matcher.find()){
            value = matcher.group(2);
        }

        return value;
    }

    /**
     * <p>Convert the given cookie to an HTTP Client cookies for use in a HTTP request.</p>
     * <p/>
     * <p>Note that you may need to upate the domain/path to match your request URL (or
     * set to null).</p>
     *
     * @param httpCookie the cookie to convert.
     * @param prefix     the prefix to remove.
     * @return the HTTP Client cookie.
     */
    public static Cookie toHttpClientCookie(HttpCookie httpCookie, String prefix) {

        BasicClientCookie cookie = null;

        if (prefix == null) {
            cookie = new BasicClientCookie(httpCookie.getCookieName(), httpCookie.getCookieValue());
        } else {
            cookie = new BasicClientCookie(httpCookie.getCookieName().substring(prefix.length()), httpCookie.getCookieValue());
        }

        cookie.setValue(httpCookie.getCookieValue());
        cookie.setVersion(httpCookie.getVersion());
        cookie.setDomain(httpCookie.getDomain());
        cookie.setPath(httpCookie.getPath());
        cookie.setComment(httpCookie.getComment());
        cookie.setSecure(httpCookie.isSecure());
        if (httpCookie.hasExpiry()) cookie.setExpiryDate(new Date(httpCookie.getExpiryTime()));

        return cookie;
    }

    /**
     * <p>Create an HttpCookie from the given HTTP Client cookie.</p>
     *
     * @param httpClientCookie the Servlet cookie.
     * @param isNew            true if this is a "new" cookie (as though from a Set-Cookie header)
     * @return the HttpCookie.
     */
    public static HttpCookie fromHttpClientCookie(Cookie httpClientCookie, boolean isNew) {
        HttpCookie cookie;

        if (isNew) {
            cookie = new HttpCookie(httpClientCookie.getName()
                    , httpClientCookie.getValue()
                    , httpClientCookie.getVersion()
                    , httpClientCookie.getPath()
                    , httpClientCookie.getDomain()
                    , httpClientCookie.getExpiryDate() == null ? -1 : (int) ((httpClientCookie.getExpiryDate().getTime() - System.currentTimeMillis()) / 1000L)
                    , httpClientCookie.isSecure()
                    , httpClientCookie.getComment()
                    , false); //httpclient 4.2.5 doesn't support httpOnly.
        } else {
            cookie = new HttpCookie(httpClientCookie.getName()
                    , httpClientCookie.getValue()
                    , httpClientCookie.getVersion()
                    , httpClientCookie.getPath()
                    , httpClientCookie.getDomain());
        }

        return cookie;
    }

    /**
     * <p>Convert the given cookie to a Servlet cookie for use in an HTTP response.</p>
     * <p/>
     * <p>Note that you may need to update the domain/path to match the request URL.</p>
     *
     * @param httpCookie the cookie to convert.
     * @return the Servlet cookie.
     */
    public static javax.servlet.http.Cookie toServletCookie(HttpCookie httpCookie) {
        javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie(httpCookie.getCookieName(), httpCookie.getCookieValue());

        cookie.setVersion(httpCookie.getVersion());
        if (httpCookie.isDomainExplicit()) cookie.setDomain(httpCookie.getDomain());
        cookie.setPath(httpCookie.getPath());
        cookie.setComment(httpCookie.getComment());
        cookie.setSecure(httpCookie.isSecure());
        cookie.setHttpOnly(httpCookie.isHttpOnly());
        if (httpCookie.hasExpiry()) cookie.setMaxAge(httpCookie.getMaxAge());

        return cookie;
    }

    /**
     * <p>Create an HttpCookie from the given Servlet cookie.</p>
     *
     * @param servletCookie the Servlet cookie.
     * @param isNew         true if this is a "new" cookie (as though from a Set-Cookie header)
     * @return the HttpCookie.
     */
    public static HttpCookie fromServletCookie(javax.servlet.http.Cookie servletCookie, boolean isNew) {
        HttpCookie cookie;

        if (isNew) {
            cookie = new HttpCookie(servletCookie.getName()
                    , servletCookie.getValue()
                    , servletCookie.getVersion()
                    , servletCookie.getPath()
                    , servletCookie.getDomain()
                    , servletCookie.getMaxAge()
                    , servletCookie.getSecure()
                    , servletCookie.getComment()
                    , servletCookie.isHttpOnly());
        } else {
            cookie = new HttpCookie(servletCookie.getName()
                    , servletCookie.getValue()
                    , servletCookie.getVersion()
                    , servletCookie.getPath()
                    , servletCookie.getDomain());
        }

        return cookie;
    }

    /**
     * <p>Creates HttpCookies from the given Servlet cookies.</p>
     *
     * @param servletCookies the Servlet cookies (may be null).
     * @param isNew          true if these are "new" cookies (as though from a Set-Cookie header)
     * @return the HttpCookies (may be empty, never null).
     */
    public static HttpCookie[] fromServletCookies(javax.servlet.http.Cookie[] servletCookies, boolean isNew) {
        List<HttpCookie> out = new ArrayList<HttpCookie>();

        if (servletCookies != null) {
            for (javax.servlet.http.Cookie servletCookie : servletCookies) {
                out.add(CookieUtils.fromServletCookie(servletCookie, isNew));
            }
        }

        return out.toArray(new HttpCookie[out.size()]);
    }

    /**
     * Search a cookie array for a single cookie with the requested name.
     *
     * @param cookies    cookie array to search. Required.
     * @param cookieName name of cookie to search for.  Required.
     * @return a cookie with that name, or null.
     * @throws IOException if more than one cookie is found with that name.
     */
    public static HttpCookie findSingleCookie(HttpCookie[] cookies, String cookieName) throws IOException {
        HttpCookie found = null;
        for (HttpCookie cookie : cookies) {
            if (cookie.getCookieName().equalsIgnoreCase(cookieName)) {
                if (found == null)
                    found = cookie;
                else
                    throw new IOException("More than one cookie found named " + cookieName);
            }
        }
        return found;
    }

    /**
     * Check if the text is a "token" or if it contains any illegal characters.
     * <p/>
     * NOTE: According to the RFC all the characters below are non-token chars,
     * Tomcat just checks for ",; " and < 32 >= 127, so we are doing the same.
     * <p/>
     * 0 - 31, 127
     * "(" | ")" | "<" | ">" | "@"
     * | "," | ";" | ":" | "\" | <">
     * | "/" | "[" | "]" | "?" | "="
     * | "{" | "}" | SP | HT
     *
     * @param text the String to check for illegal cookie value characters.
     * @return true if the text contains illegal cookie value characters or is null or is empty; false otherwise.
     */
    public static boolean isToken(final String text) {
        boolean token = true;
        if (text != null && text.length() > 0) {
            for (int i = 0; i < text.length(); i++) {
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

    /**
     * Get the cookies as a string (as in a "Cookie:" header).
     * <p/>
     * NOTE: Since we may have modified the path/domain of the cookie when
     * proxying it seems safest to drop this from the header (see RFC 2109
     * section 4.3.4)
     *
     * @param cookies                The collection of HttpCookie's to add
     * @return a string like "foo=bar; baz=blat; bloo=blot".  May be empty, but never null.
     */
    public static String getCookieHeader(final Collection<HttpCookie> cookies) {
        StringBuffer sb = new StringBuffer();

        if (cookies != null) {
            for (Iterator cookIter = cookies.iterator(); cookIter.hasNext(); ) {
                HttpCookie cook = (HttpCookie) cookIter.next();
                // always use V0, see note above
                sb.append(getV0CookieHeaderPart(cook));
                if (cookIter.hasNext())
                    sb.append("; ");
            }
        }

        return sb.toString();
    }

    /**
     * Get this cookie formatted as part of a version 0 (Netscape) "cookie:"
     * header.
     *
     * @return "<Name>=<Value>"
     */
    public static String getV0CookieHeaderPart(@NotNull final HttpCookie cookie) {
        StringBuffer headerPart = new StringBuffer();
        headerPart.append(cookie.getCookieName());
        headerPart.append('=');
        headerPart.append(cookie.getCookieValue());
        return headerPart.toString();
    }

    /**
     * Get a Set-Cookie header representation of the given cookie.
     * <p/>
     * Ex) name=value; Version=1; Domain=localhost; Path=/
     *
     * @param cookie the HttpCookie.
     * @return a Set-Cookie header representation of the given cookie.
     */
    public static String getSetCookieHeader(@NotNull final HttpCookie cookie) {
        final StringBuilder sb = new StringBuilder();
        sb.append(cookie.getCookieName()).append(EQUALS).append(cookie.getCookieValue());
        sb.append(ATTRIBUTE_DELIMITER).append(VERSION).append(EQUALS).append(cookie.getVersion());
        appendIfNotBlank(sb, DOMAIN, cookie.getDomain());
        appendIfNotBlank(sb, PATH, cookie.getPath());
        appendIfNotBlank(sb, EXPIRES, cookie.getExpires());
        appendIfNotBlank(sb, COMMENT, cookie.getComment());
        if (cookie.getExpires() == null && cookie.getMaxAge() != UNSPECIFIED_MAX_AGE) {
            sb.append(ATTRIBUTE_DELIMITER).append(MAX_AGE).append(EQUALS).append(cookie.getMaxAge());
        }
        if (cookie.isSecure()) {
            sb.append(ATTRIBUTE_DELIMITER).append(SECURE);
        }
        if (cookie.isHttpOnly()) {
            sb.append(ATTRIBUTE_DELIMITER).append(HTTP_ONLY);
        }
        return sb.toString();
    }

    public static String[] splitAttributes(String value) {
        List<String> attributes = new ArrayList<>();
        Matcher m = attributePattern.matcher(value);
        int i = 0;
        while(m.find()) {
            final int pos = m.start();
            attributes.add(value.substring(i,pos).trim());
            i = pos + 1;
        }
        attributes.add(value.substring(i).trim());
        return attributes.toArray(new String[0]);
    }

    public static String addCookieDomainAndPath(String value, String domain, String path) {
        String s = value;
        if(StringUtils.isNotBlank(s) && firstAttributePattern.matcher(value).find()) {
            if (domain != null) {
                s = findOrAppend(domainPattern.matcher(s), s, domain);
            }
            if (path != null) {
                s = findOrAppend(pathPattern.matcher(s), s, path);
            }
        }
        return s;
    }

    public static List<String> splitCookieHeader(final String cookieValue) {
        // it is possible for multiple cookies to be stored in a single Cookie header in multiple formats
        // ex) Cookie: 1=one; 2=two                         ---> Netscape format
        // ex) Cookie: $Version=1; 1=one; $Version=1; 2=two ---> RFC2109
        // ex) Cookie: 1=one; $Version=1; 2=two; $Version=1 ---> RFC2109 except Version is after name=value
        final String[] tokens = splitAttributes(cookieValue);
        final List<String> singleCookieValues = new ArrayList<>();
        if (tokens.length > 0) {
            final List<String> group = new ArrayList<>();
            final String firstToken = tokens[0].trim();
            group.add(firstToken);
            boolean hasVersion = isVersion(firstToken);
            boolean hasName = !isCookieAttribute(firstToken);
            for (int i = 1; i < tokens.length; i++) {
                final String token = tokens[i].trim();
                if ((hasVersion && isVersion(token)) || (hasName && !isCookieAttribute(token))) {
                    // current token is the start of a new cookie, so process the existing token group
                    Collections.sort(group, COOKIE_ATTRIBUTE_COMPARATOR);
                    singleCookieValues.add(StringUtils.join(group.toArray(new String[group.size()]), ATTRIBUTE_DELIMITER));
                    group.clear();
                    hasVersion = false;
                    hasName = false;
                }
                group.add(token);
                if (isVersion(token)) {
                    hasVersion = true;
                } else if (!isCookieAttribute(token)) {
                    hasName = true;
                }
            }
            // process last group
            Collections.sort(group, COOKIE_ATTRIBUTE_COMPARATOR);
            singleCookieValues.add(StringUtils.join(group.toArray(new String[group.size()]), ATTRIBUTE_DELIMITER));
        }
        return singleCookieValues;
    }

    /**
     * @return true if the given token identifies the cookie version.
     */
    public static boolean isVersion(final String token) {
        boolean isVersion = false;
        final String[] split = token.split(EQUALS);
        if (split.length > 0) {
            String candidate = split[0];
            if (candidate.startsWith(DOLLAR_SIGN)) {
                candidate = StringUtils.substring(candidate, 1);
            }
            if (candidate.equalsIgnoreCase(VERSION)) {
                isVersion = true;
            }
        }
        return isVersion;
    }

    /**
     * @return true if the given token is a recognized cookie attribute (or attribute=value pair).
     */
    public static boolean isCookieAttribute(final String token) {
        boolean isAttribute = false;
        final String[] split = token.split(EQUALS);
        if (split.length > 0) {
            String candidate = split[0];
            if (candidate.startsWith(DOLLAR_SIGN)) {
                candidate = StringUtils.substring(candidate, 1);
            }
            isAttribute = COOKIE_ATTRIBUTES.contains(candidate.toLowerCase());
        }
        return isAttribute;
    }

    private static String replaceMatchedValue(@NotNull Matcher m, @NotNull String value, @Nullable String replacement, boolean modify) {
        StringBuffer sb = new StringBuffer();
        int i = 0;
        if(m.find()) {
            sb.append(value.substring(i,m.start(1))).append(replacement);
            i = m.end(1);
        }
        if(sb.length() == 0) {
            sb.append(value);
            if(modify) {
                if(value.lastIndexOf(ATTRIBUTE_DELIMITER.trim()) < value.trim().length() - 1 ) sb.append(ATTRIBUTE_DELIMITER); //check if cookie ends with ;
                sb.append(replacement);
            }
        }
        else if(i < value.length() - 1){
            sb.append(value.substring(i));
        }
        return sb.toString();
    }

    private static String findOrAppend(@NotNull Matcher m, @NotNull String value, @Nullable String replacement) {
        StringBuffer sb = new StringBuffer();
        sb.append(value);
        if(!m.find()) {
            if(value.lastIndexOf(ATTRIBUTE_DELIMITER.trim()) < value.trim().length() - 1 )
                sb.append(ATTRIBUTE_DELIMITER); //check if cookie ends with ;
            sb.append(replacement);
        }

        return sb.toString();
    }

    private static void appendIfNotBlank(final StringBuilder stringBuilder, final String attributeName, final String attributeValue) {
        if (StringUtils.isNotBlank(attributeValue)) {
            stringBuilder.append(ATTRIBUTE_DELIMITER).append(attributeName).append(EQUALS).append(attributeValue);
        }
    }

    //- PRIVATE
    private static final String DOMAIN_PATTERN = "(?:;\\s*)(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)(domain\\s*=\\s*\"{0,1}([\\w[^;\\s\"]]+)\"{0,1})\\s*";
    private static final String PATH_PATTERN = "(?:;\\s*)(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)(path\\s*=\\s*\"{0,1}([\\w[^;]]+)\"{0,1})\\s*";
    private static final String FIRST_ATTRIBUTE_PATTERN = "^[\\w\\$-]*\\s*=\\s*(?:[\\w[^;]]+)";
    private static final String COOKIE_ATTRIBUTE_PATTERN = "(?:;\\s*)(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)([\\w\\$-,]*)(\\s*=\\s*\"{0,1}([\\w[^;\\s\"]]+)\"{0,1})*";
    private static final Pattern domainPattern = Pattern.compile(DOMAIN_PATTERN, Pattern.CASE_INSENSITIVE);
    private static final Pattern pathPattern = Pattern.compile(PATH_PATTERN,Pattern.CASE_INSENSITIVE);
    private static final Pattern attributePattern = Pattern.compile(COOKIE_ATTRIBUTE_PATTERN, Pattern.CASE_INSENSITIVE);
    private static final Pattern firstAttributePattern = Pattern.compile(FIRST_ATTRIBUTE_PATTERN, Pattern.CASE_INSENSITIVE);
    private static final Comparator<String> COOKIE_ATTRIBUTE_COMPARATOR = ComparatorUtils.nullHighComparator(new CookieTokenComparator());
    private static final String DOLLAR_SIGN = "$";
    private static final Set<String> COOKIE_ATTRIBUTES;

    static {
        COOKIE_ATTRIBUTES = new HashSet<>();
        COOKIE_ATTRIBUTES.add(DOMAIN.toLowerCase());
        COOKIE_ATTRIBUTES.add(PATH.toLowerCase());
        COOKIE_ATTRIBUTES.add(COMMENT.toLowerCase());
        COOKIE_ATTRIBUTES.add(VERSION.toLowerCase());
        COOKIE_ATTRIBUTES.add(MAX_AGE.toLowerCase());
        COOKIE_ATTRIBUTES.add(SECURE.toLowerCase());
        COOKIE_ATTRIBUTES.add(HTTP_ONLY.toLowerCase());
        COOKIE_ATTRIBUTES.add(EXPIRES.toLowerCase());
    }

    /**
     * No instances
     */
    private CookieUtils() {
    }

    /**
     * Comparator which evaluates name=value strings as lower than other known cookie attribute pairs.
     */
    private static class CookieTokenComparator implements Comparator<String> {
        @Override
        public int compare(final String token1, final String token2) {
            return ComparatorUtils.booleanComparator(true).compare(!isCookieAttribute(token1), !isCookieAttribute(token2));
        }
    }
}
