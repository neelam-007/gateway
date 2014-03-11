package com.l7tech.message;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpCookie;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HttpCookiesKnob implementation which wraps a HeadersKnob.
 */
public class HttpCookiesKnobImpl implements HttpCookiesKnob {
    private static final Logger logger = Logger.getLogger(HttpCookiesKnobImpl.class.getName());
    private static final String DOMAIN = "Domain";
    private static final String PATH = "Path";
    private static final String COMMENT = "Comment";
    private static final String VERSION = "Version";
    private static final String MAX_AGE = "Max-Age";
    private static final String SECURE = "Secure";
    private static final String ATTRIBUTE_DELIMITER = "; ";
    private static final String EQUALS = "=";
    private static final int UNSPECIFIED_MAX_AGE = -1;
    private final HeadersKnob delegate;

    /**
     * Create a HeadersKnob-backed HttpCookiesKnob.
     *
     * @param delegate the HeadersKnob which backs the HttpCookiesKnob.
     */
    public HttpCookiesKnobImpl(@NotNull final HeadersKnob delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<HttpCookie> getCookies() {
        final Set<HttpCookie> cookies = new LinkedHashSet<>();
        for (final String setCookieValue : delegate.getHeaderValues(HttpConstants.HEADER_SET_COOKIE)) {
            try {
                cookies.add(new HttpCookie(setCookieValue));
            } catch (final HttpCookie.IllegalFormatException e) {
                logger.log(Level.WARNING, "Skipping invalid Set-Cookie header: " + setCookieValue);
            }
        }
        for (final String cookieValue : delegate.getHeaderValues(HttpConstants.HEADER_COOKIE)) {
            try {
                cookies.add(new HttpCookie(cookieValue));
            } catch (final HttpCookie.IllegalFormatException e) {
                logger.log(Level.WARNING, "Skipping invalid Cookie header: " + cookieValue);
            }
        }
        return cookies;
    }

    @Override
    public boolean containsCookie(@NotNull final String name, @Nullable final String domain, @Nullable final String path) {
        boolean found = false;
        for (final HttpCookie cookie : getCookies()) {
            if (name.equals(cookie.getCookieName()) && ObjectUtils.equals(domain, cookie.getDomain()) && ObjectUtils.equals(path, cookie.getPath())) {
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * Adds a cookie which is backed by a set-cookie header as this type of header contains all cookie attributes
     *
     * @param cookie the HttpCookie to add.
     */
    @Override
    public void addCookie(@NotNull final HttpCookie cookie) {
        for (final HttpCookie conflictingCookie : getConflictingCookies(cookie)) {
            deleteCookie(conflictingCookie);
            logger.log(Level.WARNING, "Removed conflicting cookie: " + conflictingCookie);
        }
        delegate.addHeader(HttpConstants.HEADER_SET_COOKIE, toV1SetCookieHeader(cookie));
    }

    @Override
    public void deleteCookie(@NotNull final HttpCookie cookie) {
        removeMatchingCookieHeaders(HttpConstants.HEADER_SET_COOKIE, cookie);
        removeMatchingCookieHeaders(HttpConstants.HEADER_COOKIE, cookie);
    }

    private void removeMatchingCookieHeaders(final String cookieHeaderName, final HttpCookie cookie) {
        for (final String setCookieValue : delegate.getHeaderValues(cookieHeaderName)) {
            try {
                final HttpCookie fromHeader = new HttpCookie(setCookieValue);
                if (cookie.getId().equals(fromHeader.getId())) {
                    delegate.removeHeader(cookieHeaderName, setCookieValue);
                }
            } catch (final HttpCookie.IllegalFormatException e) {
                logger.log(Level.WARNING, "Skipping invalid " + cookieHeaderName + " header: " + setCookieValue);
            }
        }
    }

    private String toV1SetCookieHeader(final HttpCookie cookie) {
        final StringBuilder sb = new StringBuilder();
        sb.append(cookie.getCookieName()).append(EQUALS).append(HttpCookie.quoteIfNeeded(cookie.getCookieValue()));
        sb.append(ATTRIBUTE_DELIMITER).append(VERSION).append(EQUALS).append(cookie.getVersion());
        appendIfNotBlank(sb, DOMAIN, cookie.getDomain());
        appendIfNotBlank(sb, PATH, cookie.getPath());
        appendIfNotBlank(sb, COMMENT, cookie.getComment());
        if (cookie.getMaxAge() != UNSPECIFIED_MAX_AGE) {
            sb.append(ATTRIBUTE_DELIMITER).append(MAX_AGE).append(EQUALS).append(cookie.getMaxAge());
        }
        if (cookie.isSecure()) {
            sb.append(ATTRIBUTE_DELIMITER).append(SECURE);
        }
        return sb.toString();
    }

    private void appendIfNotBlank(final StringBuilder stringBuilder, final String attributeName, final String attributeValue) {
        if (StringUtils.isNotBlank(attributeValue)) {
            stringBuilder.append(ATTRIBUTE_DELIMITER).append(attributeName).append(EQUALS).append(attributeValue);
        }
    }

    private Set<HttpCookie> getConflictingCookies(final HttpCookie cookie) {
        final Set<HttpCookie> conflictingCookies = new HashSet<>();
        for (HttpCookie currentCookie : getCookies()) {
            if (currentCookie.getId().equals(cookie.getId())) {
                conflictingCookies.add(currentCookie);
            }
            return conflictingCookies;
        }
        return conflictingCookies;
    }
}
