package com.l7tech.message;

import com.l7tech.common.http.HttpCookie;
import org.jetbrains.annotations.NotNull;

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
    private Set<HttpCookie> cookies = new LinkedHashSet<>();

    @Override
    public Set<HttpCookie> getCookies() {
        return cookies;
    }

    @Override
    public void addCookie(@NotNull final HttpCookie cookie) {
        for (final HttpCookie conflictingCookie : getConflictingCookies(cookie)) {
            deleteCookie(conflictingCookie);
            logger.log(Level.WARNING, "Removed conflicting cookie: " + conflictingCookie);
        }
        cookies.add(cookie);
    }

    @Override
    public void deleteCookie(@NotNull final HttpCookie cookie) {
        cookies.remove(cookie);
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
