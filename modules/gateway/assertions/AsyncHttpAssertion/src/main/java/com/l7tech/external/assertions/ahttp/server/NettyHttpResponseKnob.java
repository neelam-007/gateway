package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.message.AbstractHttpResponseKnob;
import com.l7tech.util.ISO8601Date;
import com.l7tech.util.Pair;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.Cookie;
import java.util.*;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;

/**
 *
 */
public class NettyHttpResponseKnob extends AbstractHttpResponseKnob {
    private final HttpResponse httpResponse;


    public NettyHttpResponseKnob(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
        statusToSet = HttpResponseStatus.OK.getCode();
    }

    /**
     * Begins the process of sending the response to the client by setting a status code and sending headers using the HttpServletResponse.
     * @deprecated use {@link #beginResponse(java.util.Collection, java.util.Collection)}.
     */
    @Deprecated
    public void beginResponse() {
        beginResponse(Collections.<Pair<String, Object>>emptyList(), Collections.<HttpCookie>emptyList());
    }

    /**
     * Begins the process of sending the response to the client by setting a status code and sending headers using the HttpServletResponse.
     * @param headersToSend the collection of headers to send with the response.
     * @param cookiesToSend the collection of HttpCookies to send with the response.
     */
    public void beginResponse(@NotNull final Collection<Pair<String, Object>> headersToSend, @NotNull final Collection<HttpCookie> cookiesToSend) {
        httpResponse.setStatus(HttpResponseStatus.valueOf(statusToSet));

        for ( final Pair<String, Object> pair : headersToSend ) {
            final Object value = pair.right;
            if (value instanceof Long) {
                httpResponse.addHeader(pair.left, ISO8601Date.format(new Date((Long)value)));
            } else {
                httpResponse.addHeader(pair.left, (String)value);
            }
        }

        CookieEncoder cookieEncoder = new CookieEncoder(true);
        for ( final HttpCookie cookie : cookiesToSend ) {
            org.jboss.netty.handler.codec.http.Cookie c = new DefaultCookie(cookie.getCookieName(), cookie.getCookieValue());
            c.setDomain(cookie.getDomain());
            c.setComment(cookie.getComment());
            c.setSecure(cookie.isSecure());
            c.setMaxAge(cookie.getMaxAge());
            c.setPath(cookie.getPath());
            c.setVersion(cookie.getVersion());
            cookieEncoder.addCookie(c);
        }
        httpResponse.addHeader(SET_COOKIE, cookieEncoder.encode());
    }

    public boolean hasChallenge() {
        return !challengesToSend.isEmpty();
    }

    /**
     * Add the challenge headers to the response.  The challenges will be sorted in reverse alphabetical order
     * so that Digest will be preferred over Basic, if both are present.
     */
    public void beginChallenge() {
        Collections.sort(challengesToSend, new ChallengeComparator(challengeOrder));
        for (Pair<String,Integer> challenge : challengesToSend) {
            httpResponse.addHeader(HttpConstants.HEADER_WWW_AUTHENTICATE, challenge.left);
        }
    }
}
