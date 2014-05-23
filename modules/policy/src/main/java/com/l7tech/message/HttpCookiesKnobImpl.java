package com.l7tech.message;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpCookie;
import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.common.http.CookieUtils.*;
import static com.l7tech.message.HeadersKnob.HEADER_TYPE_HTTP;

/**
 * HttpCookiesKnob implementation which wraps a HeadersKnob.
 */
public class HttpCookiesKnobImpl implements HttpCookiesKnob {
    private static final Logger logger = Logger.getLogger(HttpCookiesKnobImpl.class.getName());
    private static final Comparator<String> COOKIE_ATTRIBUTE_COMPARATOR = ComparatorUtils.nullHighComparator(new CookieTokenComparator());
    private static final String DOLLAR_SIGN = "$";
    private static final Set<String> COOKIE_ATTRIBUTES;
    private final HeadersKnob delegate;
    private final String cookieHeaderName;

    static {
        COOKIE_ATTRIBUTES = new HashSet<>();
        COOKIE_ATTRIBUTES.add(DOMAIN.toLowerCase());
        COOKIE_ATTRIBUTES.add(PATH.toLowerCase());
        COOKIE_ATTRIBUTES.add(COMMENT.toLowerCase());
        COOKIE_ATTRIBUTES.add(VERSION.toLowerCase());
        COOKIE_ATTRIBUTES.add(MAX_AGE.toLowerCase());
        COOKIE_ATTRIBUTES.add(SECURE.toLowerCase());
        COOKIE_ATTRIBUTES.add(HTTP_ONLY.toLowerCase());
    }

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
        if (cookieHeaderName.equalsIgnoreCase(HttpConstants.HEADER_COOKIE)) {
            for (final String cookieValue : delegate.getHeaderValues(HttpConstants.HEADER_COOKIE, HEADER_TYPE_HTTP)) {
                final List<String> singleCookieValues = splitCookieHeader(cookieValue);
                for (final String singleCookieValue : singleCookieValues) {
                    try {
                        cookies.add(new HttpCookie(singleCookieValue));
                    } catch (final HttpCookie.IllegalFormatException e) {
                        logger.log(Level.WARNING, "Could not process cookie value: " + cookieValue);
                    }
                }
            }
        } else {
            for (final String cookieValue : delegate.getHeaderValues(cookieHeaderName, HEADER_TYPE_HTTP)) {
                try {
                    final boolean added = cookies.add(new HttpCookie(cookieValue));
                    if (!added) {
                        logger.log(Level.WARNING, "Found duplicate cookie: " + cookieValue);
                    }
                } catch (final HttpCookie.IllegalFormatException e) {
                    logger.log(Level.WARNING, "Skipping invalid " + cookieHeaderName + " header: " + cookieValue);
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
     * Adds a cookie which is backed by a set-cookie format as this type of header contains all cookie attributes.
     *
     * @param cookie the HttpCookie to add.
     */
    @Override
    public void addCookie(@NotNull final HttpCookie cookie) {
        for (final HttpCookie conflictingCookie : getConflictingCookies(cookie)) {
            deleteCookie(conflictingCookie);
            logger.log(Level.WARNING, "Removed conflicting cookie: " + conflictingCookie);
        }
        delegate.addHeader(cookieHeaderName, CookieUtils.getSetCookieHeader(cookie), HEADER_TYPE_HTTP);
    }

    @Override
    public void deleteCookie(@NotNull final HttpCookie cookie) {
        removeMatchingCookieHeaders(cookieHeaderName, cookie);
    }

    private List<String> splitCookieHeader(final String cookieValue) {
        // it is possible for multiple cookies to be stored in a single Cookie header in multiple formats
        // ex) Cookie: 1=one; 2=two                         ---> Netscape format
        // ex) Cookie: $Version=1; 1=one; $Version=1; 2=two ---> RFC2109
        // ex) Cookie: 1=one; $Version=1; 2=two; $Version=1 ---> RFC2109 except Version is after name=value
        final String[] tokens = StringUtils.split(cookieValue, ATTRIBUTE_DELIMITER.trim());
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
    private boolean isVersion(final String token) {
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
    private static boolean isCookieAttribute(final String token) {
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

    private void removeMatchingCookieHeaders(final String cookieHeaderName, final HttpCookie cookie) {
        for (final String cookieValue : delegate.getHeaderValues(cookieHeaderName, HEADER_TYPE_HTTP)) {
            try {
                final HttpCookie fromHeader = new HttpCookie(cookieValue);
                if (cookie.getId().equals(fromHeader.getId())) {
                    delegate.removeHeader(cookieHeaderName, cookieValue, HEADER_TYPE_HTTP);
                }
            } catch (final HttpCookie.IllegalFormatException e) {
                logger.log(Level.WARNING, "Skipping invalid " + cookieHeaderName + " header: " + cookieValue);
            }
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
