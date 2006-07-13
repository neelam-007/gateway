/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */
package com.l7tech.server.service.resolution;

import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.service.PublishedService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves services based on the Service OID that is passed in the original
 * url oor at the end of uri. The format expected is <i>/service/3145729</i>.
 *
 * @author emil
 */
public class OriginalUrlServiceOidResolver extends NameValueServiceResolver {
    private final Pattern[] regexPatterns;

    /**
     * the regular expresison that extracts the service oid at the end of the original url ir request URI
     * Each regex must match the service OID as match group #1 or else fail
     */
    private final String[] REGEXES = {
        "/service/(\\d+)$",
        "\\?(?<=[?&])serviceoid=(\\d+)(?:(\\&|$))",
    };

    public OriginalUrlServiceOidResolver() {
        List<Pattern> compiled = new ArrayList<Pattern>();

        try {
            for (String s : REGEXES) {
                compiled.add(Pattern.compile(s));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "A Regular Expression failed to compile. " +
                                      "This resolver is disabled.", e);
            compiled.clear();
        }

        regexPatterns = compiled.isEmpty() ? null : compiled.toArray(new Pattern[0]);
    }

    protected Object[] doGetTargetValues(PublishedService service) {
        return new String[]{Long.toString(service.getOid())};
    }

    /**
     *
     * @param request the message request to examine
     * @return the service OID if found, <b>null</b> otherwise
     * @throws ServiceResolutionException on service resolution error (multiple
     */
    protected Object getRequestValue(Message request) throws ServiceResolutionException {
        if (regexPatterns == null) { // compile failed
            return null;
        }
        HttpRequestKnob httpReqKnob = (HttpRequestKnob)request.getKnob(HttpRequestKnob.class);
        if (httpReqKnob == null) return null;
        String originalUrl;
        try {
            originalUrl = httpReqKnob.getHeaderSingleValue(SecureSpanConstants.HttpHeaders.ORIGINAL_URL);
        } catch (IOException e) {
            throw new ServiceResolutionException("Found multiple " + SecureSpanConstants.HttpHeaders.ORIGINAL_URL + " values"); // can't happen
        }

        if (originalUrl == null) {
            logger.finest("The header '" + SecureSpanConstants.HttpHeaders.ORIGINAL_URL + "' is not present");
        } else {
            final Object match = findMatch(originalUrl);
            if (match != null) {
                logger.finest("Matched against the header '" + SecureSpanConstants.HttpHeaders.ORIGINAL_URL + "' URL: " + originalUrl);
                return match;
            }
            logger.finest("Not Matched against the header " + SecureSpanConstants.HttpHeaders.ORIGINAL_URL + "' URL: " + originalUrl);
        }

        String requestURI = httpReqKnob.getRequestUri();
        final Object match = findMatch(requestURI);
        if (match != null) {
            logger.finest("Matched against the Request URI: " + requestURI);
            return match;
        }
        logger.finest("Not Matched against the request URI: " +requestURI);

        return null;
    }

    private Object findMatch(String originalUrl) {
        for (Pattern regexPattern : regexPatterns) {
            Matcher matcher = regexPattern.matcher(originalUrl);
            if (matcher.find() && matcher.groupCount() == 1) {
                return matcher.group(1);
            }
        }
        return null;
    }

    public int getSpeed() {
        return FAST;
    }

    public Set getDistinctParameters(PublishedService candidateService) {
        throw new UnsupportedOperationException();
    }

    public Set<PublishedService> resolve(Message request, Set<PublishedService> serviceSubset) throws ServiceResolutionException {
        Object value = getRequestValue(request);
        // special case: if the request does not follow pattern, then all services passed
        // match, the next resolver will narrow it down
        if (value == null) {
            if (serviceSubset.size() == 1) {
                // in this case, i can't return the set, otherwize it will be interpreted as a match!
                throw new NoServiceOIDResolutionPassthroughException();
            } else {
                return serviceSubset;
            }
        } else {
            return super.resolve(value, serviceSubset);
        }
    }

    protected static final Logger logger = Logger.getLogger(OriginalUrlServiceOidResolver.class.getName());
}
