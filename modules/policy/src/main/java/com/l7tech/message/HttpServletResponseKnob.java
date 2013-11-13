package com.l7tech.message;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of {@link HttpResponseKnob} that knows how to place the HTTP response transport metadata
 * into a servlet response.
 */
public class HttpServletResponseKnob extends AbstractHttpResponseKnob {
    private final HttpServletResponse response;
    private final List<Cookie> cookiesToSend = new ArrayList<Cookie>();

    public HttpServletResponseKnob(HttpServletResponse response) {
        if (response == null) throw new NullPointerException();
        this.response = response;
    }

    @Override
    public void addCookie(HttpCookie cookie) {
        cookiesToSend.add(CookieUtils.toServletCookie(cookie));
    }

    /**
     * Begins the process of sending the response to the client by setting a status code and sending headers using the HttpServletResponse.
     * @deprecated use {@link #beginResponse(java.util.Collection)}.
     */
    @Deprecated
    public void beginResponse() {
        beginResponse(Collections.<Pair<String, Object>>emptyList());
    }

    /**
     * Begins the process of sending the response to the client by setting a status code and sending headers using the HttpServletResponse.
     * @param headersToSend the collection of headers to send with the response.
     */
    public void beginResponse(@NotNull final Collection<Pair<String, Object>> headersToSend) {
        response.setStatus(statusToSet);

        for ( final Pair<String, Object> pair : headersToSend ) {
            final Object value = pair.right;
            if (value instanceof Long) {
                response.addDateHeader(pair.left, (Long)value);
            } else {
                response.addHeader(pair.left, (String)value);
            }
        }

        for ( final Cookie cookie : cookiesToSend ) {
            response.addCookie(cookie);
        }
    }

    public boolean hasChallenge() {
        return !challengesToSend.isEmpty();
    }

    /**
     * Add the challenge headers to the response. The challenges will be sorted based on the protocol preference order:
     * * "windows" - most secure protocol comes first
     * * "reverse" - challenge challenges will be sorted in reverse alphabetical order
     * so that Digest will be preferred over Basic, if both are present.
     */
    public void beginChallenge() {
        Collections.sort(challengesToSend, new ChallengeComparator(challengeOrder));

        for (Pair<String, Integer> challenge : challengesToSend) {
            response.addHeader(HttpConstants.HEADER_WWW_AUTHENTICATE, challenge.left);
        }
    }

    /**
     * Think twice before using this. The HttpServletResponse should be restricted for the usage of the http transport.
     * Other uses may interfere with the stealth mode implementation. See ResponseKillerValve for more information.
     * todo, remove this completely?
     * @deprecated don't touch this unless you really need it.
     * @return the raw HttpServletResponse
     * */
    @Deprecated
    public HttpServletResponse getHttpServletResponse() {
        return response;
    }
}
