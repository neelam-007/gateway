package com.l7tech.server.service.resolution;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.ResolutionConfiguration;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.message.UriKnob;
import com.l7tech.objectmodel.Goid;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * <p>
 * Resolves services based on the HTTP/FTP URI from which the incoming message came in through. Different services
 * can be assigned a resolution URI. By default, a service is not assigned a resolution URI. For resolution purposes
 * this special case is assigned the value of "". Requests coming in the ssg at a URI starting with
 * SecureSpanConstants.SSG_RESERVEDURI_PREFIX are considered as URIs of "".
 * </p><p>
 * Each service has one and only one resolution URI but the same resolution URI can be assigned to multiple services.
 * </p><p>
 * The wildcard '*' is allowed when assigning resolution URIs to services, these wildcards are considered at
 * resolution time.
 * </p><p>
 * Say two services published at /service1* and /service1/foo*, if a message comes in at /service1/foo/bar then
 * both resolution uris match. To resolve these conflicts, we adopt the mechanism described at
 * http://www.roguewave.com/support/docs/leif/leif/html/servletug/7-3.html#732:
 *
 * <ol>
 * <li>we prefer an exact path match over a wildcard path match</li>
 * <li>we finally prefer path matches over filetype matches</li>
 * <li>we then prefer to match the longest pattern</li>
 * </ol>
 * </p>
 *
 * @author franco
 */
public class UriResolver extends ServiceResolver<String> {

    public UriResolver( final AuditFactory auditorFactory ) {
        this( auditorFactory, true );
    }

    protected UriResolver( final AuditFactory auditorFactory,
                           final boolean caseSensitive ) {
        super( auditorFactory );
        this.caseSensitive = caseSensitive;
    }

    @Override
    public void configure( final ResolutionConfiguration resolutionConfiguration ) {
        super.configure( resolutionConfiguration );
        enableCaseSensitivity.set( resolutionConfiguration.isPathCaseSensitive() );
        enableOriginalUrlHeader.set( resolutionConfiguration.isUseL7OriginalUrl() );
    }

    @Override
    public boolean usesMessageContent() {
        return false;
    }

    private Result doResolve( final String requestValue,
                              final Collection<PublishedService> serviceSubset,
                              final boolean exactOnly ) {
        List<Goid> res = uriToServiceMap.get(new URIResolutionParam(requestValue));
        if (res != null && res.size() > 0) {
            if (auditor != null) {
                auditor.logAndAudit(MessageProcessingMessages.SR_HTTPURI_PERFECT, requestValue);
            }
            Set<PublishedService> output = narrowList(serviceSubset, res);
            if (output.size() > 0) return new Result(output); // otherwise, we continue and try to find match using wildcard ones
        }

        if ( exactOnly ) {
            return Result.NO_MATCH;
        }

        // otherwise, try to match using wildcards
        Set<URIResolutionParam> keys = uriToServiceMap.keySet();
        boolean encounteredPathPattern = false;
        boolean encounteredExtensionPattern = false;
        ArrayList<URIResolutionParam> matchingRegexKeys = new ArrayList<URIResolutionParam>();
        for (URIResolutionParam key : keys) {
            if (key.hasWildcards) {
                // only consider cached URI associated to at least one service in the passed subset
                // this ensures that further calls to narrowList will not yield empty service sets and
                // that the best fit will chosen from a set of potentially valid ones only
                if (isInSubset(serviceSubset, uriToServiceMap.get(key))) {
                    if (key.globalWildcard || ((Pattern) key.pattern).matcher(requestValue).matches()) {
                        if (key.pathPattern) encounteredPathPattern = true;
                        else encounteredExtensionPattern = true;
                        matchingRegexKeys.add(key);
                    }
                }
            }
        }
        if (matchingRegexKeys.size() <= 0) {
            if (auditor != null) {
                auditor.logAndAudit(MessageProcessingMessages.SR_HTTPURI_WILD_NONE, requestValue);
            }
            return Result.NO_MATCH;
        } else if (matchingRegexKeys.size() == 1) {
            if (auditor != null) {
                auditor.logAndAudit(MessageProcessingMessages.SR_HTTPURI_WILD_ONE, requestValue);
            }
            res = uriToServiceMap.get(matchingRegexKeys.get(0));
            return new Result(narrowList(serviceSubset, res));
        } else {
            // choose best match
            if (auditor != null) {
                auditor.logAndAudit(MessageProcessingMessages.SR_HTTPURI_WILD_MULTI, requestValue);
            }
            URIResolutionParam best = whichOneIsBest(matchingRegexKeys,
                                                    encounteredPathPattern,
                                                    encounteredExtensionPattern);
            res = uriToServiceMap.get(best);
            return new Result(narrowList(serviceSubset, res));
        }
    }

    @Override
    public void populateResolutionParameters( final Message request, final Map<String, Object> parameters ) throws ServiceResolutionException {
        if ( appliesToMessage(request) ) {
            final String value = getRequestValue( request );
            parameters.put( PROP_VALUE, value );
        }
    }

    @Override
    public Result resolve( Map<String,Object> parameters, Collection<PublishedService> serviceSubset) throws ServiceResolutionException {
        if ( caseSensitive != enableCaseSensitivity.get() ) return Result.NOT_APPLICABLE;
        if (!parameters.containsKey( PROP_VALUE )) return Result.NOT_APPLICABLE;
        final String requestValue = transformValue((String) parameters.get( PROP_VALUE ));
        final boolean exactOnly = Boolean.TRUE.equals( parameters.get( PROP_EXACT_ONLY ) );

        rwlock.readLock().lock();
        try {
            // first look at repetitive failures
            if (knownToFail.contains(requestValue)) { // why is this suspicious?
                auditor.logAndAudit(MessageProcessingMessages.SR_HTTPURI_CACHEDFAIL, requestValue);
                return Result.NO_MATCH;
            }
            Result res = doResolve(requestValue, serviceSubset, exactOnly);
            if (res == Result.NO_MATCH && !exactOnly) {
                // todo, this could be exploited as an attack. we should either not try to do this or
                // we should have a worker thread making sure this does not grow too big
                knownToFail.add(requestValue);
            }
            return res;
        } finally {
            rwlock.readLock().unlock();
        }
    }

    private String transformValue( final String value ) {
        String resultValue = value;

        //url decode the string. This is needed to handle urls with non standard characters. SSG-7898
        try {
            resultValue = URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("This should not happen!");
        }

        if ( !caseSensitive && value != null ) {
            resultValue = value.toLowerCase();
        }

        return resultValue;
    }

    private static boolean isInSubset(Collection<PublishedService> serviceSubset, List<Goid> criteria) {
        for (PublishedService svc : serviceSubset) {
            if (criteria.contains(svc.getGoid())) {
                return true;
            }
        }
        return false;
    }

    private static Set<PublishedService> narrowList(Collection<PublishedService> serviceSubset, List<Goid> criteria) {
        Set<PublishedService> output = new HashSet<PublishedService>();
        for (PublishedService svc : serviceSubset) {
            if (criteria.contains(svc.getGoid())) {
                output.add(svc);
            }
        }
        return output;
    }

    @Override
    public void serviceDeleted(PublishedService service) {
        rwlock.writeLock().lock();
        try {
            deletenolock(service);
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    @Override
    public void serviceUpdated(PublishedService service) throws ServiceResolutionException {
        rwlock.writeLock().lock();
        try {
            serviceDeleted(service);
            serviceCreated(service);
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    @Override
    protected List<String> buildTargetValues( final PublishedService service ) {
        String uri = service.getRoutingUri();
        if (uri == null) uri = "";
        return Collections.singletonList( transformValue(uri) );
    }

    @Override
    protected void updateServiceValues( final PublishedService service,
                                        final List<String> targetValues ) {
        rwlock.writeLock().lock();
        try {
            for ( final String targetValue : targetValues ) {
                createnolock(service, targetValue);
            }
            knownToFail.clear();
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    private void createnolock( final PublishedService service, final String targetValue ) {
        final URIResolutionParam uriparam = new URIResolutionParam(targetValue);
        List<Goid> listedServicesForThatURI = uriToServiceMap.get(uriparam);
        if (listedServicesForThatURI == null) {
            listedServicesForThatURI = new ArrayList<Goid>();
            uriToServiceMap.put(uriparam, listedServicesForThatURI);
        }
        listedServicesForThatURI.add(service.getGoid());
        servicetoURIMap.put(service.getGoid(), uriparam);
    }

    private void deletenolock(PublishedService service) {
        URIResolutionParam uriparam = servicetoURIMap.get(service.getGoid());
        if (uriparam == null) {
            return; // already deleted
        }
        List<Goid> listedServicesForThatURI = uriToServiceMap.get(uriparam);
        listedServicesForThatURI.remove(service.getGoid());
        servicetoURIMap.remove(service.getGoid());
    }

    private static URIResolutionParam whichOneIsBest(List<URIResolutionParam> in,
                                              boolean containsPathPattern,
                                              boolean containsExtensionPattern) {
        // eliminate /* if present
        in.remove(CATCHALLRESOLUTION);
        if (in.size() == 1) return in.get(0);

        // eliminate extensions if paths exist
        if (containsPathPattern && containsExtensionPattern) {
            for (Iterator<URIResolutionParam> iterator = in.iterator(); iterator.hasNext();) {
                URIResolutionParam p = iterator.next();
                if (!p.pathPattern) iterator.remove();
            }
        }
        if (in.size() == 1) return in.get(0);

        // choose longest
        long longestlength = 0;
        URIResolutionParam output = null;
        for (URIResolutionParam p : in) {
            if (p.uri.length() > longestlength) {
                output = p;
                longestlength = p.uri.length();
            }
        }
        return output;
    }

    private String getRequestValue(Message request) throws ServiceResolutionException {
        UriKnob uriKnob = request.getKnob(UriKnob.class);
        if (uriKnob == null)
            throw new ServiceResolutionException("Unable to access request URI.");
        String uri = uriKnob.getRequestUri();
        
        HttpRequestKnob httpReqKnob = request.getKnob(HttpRequestKnob.class);
        if (httpReqKnob != null) {
            final String originalUrl = enableOriginalUrlHeader.get() ?
                    httpReqKnob.getHeaderFirstValue(SecureSpanConstants.HttpHeaders.ORIGINAL_URL) :
                    null;
            if (originalUrl != null) {
                try {
                    URL url = new URL(originalUrl);
                    uri = url.getFile();
                    if (uri.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX) || uri.equals("/")) uri = "";
                    auditor.logAndAudit(MessageProcessingMessages.SR_HTTPURI_URI_FROM_HEADER, uri);
                    return uri;
                } catch (MalformedURLException e) {
                    String err = MessageFormat.format("Invalid L7-Original-URL value: ''{0}''", originalUrl);
                    logger.log(Level.WARNING, err, e);
                    throw new ServiceResolutionException(err);
                }
            }
        }

        if (uri == null || uri.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX)) uri = "";
        auditor.logAndAudit(MessageProcessingMessages.SR_HTTPURI_REAL_URI, uri);
        return uri;
    }

    /**
     * Check whether the specified request is expected to provide a URI that can be resolved.
     * <p/>
     * This method returns true if the message has an HttpRequestKnob or an FtpRequestKnob.
     *
     * @param request the request to examine.  Required.
     * @return true if the request arrived over a transport that provides a request URI.
     */
    public boolean appliesToMessage(Message request) {
        return canResolveByURI( request );
    }

    public static boolean canResolveByURI( final Message request ) {
        return request.getKnob(UriKnob.class) != null;
    }

    public static class URIResolutionParam {
        public URIResolutionParam(String uri) {
            while (uri.indexOf("**") >= 0) {
                uri = uri.replace("**", "*");
            }
            this.uri = uri;
            if (uri.indexOf('*') > 0) {
                this.pathPattern = uri.charAt(uri.length() - 1) == '*';
                // Quote all regex metacharacters, but allow modified asterix under controlled conditions
                String tmp = Pattern.quote(uri).replace("*", "\\E.*\\Q");
                pattern = Pattern.compile(tmp);
                this.hasWildcards = true;
                this.globalWildcard = "/*".equals(uri);
            } else {
                // no wildcard case
                this.pattern = uri;
                this.hasWildcards = false;
                this.globalWildcard = false;
                this.pathPattern = true;
            }
        }
        public String toString() {
            return uri;
        }
        final String uri;
        final Object pattern;
        final boolean hasWildcards;
        final boolean globalWildcard;
        final boolean pathPattern; // as opposed to a filetype pattern

        public boolean equals(Object o) {
            if (this == o) return true;
            // allow equality based on simple string
            if (o instanceof String) {
                return o.equals(this.uri);
            }
            if (o == null || getClass() != o.getClass()) return false;
            final URIResolutionParam that = (URIResolutionParam) o;
            return !(uri != null ? !uri.equals(that.uri) : that.uri != null);
        }

        public int hashCode() {
            return uri.hashCode();
        }
    }

    /**
     * Boolean property for exact matching only.
     */
    protected static final String SUFFIX_EXACT_ONLY = ".exactOnly";

    protected final String PROP_EXACT_ONLY = PROP_BASE + SUFFIX_EXACT_ONLY;

    private final boolean caseSensitive;
    private final ArrayList<String> knownToFail = new ArrayList<String>();
    private final Map<URIResolutionParam, List<Goid>> uriToServiceMap = new HashMap<URIResolutionParam, List<Goid>>();
    private final Map<Goid, URIResolutionParam> servicetoURIMap = new HashMap<Goid, URIResolutionParam>();
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock(false);
    private static final URIResolutionParam CATCHALLRESOLUTION = new URIResolutionParam("/*");
    private final AtomicBoolean enableCaseSensitivity = new AtomicBoolean(true);
    private final AtomicBoolean enableOriginalUrlHeader = new AtomicBoolean(true);
}
