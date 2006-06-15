/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util.res;

import com.l7tech.common.http.cache.UrlResolver;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.server.policy.ServerPolicyException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.Map;

/**
 * A ResourceGetter that has a single statically-configured URL that it watches to download the latest value of
 * the resource, corresponding to {@link com.l7tech.policy.SingleUrlResourceInfo}.
 */
class SingleUrlResourceGetter<R> extends UrlResourceGetter<R> {
    private final String url;

    SingleUrlResourceGetter(Assertion assertion,
                            SingleUrlResourceInfo ri,
                            UrlResolver<R> urlResolver)
            throws ServerPolicyException
    {
        super(urlResolver);
        String url = ri.getUrl();
        if (url == null) throw new ServerPolicyException(assertion, "Missing resource url");
        this.url = url;

        // Ensure URL is well-formed
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new ServerPolicyException(assertion, "Invalid resource URL: " + url);
        }
    }

    public void close() {
        // Nothing we can do except wait for the finalizer -- userObject(s) may be in use
    }

    public R getResource(ElementCursor message, Map vars) throws IOException, ResourceParseException, GeneralSecurityException, ResourceIOException, MalformedResourceUrlException {
        String actualUrl = vars == null ? url : ExpandVariables.process(url, vars);
        try {
            return fetchObject(actualUrl);
        } catch (ParseException e) {
            throw new ResourceParseException(e, actualUrl);
        }
    }
}
