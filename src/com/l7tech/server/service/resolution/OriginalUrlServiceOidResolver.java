/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */
package com.l7tech.server.service.resolution;

import com.l7tech.common.audit.MessageProcessingMessages;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.service.PublishedService;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves services based on the Service OID that is passed in the original
 * url oor at the end of uri. The format expected is <i>/service/3145729</i>.
 *
 * @author emil
 */
public class OriginalUrlServiceOidResolver extends NameValueServiceResolver<String> {
    private final Pattern[] regexPatterns;

    /**
     * the regular expresison that extracts the service oid at the end of the original url ir request URI
     * Each regex must match the service OID as match group #1 or else fail
     */
    private final String[] REGEXES = {
        "/service/(\\d+)$",
        "\\?(?<=[?&])serviceoid=(\\d+)(?:(\\&|$))",
    };

    public OriginalUrlServiceOidResolver(ApplicationContext spring) {
        super(spring);
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

    protected List<String> doGetTargetValues(PublishedService service) {
        return Arrays.asList(Long.toString(service.getOid()));
    }

    /**
     *
     * @param request the message request to examine
     * @return the service OID if found, <b>null</b> otherwise
     * @throws ServiceResolutionException on service resolution error (multiple
     */
    protected String getRequestValue(Message request) throws ServiceResolutionException {
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
            auditor.logAndAudit(MessageProcessingMessages.SR_ORIGURL_NOHEADER, SecureSpanConstants.HttpHeaders.ORIGINAL_URL);
        } else {
            final String match = findMatch(originalUrl);
            if (match != null) {
                auditor.logAndAudit(MessageProcessingMessages.SR_ORIGURL_HEADER_MATCH, SecureSpanConstants.HttpHeaders.ORIGINAL_URL, originalUrl); 
                return match;
            }
            auditor.logAndAudit(MessageProcessingMessages.SR_ORIGURL_HEADER_NOMATCH, SecureSpanConstants.HttpHeaders.ORIGINAL_URL, originalUrl);
        }

        String requestURI = httpReqKnob.getRequestUri();
        final String match = findMatch(requestURI);
        if (match != null) {
            auditor.logAndAudit(MessageProcessingMessages.SR_ORIGURL_URI_MATCH, requestURI);
            return match;
        }
        auditor.logAndAudit(MessageProcessingMessages.SR_ORIGURL_URI_NOMATCH, requestURI);
        return null;
    }

    private String findMatch(String originalUrl) {
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

    public Set<String> getDistinctParameters(PublishedService candidateService) {
        throw new UnsupportedOperationException();
    }

    public Set<PublishedService> resolve(Message request, Set<PublishedService> serviceSubset) throws ServiceResolutionException {
        String value = getRequestValue(request);
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
}
