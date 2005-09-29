package com.l7tech.common.http;

import java.util.Date;

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
