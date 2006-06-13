/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util.res;

import com.l7tech.common.http.cache.HttpObjectCache;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.util.TextUtils;
import com.l7tech.policy.MessageUrlResourceInfo;

import java.util.regex.PatternSyntaxException;
import java.util.Map;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;

/**
 * A ResourceGetter that finds URLs inside the message, then fetches the appropriate resource (with caching),
 * corresponding to {@link com.l7tech.policy.MessageUrlResourceInfo}.
 */
class MessageUrlResourceGetter<R,C> extends UrlResourceGetter<R,C> {
    private final UrlFinder urlFinder;
    private final boolean allowMessagesWithoutUrl;

    // --- Instance fields ---

    MessageUrlResourceGetter(MessageUrlResourceInfo ri,
                                     ResourceObjectFactory<R,C> rof,
                                     UrlFinder urlFinder,
                                     HttpObjectCache<C> httpObjectCache,
                                     HttpObjectCache.UserObjectFactory<C> cacheObjectFactory,
                                     Auditor auditor)
            throws PatternSyntaxException
    {
        super(rof, httpObjectCache, cacheObjectFactory, ri.makeUrlPatterns(), auditor);
        this.urlFinder = urlFinder;
        this.allowMessagesWithoutUrl = ri.isAllowMessagesWithoutUrl();

        if (rof == null || urlFinder == null) throw new NullPointerException(); // can't happen
    }

    public void close() {
        // Nothing we can do except wait for the finalizer -- userObject(s) may be in use
    }

    public R getResource(ElementCursor message, Map vars)
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

        if (!TextUtils.matchesAny(url, getUrlWhitelist()))
            throw new UrlNotPermittedException("External resource URL not permitted by whitelist: " + url, url);

        try {
            return fetchObject(httpObjectCache, url);
        } catch (ParseException e) {
            throw new ResourceParseException(e, url);
        }
    }
}
