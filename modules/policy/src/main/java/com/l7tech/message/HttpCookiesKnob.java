package com.l7tech.message;

import com.l7tech.common.http.HttpCookie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * A knob which stores http-specific cookies.
 */
public interface HttpCookiesKnob extends MessageKnob {
    /**
     * @return all HttpCookies in the knob, not including any invalid cookies.
     */
    @NotNull
    public Set<HttpCookie> getCookies();

    /**
     * @return all HttpCookies in the knob as header values, including any invalid cookies.
     */
    @NotNull
    public Set<String> getCookiesAsHeaders();

    /**
     * @param name   the name of the cookie to look for.
     * @param domain the optional domain of the cookie to look for.
     * @param path   the optional path of the cookie to look for.
     * @return true if a cookie exists with the given name, domain and path.
     */
    public boolean containsCookie(@NotNull final String name, @Nullable final String domain, @Nullable final String path);

    /**
     * Add an HttpCookie to the knob, overriding any existing cookie with the same name, domain and path.
     *
     * @param cookie the HttpCookie to add.
     */
    public void addCookie(@NotNull final HttpCookie cookie);

    /**
     * Deletes an HttpCookie from the knob.
     *
     * @param cookie the HttpCookie to delete.
     */
    public void deleteCookie(@NotNull final HttpCookie cookie);
}
