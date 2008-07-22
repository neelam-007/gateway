package com.l7tech.common.http;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility methods for working with HttpCookies.
 *
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


    //Pattern is thread safe
    private static final Pattern netscapeDatePattern;
    private static final Pattern rfc850DatePattern;
    private static final Pattern rfc1123DatePattern;
    private static final Pattern rfc1036AndRfc822DatePattern;
    private static final Pattern ansiCDatePattern;
    private static final Pattern amazonDatePattern;
    
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
    
    private static Calendar calendar = Calendar.getInstance();

    static{
        //Order is based on most likely to least likely
        //This could live somewhere else, or driven from configuration. Either way compile the patterns once and reuse
        datePatternToFormat = new HashMap<Pattern, String>();
        datePatterns = new ArrayList<Pattern>();
        
        netscapeDatePattern = Pattern.compile(NETSCAPE_PATTERN);
        datePatterns.add(netscapeDatePattern);
        datePatternToFormat.put(netscapeDatePattern, NETSCAPE_RFC850_DATEFORMAT);

        rfc850DatePattern = Pattern.compile(RFC850_PATTERN);
        datePatterns.add(rfc850DatePattern);
        datePatternToFormat.put(rfc850DatePattern, NETSCAPE_RFC850_DATEFORMAT);

        rfc1123DatePattern = Pattern.compile(RFC1123_PATTERN);
        datePatterns.add(rfc1123DatePattern);
        datePatternToFormat.put(rfc1123DatePattern, RFC1123_RFC1036_RFC822_DATEFORMAT);

        rfc1036AndRfc822DatePattern = Pattern.compile(RFC1036_RFC822_PATTERN);
        datePatterns.add(rfc1036AndRfc822DatePattern);
        datePatternToFormat.put(rfc1036AndRfc822DatePattern, RFC1123_RFC1036_RFC822_DATEFORMAT);

        ansiCDatePattern = Pattern.compile(ANSI_C_PATTERN);
        datePatterns.add(ansiCDatePattern);
        datePatternToFormat.put(ansiCDatePattern, ANSI_C_DATEFORMAT);
        
        amazonDatePattern = Pattern.compile(AMAZON_PATTERN);
        datePatterns.add(amazonDatePattern);
        datePatternToFormat.put(amazonDatePattern, AMAZON_DATEFORMAT);
        
    }

    /*
    * @return an unmodifiable list of all the Pattern's we support for Cookie date formats
    * */
    public static List<Pattern> getDatePatterns(){
        return Collections.unmodifiableList(datePatterns);
    }

    /*
    * Using the List<Pattern> returned from getDatePatterns, look up the
    * String format to use with SimpleDateFormat.
    * @param p the Pattern we want a String format for
    * @return the String which can be used to a parse a date with the pattern p
    * */
    public static String getDateFormat(Pattern p){
        return datePatternToFormat.get(p);
    }

    /*
    * SimpleDateFormat must calculates 2 digit years relative to some century
    * It does this by adjusting the date to be within 80 years before and 20 years after the date
    * As many cookies are set with expiries well into the future we need to check if the year
    * is two digits. If it is expand it with the first two digits of the current century
    * */
    public static String expandYear(String pattern, String expiry){
        if(pattern.equals(CookieUtils.RFC850_PATTERN)){
            //Sunday, 06-Nov-38 08:49:37 GMT expand to Sunday, 06-Nov-2038 08:49:37
            int start = expiry.lastIndexOf("-");
            String newDate = expiry.substring(0, start+1);
            int year = calendar.get(Calendar.YEAR);
            String yearStr = (new Integer(year)).toString();
            String twoDigits = yearStr.substring(0,2);
            newDate += twoDigits + expiry.substring(start+1, expiry.length());
            return newDate;
        }else if(pattern.equals(CookieUtils.RFC1036_RFC822_PATTERN)){
            //Sun, 06 Nov 38 08:49:37 GMT expand to Sun, 06 Nov 2038 08:49:37 GMT
            int yearStart = 12;
            String newDate = expiry.substring(0, yearStart);
            int year = calendar.get(Calendar.YEAR);
            String yearStr = (new Integer(year)).toString();
            String twoDigits = yearStr.substring(0,2);
            newDate += twoDigits + expiry.substring(yearStart, expiry.length());
            return newDate;
        }
        return expiry;
    }
    /**
     * <p>Is the given cookie a gateway managed cookie?</p>
     *
     * <p>If a cookie is gateway managed it should not be passed through the gateway.</p>
     *
     * @param cookie the cookie to check.
     * @return true if gateway managed
     */
    public static boolean isGatewayManagedCookie(HttpCookie cookie) {
        boolean managed = false;
        String name = cookie==null ? null : cookie.getCookieName();

        if(name!=null) {
            if(name.startsWith(PREFIX_GATEWAY_MANAGED)) managed = true;
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
        boolean passthrough = false;

        passthrough = !isGatewayManagedCookie(cookie);

        return passthrough;
    }

    /**
     * <p>Ensures that the given cookie is valid to be returned from the given domain and path.</p>
     *
     * <p>If the cookie is valid then it is returned, else a new cookie is created with the same
     * values but a modified domain and/or path.</p>
     *
     * @param cookie the cookie to check
     * @param domain the cookies target domain
     * @param path the cookies target path (not that the path is trimmed up to and including the last /)
     * @return a valid cookie
     */
    public static HttpCookie ensureValidForDomainAndPath(HttpCookie cookie, String domain, String path) {
        HttpCookie result = cookie;

        if(result!=null) {
            String cookieDomain = cookie.getDomain();
            String cookiePath = cookie.getPath();

            String calcPath = path;
            int trim = calcPath.lastIndexOf('/');
            if(trim>0) {
                calcPath = calcPath.substring(0, trim);
            }

            if((cookieDomain!=null && !domain.endsWith(cookieDomain))
            || (cookiePath!=null && !calcPath.startsWith(cookiePath))){
                result = new HttpCookie(cookie, domain, calcPath);
            }
        }

        return result;
    }

    /**
     * <p>Convert the given cookie to an HTTP Client cookies for use in a HTTP request.</p>
     *
     * <p>Note that you may need to upate the domain/path to match your request URL (or
     * set to null).</p>
     *
     * @param httpCookie the cookie to convert.
     * @return the HTTP Client cookie.
     */
    public static org.apache.commons.httpclient.Cookie toHttpClientCookie(HttpCookie httpCookie) {
        org.apache.commons.httpclient.Cookie cookie = new org.apache.commons.httpclient.Cookie();

        cookie.setName(httpCookie.getCookieName());
        cookie.setValue(httpCookie.getCookieValue());
        cookie.setVersion(httpCookie.getVersion());
        cookie.setDomainAttributeSpecified(httpCookie.isDomainExplicit());
        cookie.setDomain(httpCookie.getDomain());
        cookie.setPath(httpCookie.getPath());
        cookie.setComment(httpCookie.getComment());
        cookie.setSecure(httpCookie.isSecure());
        if(httpCookie.hasExpiry()) cookie.setExpiryDate(new Date(httpCookie.getExpiryTime()));

        return cookie;
    }

    /**
     * <p>Create an HttpCookie from the given HTTP Client cookie.</p>
     *
     * @param httpClientCookie the Servlet cookie.
     * @param isNew true if this is a "new" cookie (as though from a Set-Cookie header)
     * @return the HttpCookie.
     */
    public static HttpCookie fromHttpClientCookie(org.apache.commons.httpclient.Cookie httpClientCookie, boolean isNew) {
        HttpCookie cookie = null;

        if(isNew) {
            cookie = new HttpCookie(httpClientCookie.getName()
                                   ,httpClientCookie.getValue()
                                   ,httpClientCookie.getVersion()
                                   ,httpClientCookie.isPathAttributeSpecified() ? httpClientCookie.getPath() : null
                                   ,httpClientCookie.isDomainAttributeSpecified() ? httpClientCookie.getDomain() : null
                                   ,httpClientCookie.getExpiryDate() == null ? -1 : (int)((httpClientCookie.getExpiryDate().getTime()-System.currentTimeMillis())/1000L)
                                   ,httpClientCookie.getSecure()
                                   ,httpClientCookie.getComment());
        }
        else {
            cookie = new HttpCookie(httpClientCookie.getName()
                                   ,httpClientCookie.getValue()
                                   ,httpClientCookie.getVersion()
                                   ,httpClientCookie.getPath()
                                   ,httpClientCookie.getDomain());
        }

        return cookie;
    }

    /**
     * <p>Convert the given cookie to a Servlet cookie for use in an HTTP response.</p>
     *
     * <p>Note that you may need to update the domain/path to match the request URL.</p>
     *
     * @param httpCookie the cookie to convert.
     * @return the Servlet cookie.
     */
    public static javax.servlet.http.Cookie toServletCookie(HttpCookie httpCookie) {
        javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie(httpCookie.getCookieName(), httpCookie.getCookieValue());

        cookie.setVersion(httpCookie.getVersion());
        if(httpCookie.isDomainExplicit()) cookie.setDomain(httpCookie.getDomain());
        cookie.setPath(httpCookie.getPath());
        cookie.setComment(httpCookie.getComment());
        cookie.setSecure(httpCookie.isSecure());
        if(httpCookie.hasExpiry()) cookie.setMaxAge(httpCookie.getMaxAge());

        return cookie;
    }

    /**
     * <p>Create an HttpCookie from the given Servlet cookie.</p>
     *
     * @param servletCookie the Servlet cookie.
     * @param isNew true if this is a "new" cookie (as though from a Set-Cookie header)
     * @return the HttpCookie.
     */
    public static HttpCookie fromServletCookie(javax.servlet.http.Cookie servletCookie, boolean isNew) {
        HttpCookie cookie = null;

        if(isNew) {
            cookie = new HttpCookie(servletCookie.getName()
                                   ,servletCookie.getValue()
                                   ,servletCookie.getVersion()
                                   ,servletCookie.getPath()
                                   ,servletCookie.getDomain()
                                   ,servletCookie.getMaxAge()
                                   ,servletCookie.getSecure()
                                   ,servletCookie.getComment());
        }
        else {
            cookie = new HttpCookie(servletCookie.getName()
                                   ,servletCookie.getValue()
                                   ,servletCookie.getVersion()
                                   ,servletCookie.getPath()
                                   ,servletCookie.getDomain());
        }

        return cookie;
    }

    /**
     * Convert a Servlet cookie to an HttpClient cookie.
     *
     * @param servletCookie the cookie to convert
     * @return the HTTP Client cookie
     */
    public static org.apache.commons.httpclient.Cookie servletCookieToHttpClientCookie(javax.servlet.http.Cookie servletCookie) {
        org.apache.commons.httpclient.Cookie c = new org.apache.commons.httpclient.Cookie();

        c.setName(servletCookie.getName());
        c.setValue(servletCookie.getValue());
        c.setPath(servletCookie.getPath());
        c.setDomain(servletCookie.getDomain());
        c.setVersion(servletCookie.getVersion());
        c.setComment(servletCookie.getComment());
        if (servletCookie.getMaxAge() >= 0)
            c.setExpiryDate(new Date(System.currentTimeMillis() + (servletCookie.getMaxAge() * 1000L)));

        return c;
    }

    //- PRIVATE

    /**
     * No instances
     */
    private CookieUtils() {
    }
}
