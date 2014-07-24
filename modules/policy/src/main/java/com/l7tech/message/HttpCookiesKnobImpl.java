package com.l7tech.message;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpCookie;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.common.http.CookieUtils.splitCookieHeader;
import static com.l7tech.message.HeadersKnob.HEADER_TYPE_HTTP;

/**
 * HttpCookiesKnob implementation which wraps a HeadersKnob.
 */
public class HttpCookiesKnobImpl implements HttpCookiesKnob {
    private static final Logger logger = Logger.getLogger(HttpCookiesKnobImpl.class.getName());

    private final HeadersKnob delegate;
    private final String cookieHeaderName;

    /**
     * Create a HeadersKnob-backed HttpCookiesKnob.
     *
     * @param delegate         the HeadersKnob which backs the HttpCookiesKnob.
     * @param cookieHeaderName the header name to use for cookies - ex) {@link HttpConstants#HEADER_COOKIE or HttpConstants#HEADER_SET_COOKIE}
     */
    public HttpCookiesKnobImpl(@NotNull final HeadersKnob delegate, @NotNull final String cookieHeaderName) {
        if (!HttpConstants.HEADER_COOKIE.equalsIgnoreCase(cookieHeaderName) && !HttpConstants.HEADER_SET_COOKIE.equalsIgnoreCase(cookieHeaderName)) {
            throw new IllegalArgumentException("Cookie header name should be either " + HttpConstants.HEADER_COOKIE + " or " + HttpConstants.HEADER_SET_COOKIE);
        }
        this.delegate = delegate;
        this.cookieHeaderName = cookieHeaderName;
    }

    /**
     * @return the header name being used for cookies - ex) {@link HttpConstants#HEADER_COOKIE or HttpConstants#HEADER_SET_COOKIE}
     */
    @NotNull
    public String getCookieHeaderName() {
        return cookieHeaderName;
    }

    @Override
    public Set<HttpCookie> getCookies() {
        final Set<HttpCookie> cookies = new LinkedHashSet<>();
        for (final String cookieValue : delegate.getHeaderValues(cookieHeaderName, HEADER_TYPE_HTTP)) {
            final List<String> singleCookieValues = splitCookieHeader(cookieValue);
            for (final String singleCookieValue : singleCookieValues) {
                try {
                    cookies.add(new HttpCookie(singleCookieValue));
                } catch (final HttpCookie.IllegalFormatException e) {
                    logger.log(Level.WARNING, "Could not process cookie value: " + cookieValue);
                }
            }
        }
        return cookies;
    }

    @Override
    public Set<String> getCookiesAsHeaders() {
        final Set<String> cookieHeaders = new LinkedHashSet<>();
        for (final String value : delegate.getHeaderValues(cookieHeaderName, HEADER_TYPE_HTTP)) {
            if (cookieHeaderName.equalsIgnoreCase(HttpConstants.HEADER_COOKIE)) {
                cookieHeaders.addAll(splitCookieHeader(value));
            } else {
                cookieHeaders.add(value);
            }
        }
        return cookieHeaders;
    }

    /**
     * Adds a cookie which is backed by a set-cookie format as this type of header contains all cookie attributes.
     *
     * @param cookie the HttpCookie to add.
     */
    @Override
    public void addCookie(@NotNull final HttpCookie cookie) {
        if (getCookies().contains(cookie)) {
            deleteCookie(cookie);
        }
        delegate.addHeader(cookieHeaderName, CookieUtils.getSetCookieHeader(cookie), HEADER_TYPE_HTTP);
    }

    @Override
    public void deleteCookie(@NotNull final HttpCookie cookie) {
        removeMatchingCookieHeaders(cookieHeaderName, cookie);
    }

    private void removeMatchingCookieHeaders(final String cookieHeaderName, final HttpCookie cookie) {
        for (final String cookieValue : delegate.getHeaderValues(cookieHeaderName, HEADER_TYPE_HTTP)) {
            final List<String> cookies = CookieUtils.splitCookieHeader(cookieValue);
            final List<String> toRemove = new ArrayList<>();
            for (final String cookieHeader : cookies) {
                try {
                    final HttpCookie fromHeader = new HttpCookie(cookieHeader);
                    if (cookie.getId().equals(fromHeader.getId())) {
                        toRemove.add(cookieHeader);
                    }
                } catch (final HttpCookie.IllegalFormatException e) {
                    logger.log(Level.WARNING, "Skipping invalid " + cookieHeaderName + " header: " + cookieValue);
                }
            }
            if (!toRemove.isEmpty()) {
                logger.log(Level.INFO, "Removing cookies: " + toRemove);
                cookies.removeAll(toRemove);
                delegate.removeHeader(cookieHeaderName, cookieValue, HEADER_TYPE_HTTP);
                if (!cookies.isEmpty()) {
                    delegate.addHeader(cookieHeaderName, StringUtils.join(cookies, "; "), HEADER_TYPE_HTTP);
                }
            }
        }
    }
}
