/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service.resolution;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.message.HasServiceOid;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves services based on the Service OID that is passed in the original
 * url or at the end of uri. The format expected is <i>/service/3145729</i>.
 *
 * Also resolves using the HasServiceOid message facet.
 */
public class ServiceOidResolver extends NameValueServiceResolver<String> {
    private final Pattern[] regexPatterns;

    public ServiceOidResolver(final ApplicationContext spring) {
        super(spring);
        List<Pattern> compiled = new ArrayList<Pattern>();

        try {
            for (String s : SecureSpanConstants.RESOLUTION_BY_OID_REGEXES) {
                compiled.add(Pattern.compile(s));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "A Regular Expression failed to compile. " +
                                      "This resolver is disabled.", e);
            compiled.clear();
        }

        regexPatterns = compiled.isEmpty() ? null : compiled.toArray(new Pattern[compiled.size()]);
    }

    @Override
    protected List<String> doGetTargetValues(PublishedService service) {
        return Arrays.asList(Long.toString(service.getOid()));
    }

    /**
     *
     * @param request the message request to examine
     * @return the service OID if found, <b>null</b> otherwise
     * @throws ServiceResolutionException on service resolution error (multiple
     */
    @Override
    protected String getRequestValue(Message request) throws ServiceResolutionException {
        HasServiceOid hso = request.getKnob( HasServiceOid.class );
        if ( hso != null && hso.getServiceOid() > 0 ) return Long.toString( hso.getServiceOid() );

        if (regexPatterns == null) { // compile failed
            return null;
        }
        
        HttpRequestKnob httpReqKnob = request.getKnob(HttpRequestKnob.class);
        if (httpReqKnob == null) return null;
        String originalUrl;
        originalUrl = httpReqKnob.getHeaderFirstValue(SecureSpanConstants.HttpHeaders.ORIGINAL_URL);

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

    @Override
    public boolean usesMessageContent() {
        return false;
    }

    @Override
    public Set<String> getDistinctParameters(PublishedService candidateService) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isApplicableToMessage(Message request) throws ServiceResolutionException {
        // special case: if the request does not follow pattern, then all services passed
        // match, the next resolver will narrow it down
        return getRequestValue(request) != null;
    }
}
