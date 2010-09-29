package com.l7tech.server.util.res;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.url.UrlResolver;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.util.ExceptionUtils;

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
class SingleUrlResourceGetter<R, M> extends UrlResourceGetter<R, M> {
    private final String url;
    private final boolean expandVariables;

    SingleUrlResourceGetter(Assertion assertion,
                            SingleUrlResourceInfo ri,
                            UrlResolver<R> urlResolver,
                            Audit audit)
            throws ServerPolicyException
    {
        super(urlResolver, audit);
        String url = ri.getUrl();
        if (url == null) throw new ServerPolicyException(assertion, "Missing resource url");
        this.url = url;
        this.expandVariables = Syntax.getReferencedNames( url ).length > 0;

        if ( !expandVariables ) {
            // Ensure URL is well-formed
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                throw new ServerPolicyException(assertion, "Invalid resource URL: " + url);
            }
        }
    }

    @Override
    public void close() {
        // Nothing we can do -- userObject(s) may be in use
    }

    @Override
    public R getResource( final M notUsed,
                          final Map<String,Object> vars ) throws IOException, ResourceParseException, GeneralSecurityException, ResourceIOException, MalformedResourceUrlException {
        final String actualUrl = vars == null || !expandVariables ?
                url :
                ExpandVariables.process(url, vars, audit);
        try {
            return fetchObject(actualUrl);
        } catch (ParseException e) {
            throw new ResourceParseException(ExceptionUtils.getMessage(e), e, actualUrl);
        } catch (IOException e) {
            throw new ResourceIOException(ExceptionUtils.getMessage(e), e, actualUrl);
        }
    }
}
