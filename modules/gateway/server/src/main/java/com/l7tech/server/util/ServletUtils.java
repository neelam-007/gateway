package com.l7tech.server.util;

import com.l7tech.message.HeadersKnob;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServletUtils {
    public static final String PARAM_HTTP_X509CERT = "javax.servlet.request.X509Certificate";

    private static final Logger logger = Logger.getLogger(ServletUtils.class.getName());

    /**
     * @return the most-local X509Certificate from the request's certificate chain
     */
    public static X509Certificate getRequestCert(HttpServletRequest req) {
        // this check only makes sense if the request comes over SSL
        if (!req.isSecure()) throw new IllegalArgumentException("Cannot get cert from non-SSL request");
        // check if the user currently has a valid cert

        Object param = req.getAttribute(PARAM_HTTP_X509CERT);
        if (param == null) {
            logger.fine("No client cert in that request.");
            return null;
        } else if (param instanceof Object[]) {
            Object[] maybeCerts = (Object[])param;
            if (maybeCerts[0] instanceof X509Certificate) {
                return (X509Certificate)maybeCerts[0];
            } else {
                logger.info("Non-X.509 Certificate found in client certificate chain: " + maybeCerts[0].getClass().getName());
                return null;
            }
        } else if (param instanceof X509Certificate) {
            logger.fine("Found X.509 certificate in request");
            return (X509Certificate)param;
        }

        logger.info("Cert param present but type not suppoted " + param.getClass().getName());
        return null;
    }

    /**
     * Loads a HeadersKnob with headers from request, filtering a specific set of 'non-application' headers.
     *
     * @param sourceRequest     the HttpServletRequest which is a source of headers.
     * @param targetHeadersKnob the HeadersKnob to load with headers from the request.
     * @see {@link com.l7tech.policy.assertion.HttpPassthroughRuleSet#HEADERS_NOT_TO_IMPLICITLY_FORWARD}
     */
    public static void loadHeaders(@NotNull final HttpServletRequest sourceRequest, @NotNull final HeadersKnob targetHeadersKnob) {
        final Enumeration headerNames = sourceRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            final String headerName = (String) headerNames.nextElement();
            if (!HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD.contains(headerName.toLowerCase())) {
                final Enumeration headerValues = sourceRequest.getHeaders(headerName);
                while (headerValues.hasMoreElements()) {
                    targetHeadersKnob.addHeader(headerName, headerValues.nextElement());
                }
            } else {
                logger.log(Level.FINEST, "Filtering request header " + headerName);
            }
        }
    }
}
