package com.l7tech.server.service.resolution;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.ResolutionConfiguration;
import com.l7tech.message.HasServiceGoid;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.util.GoidUpgradeMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves services based on the Service OID that is passed in the original
 * url or at the end of uri. The format expected is <i>/service/3145729</i>.
 *
 * Also resolves using the HasServiceGoid message facet.
 */
public class ServiceIdResolver extends NameValueServiceResolver<String> {
    private final Pattern[] regexPatterns;
    // when disabled we still support HasServiceGoid for hard coded service resolution
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicBoolean enableOriginalUrlHeader = new AtomicBoolean(true);

    public ServiceIdResolver(final AuditFactory auditorFactory) {
        super( auditorFactory );
        List<Pattern> compiled = new ArrayList<Pattern>();

        try {
            for (String s : SecureSpanConstants.RESOLUTION_BY_ID_REGEXES) {
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
    public void configure( final ResolutionConfiguration resolutionConfiguration ) {
        super.configure( resolutionConfiguration );
        enabled.set( resolutionConfiguration.isUseServiceOid() );
        enableOriginalUrlHeader.set( resolutionConfiguration.isUseL7OriginalUrl() );
    }

    @Override
    protected List<String> buildTargetValues(PublishedService service) {
        ArrayList<String> targetValues = new ArrayList<>();
        targetValues.add(Goid.toString(service.getGoid()));
        //This is needed to handle the services being referenced by their old oid's
        if(GoidUpgradeMapper.prefixMatches(EntityType.SERVICE, service.getGoid().getHi()))
            targetValues.add(Long.toString(service.getGoid().getLow()));
        return targetValues;
    }

    /**
     *
     * @param request the message request to examine
     * @return the service ID if found, <b>null</b> otherwise
     * @throws ServiceResolutionException on service resolution error (multiple
     */
    @Override
    protected String getRequestValue(Message request) throws ServiceResolutionException {
        HasServiceGoid hso = request.getKnob( HasServiceGoid.class );
        if ( hso != null && !Goid.isDefault(hso.getServiceGoid()) ) return Goid.toString( hso.getServiceGoid() );

        if ( !enabled.get() || regexPatterns == null) { // compile failed
            return null;
        }
        
        HttpRequestKnob httpReqKnob = request.getKnob(HttpRequestKnob.class);
        if (httpReqKnob == null) return null;

        if ( enableOriginalUrlHeader.get() ) {
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
            // note that the below group count check excludes one of the regex expressions
            // this seems like a fortunate bug since services can (and do) use the serviceoid
            // query parameter unrelated to service resolution
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
    public boolean isApplicableToMessage(Message request) throws ServiceResolutionException {
        // special case: if the request does not follow pattern, then all services passed
        // match, the next resolver will narrow it down
        return getRequestValue(request) != null;
    }
}
