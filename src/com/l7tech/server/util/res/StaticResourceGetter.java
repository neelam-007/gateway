/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util.res;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.http.cache.HttpObjectCache;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.server.policy.ServerPolicyException;

import java.text.ParseException;
import java.util.regex.Pattern;
import java.util.Map;
import java.io.IOException;

/**
 * A ResourceGetter that owns a single statically-configured value for the resource, with no network
 * communication needed, and corresponding to {@link com.l7tech.policy.StaticResourceInfo}.
 */
class StaticResourceGetter<R,C> extends ResourceGetter<R,C> {
    private final R userObject;

    StaticResourceGetter(Assertion assertion,
                                 StaticResourceInfo ri,
                                 ResourceObjectFactory<R,C> rof,
                                 HttpObjectCache<C> httpObjectCache,
                                 HttpObjectCache.UserObjectFactory<C> cacheObjectFactory)
            throws ServerPolicyException
    {
        super(httpObjectCache, cacheObjectFactory);
        String doc = ri.getDocument();
        if (doc == null) throw new ServerPolicyException(assertion, "Empty static document");
        try {
            C cacheObject = cacheObjectFactory.createUserObject("", doc);
            R userObject = rof.createResourceObject("", cacheObject);
            if (userObject == null)
                throw new ServerPolicyException(assertion, "Unable to create static user object: ResourceObjectFactory returned null");
            this.userObject = userObject;
        } catch (ParseException e) {
            throw new ServerPolicyException(assertion, "Unable to create static user object: " + ExceptionUtils.getMessage(e), e);
        } catch (IOException e) {
            throw new ServerPolicyException(assertion, "Unable to create static user object: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public void close() {
        ResourceUtils.closeQuietly(userObject);
    }

    public Pattern[] getUrlWhitelist() {
        return new Pattern[] { MATCH_ALL };
    }

    public R getResource(ElementCursor message, Map vars) throws IOException {
        return userObject;
    }
}
