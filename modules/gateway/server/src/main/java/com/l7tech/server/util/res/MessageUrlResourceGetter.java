package com.l7tech.server.util.res;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.server.url.UrlResolver;
import com.l7tech.util.TextUtils;
import com.l7tech.policy.MessageUrlResourceInfo;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A ResourceGetter that finds URLs inside the message, then fetches the appropriate resource (with caching),
 * corresponding to {@link com.l7tech.policy.MessageUrlResourceInfo}.
 */
class MessageUrlResourceGetter<R, M> extends UrlResourceGetter<R, M> {
    private final UrlFinder<M> urlFinder;
    private final boolean allowMessagesWithoutUrl;
    private final Pattern[] urlWhitelist;

    // --- Instance fields ---

    MessageUrlResourceGetter(MessageUrlResourceInfo ri,
                             UrlResolver<R> urlResolver,
                             UrlFinder<M> urlFinder,
                             Audit audit)
            throws PatternSyntaxException
    {
        super(urlResolver, audit);
        if (urlFinder == null) throw new NullPointerException();
        this.urlFinder = urlFinder;
        this.allowMessagesWithoutUrl = ri.isAllowMessagesWithoutUrl();
        this.urlWhitelist = ri.makeUrlPatterns();
    }

    @Override
    public void close() {
        // Nothing we can do -- userObject(s) may be in use
    }

    @Override
    public R getResource(M message, Map<String,Object> vars)
            throws IOException, MalformedResourceUrlException, UrlNotPermittedException,
            ResourceIOException, InvalidMessageException, ResourceParseException, GeneralSecurityException, UrlNotFoundException
    {
        final String url;
        url = urlFinder.findUrl(message);
        if (url == null) {
            if (allowMessagesWithoutUrl)
                return null;

            throw new UrlNotFoundException();
        }

        // match against URL patterns

        if (!TextUtils.matchesAny(url, urlWhitelist))
            throw new UrlNotPermittedException("External resource URL not permitted by whitelist: " + url, url);

        try {
            return fetchObject(url);
        } catch (ParseException e) {
            throw new ResourceParseException(e, url);
        } catch (IOException e) {
            throw new ResourceIOException(e, url);
        }
    }
}
