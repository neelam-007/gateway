package com.l7tech.message;

import com.l7tech.common.http.HttpCookie;
import org.jetbrains.annotations.NotNull;

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
