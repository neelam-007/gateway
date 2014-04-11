package com.l7tech.message;

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

/**
 * HttpCookiesKnob implementation which wraps a HeadersKnob.
 */
public class HttpCookiesKnobImpl implements HttpCookiesKnob {
    private static final Logger logger = Logger.getLogger(HttpCookiesKnobImpl.class.getName());
    private static final Comparator<String> COOKIE_ATTRIBUTE_COMPARATOR = ComparatorUtils.nullHighComparator(new CookieTokenComparator());
    private static final String DOMAIN = "Domain";
    private static final String PATH = "Path";
    private static final String COMMENT = "Comment";
    private static final String VERSION = "Version";
    private static final String MAX_AGE = "Max-Age";
    private static final String SECURE = "Secure";
    private static final String HTTP_ONLY = "HttpOnly";
    private static final String ATTRIBUTE_DELIMITER = "; ";
    private static final String EQUALS = "=";
    private static final String DOLLAR_SIGN = "$";
    private static final int UNSPECIFIED_MAX_AGE = -1;
    private static final Set<String> COOKIE_ATTRIBUTES;
    private final HeadersKnob delegate;

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
            // it is possible for multiple cookies to be stored in a single Cookie header in multiple formats
            // ex) Cookie: 1=one; 2=two                         ---> Netscape format
            // ex) Cookie: $Version=1; 1=one; $Version=1; 2=two ---> RFC2109
            // ex) Cookie: 1=one; $Version=1; 2=two; $Version=1 ---> RFC2109 except Version is after name=value
            final String[] tokens = StringUtils.split(cookieValue, ATTRIBUTE_DELIMITER);
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
        if (cookie.isHttpOnly()) {
            sb.append(ATTRIBUTE_DELIMITER).append(HTTP_ONLY);
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
