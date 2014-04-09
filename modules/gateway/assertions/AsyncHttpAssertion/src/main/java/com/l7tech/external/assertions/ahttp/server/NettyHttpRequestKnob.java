package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.mime.MimeHeader;
import com.l7tech.message.HttpRequestKnobAdapter;
import com.l7tech.util.ISO8601Date;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.HttpRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE;

/**
*
*/
class NettyHttpRequestKnob extends HttpRequestKnobAdapter {

    private final HttpRequest httpRequest;
    private final InetSocketAddress clientAddress;
    private final InetSocketAddress serverAddress;
    private final URL requestUrl;
    private HttpCookie[] cookies;

    NettyHttpRequestKnob(HttpRequest httpRequest, InetSocketAddress clientAddress, InetSocketAddress serverAddress, URL requestUrl) {
        this.httpRequest = httpRequest;
        this.clientAddress = clientAddress;
        this.serverAddress = serverAddress;
        this.requestUrl = requestUrl;
    }

    @Override
    public String getHeaderSingleValue(String name) throws IOException {
        return httpRequest.getHeader(name);
    }

    @Override
    public String getSoapAction() {
        return getHeaderFirstValue("SOAPAction");
    }

    @Override
    public HttpCookie[] getCookies() {
        if (cookies == null) {
            Set<Cookie> allCookies = new LinkedHashSet<Cookie>();
            List<String> cookieHeaderValues = httpRequest.getHeaders(COOKIE);
            for (String cookieHeader : cookieHeaderValues) {
                CookieDecoder cookieDecoder = new CookieDecoder();
                Set<Cookie> cookies = cookieDecoder.decode(cookieHeader);
                allCookies.addAll(cookies);
            }

            cookies = new HttpCookie[allCookies.size()];
            int i = 0;
            for (Cookie c : allCookies) {
                // TODO double check that path and domain get populated correctly; may require getting the info from elsewhere
                HttpCookie cookie = new HttpCookie(c.getName(), c.getValue(), c.getVersion(), c.getPath(), c.getDomain(), c.getMaxAge(), c.isSecure(), c.getComment(), c.isHttpOnly());
                cookies[i++] = cookie;
            }
        }
        return cookies;
    }

    @Override
    public HttpMethod getMethod() {
        try {
            return HttpMethod.valueOf(httpRequest.getMethod().getName());
        } catch (IllegalArgumentException e) {
            return HttpMethod.OTHER;
        }
    }

    @Override
    public String getMethodAsString() {
        return httpRequest.getMethod().getName();
    }

    @Override
    public String getRequestUri() {
        return httpRequest.getUri();
    }

    @Override
    public String getRequestUrl() {
        return requestUrl.toString();
    }

    @Override
    public URL getRequestURL() {
        return requestUrl;
    }

    @Override
    public long getDateHeader(String name) throws ParseException {
        String value = getHeaderFirstValue(name);
        if (value == null)
            return -1;
        return ISO8601Date.parse(value).getTime();
    }

    @Override
    public int getIntHeader(String name) {
        String value = getHeaderFirstValue(name);
        if (value == null)
            return -1;
        try {
            long longVal = MimeHeader.parseNumericValue(name);
            if (longVal > (long)Integer.MAX_VALUE || longVal < (long)Integer.MIN_VALUE)
                return -1;
            return (int)longVal;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public String getHeaderFirstValue(String name) {
        return httpRequest.getHeader(name);
    }

    @Override
    public String[] getHeaderNames() {
        Set<String> headerNames = httpRequest.getHeaderNames();
        return headerNames.toArray(new String[headerNames.size()]);
    }

    @Override
    public String[] getHeaderValues(String name) {
        List<String> headers = httpRequest.getHeaders(name);
        return headers.toArray(new String[headers.size()]);
    }

    @Override
    public X509Certificate[] getClientCertificate() throws IOException {
        // TODO SSL/TLS support
        return null;
    }

    @Override
    public boolean isSecure() {
        // TODO SSL/TLS support
        return false;
    }

    @Override
    public String getParameter(String name) {
        // TODO query/body form post parameter support
        return null;
    }

    @Override
    public Map getParameterMap() {
        // TODO query/body form post parameter support
        return Collections.emptyMap();
    }

    @Override
    public String[] getParameterValues(String s) {
        // TODO query/body form post parameter support
        return new String[0];
    }

    @Override
    public Enumeration getParameterNames() {
        return Collections.emptyEnumeration();
    }

    @Override
    public String getQueryString() {
        return requestUrl.getQuery();
    }

    @Override
    public Object getConnectionIdentifier() {
        return new Object();
    }

    @Override
    public String getRemoteAddress() {
        return clientAddress.getAddress().getHostAddress();
    }

    @Override
    public String getRemoteHost() {
        // TODO support forcing a reverse lookup if enabled
        return clientAddress.getHostString();
    }

    @Override
    public int getRemotePort() {
        return clientAddress.getPort();
    }

    @Override
    public String getLocalAddress() {
        return serverAddress.getAddress().getHostAddress();
    }

    @Override
    public String getLocalHost() {
        return serverAddress.getHostString();
    }

    @Override
    public int getLocalPort() {
        return requestUrl.getPort();
    }

    @Override
    public int getLocalListenerPort() {
        return serverAddress.getPort();
    }
}
