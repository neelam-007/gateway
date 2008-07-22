/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.message.FtpRequestKnob;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.service.PublishedService;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    public UriResolver(ApplicationContext spring) {
        super(spring);
    }

    public boolean usesMessageContent() {
        return false;
    }

    public static Result doResolve(String requestValue, Collection<PublishedService> serviceSubset,
                                 Map<URIResolutionParam, List<Long>> uriToServiceMap, Auditor auditor) {
        List<Long> res = uriToServiceMap.get(new URIResolutionParam(requestValue));
        if (res != null && res.size() > 0) {
            if (auditor != null) {
                auditor.logAndAudit(MessageProcessingMessages.SR_HTTPURI_PERFECT, requestValue);
            }
            Set<PublishedService> output = narrowList(serviceSubset, res);
            if (output.size() > 0) return new Result(output); // otherwise, we continue and try to find match using wildcard ones
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
                    if (((Pattern) key.pattern).matcher(requestValue).matches()) {
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

    public Result resolve(Message request, Collection<PublishedService> serviceSubset) throws ServiceResolutionException {
        rwlock.readLock().lock();
        try {
            // since this only applies to http messages, we dont want to narrow down subset if msg is not http
            boolean notHttp = (request.getKnob(HttpRequestKnob.class) == null);
            boolean notFtp = (request.getKnob(FtpRequestKnob.class) == null);
            if (notHttp && notFtp) {
                return Result.NOT_APPLICABLE;
            } else {
                String requestValue = getRequestValue(request);
                // first look at repetitive failures
                if (knownToFail.contains(requestValue)) { // why is this suspicious?
                    auditor.logAndAudit(MessageProcessingMessages.SR_HTTPURI_CACHEDFAIL, requestValue);
                    return Result.NO_MATCH;
                }
                Result res = doResolve(requestValue, serviceSubset, uriToServiceMap, auditor);
                if (res == Result.NO_MATCH) {
                    // todo, this could be exploted as an attack. we should either not try to do this or
                    // we should have a worker thread making sure this does not grow too big
                    knownToFail.add(requestValue);
                }
                return res;
            }
        } finally {
            rwlock.readLock().unlock();
        }
    }

    private static boolean isInSubset(Collection<PublishedService> serviceSubset, List<Long> criteria) {
        for (PublishedService svc : serviceSubset) {
            if (criteria.contains(svc.getOidAsLong())) {
                return true;
            }
        }
        return false;
    }

    private static Set<PublishedService> narrowList(Collection<PublishedService> serviceSubset, List<Long> criteria) {
        Set<PublishedService> output = new HashSet<PublishedService>();
        for (PublishedService svc : serviceSubset) {
            if (criteria.contains(svc.getOidAsLong())) {
                output.add(svc);
            }
        }
        return output;
    }

    public void serviceCreated(PublishedService service) {
        rwlock.writeLock().lock();
        try {
            createnolock(service);
            knownToFail.clear();
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public void serviceDeleted(PublishedService service) {
        rwlock.writeLock().lock();
        try {
            deletenolock(service);
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public void serviceUpdated(PublishedService service) {
        rwlock.writeLock().lock();
        try {
            deletenolock(service);
            createnolock(service);
            knownToFail.clear();
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public Set<String> getDistinctParameters(PublishedService candidateService) {
        throw new UnsupportedOperationException();
    }

    public boolean isSoap() {
        return false;
    }

    public String doGetTargetValue(PublishedService service) {
        String uri = service.getRoutingUri();
        if (uri == null) uri = "";
        return uri;
    }

    private URIResolutionParam getTargetValue(PublishedService service) {
        return new URIResolutionParam(doGetTargetValue(service));
    }

    private void createnolock(PublishedService service) {
        URIResolutionParam uriparam = getTargetValue(service);
        List<Long> listedServicesForThatURI = uriToServiceMap.get(uriparam);
        if (listedServicesForThatURI == null) {
            listedServicesForThatURI = new ArrayList<Long>();
            uriToServiceMap.put(uriparam, listedServicesForThatURI);
        }
        listedServicesForThatURI.add(service.getOid());
        servicetoURIMap.put(service.getOid(), uriparam);
    }

    private void deletenolock(PublishedService service) {
        URIResolutionParam uriparam = servicetoURIMap.get(service.getOid());
        if (uriparam == null) {
            logger.warning("deletion invoked but service does not seem to exist in resolver cache " + service.getOid());
            return;
        }
        List<Long> listedServicesForThatURI = uriToServiceMap.get(uriparam);
        listedServicesForThatURI.remove(service.getOid());
        servicetoURIMap.remove(service.getOid());
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
        HttpRequestKnob httpReqKnob = (HttpRequestKnob)request.getKnob(HttpRequestKnob.class);
        if (httpReqKnob == null) {
            FtpRequestKnob ftpReqKnob = (FtpRequestKnob)request.getKnob(FtpRequestKnob.class);
            if (ftpReqKnob == null) return null;
            String uri = ftpReqKnob.getRequestUri();
            if (uri.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX)) uri = "";
            auditor.logAndAudit(MessageProcessingMessages.SR_HTTPURI_REAL_URI, uri);
            return uri;
        }
        String originalUrl;
        try {
            originalUrl = httpReqKnob.getHeaderSingleValue(SecureSpanConstants.HttpHeaders.ORIGINAL_URL);
        } catch (IOException e) {
            throw new ServiceResolutionException("Found multiple " + SecureSpanConstants.HttpHeaders.ORIGINAL_URL + " values"); // can't happen
        }
        if (originalUrl == null) {
            String uri = httpReqKnob.getRequestUri();
            if (uri == null || uri.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX)) uri = "";
            auditor.logAndAudit(MessageProcessingMessages.SR_HTTPURI_REAL_URI, uri);
            return uri;
        } else {
            try {
                URL url = new URL(originalUrl);
                String uri = url.getFile();
                if (uri.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX) || uri.equals("/")) uri = "";
                auditor.logAndAudit(MessageProcessingMessages.SR_HTTPURI_URI_FROM_HEADER, uri);
                return uri;
            } catch (MalformedURLException e) {
                String err = MessageFormat.format("Invalid L7-Original-URL value: ''{0}''", originalUrl);
                logger.log( Level.WARNING, err, e );
                throw new ServiceResolutionException( err );
            }
        }
    }

    public static class URIResolutionParam {
        public URIResolutionParam(String uri) {
            while (uri.indexOf("**") >= 0) {
                uri = uri.replace("**", "*");
            }
            this.uri = uri;
            if (uri.indexOf('*') > 0) {
                this.pathPattern = uri.charAt(uri.length() - 1) == '*';
                String tmp = uri.replace(".", "\\.");
                tmp = tmp.replace("*", ".*");
                tmp = tmp.replace("?", "\\?");
                tmp = tmp.replace("$", "\\$");
                pattern = Pattern.compile(tmp);
                this.hasWildcards = true;
            } else {
                // no wildcard case
                this.pattern = uri;
                this.hasWildcards = false;
                this.pathPattern = true;
            }
        }
        public String toString() {
            return uri;
        }
        final String uri;
        final Object pattern;
        final boolean hasWildcards;
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

    private final ArrayList<String> knownToFail = new ArrayList<String>();
    private final Map<URIResolutionParam, List<Long>> uriToServiceMap = new HashMap<URIResolutionParam, List<Long>>();
    private final Map<Long, URIResolutionParam> servicetoURIMap = new HashMap<Long, URIResolutionParam>();
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock(false);
    private static final URIResolutionParam CATCHALLRESOLUTION = new URIResolutionParam("/*");
}
