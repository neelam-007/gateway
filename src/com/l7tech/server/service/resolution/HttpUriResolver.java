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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Resolves services based on the HTTP URI from which the incoming message came in through. Different services
 * can be assigned a resolution URI. By default, a service is not assigned a resolution URI. For resolution purposes
 * this special case is assigned the value of "". Requests coming in the ssg at a URI starting with
 * SecureSpanConstants.SSG_RESERVEDURI_PREFIX are considered as URIs of "".
 *
 * Each service has one and only one resolution URI but the same resolution URI can be assigned to multiple services.
 *
 * The wildcard '*' is allowed when assigning resolution URIs to services, these wildcards are considered at
 * resolution time.
 *
 * Say two services published at /service1* and /service1/foo*, if a message comes in at /service1/foo/bar then
 * both resolution uris match. To resolve these conflicts, we adopt the mechanism described at
 * http://www.roguewave.com/support/docs/leif/leif/html/servletug/7-3.html#732:
 *
 * 1. we prefer an exact path match over a wildcard path match
 * 2. we finally prefer path matches over filetype matches
 * 3. we then prefer to match the longest pattern
 *
 * @author franco
 */
public class HttpUriResolver extends ServiceResolver {
    public int getSpeed() {
        return FAST;
    }

    public Set<PublishedService> resolve(Message request,
                                         Set<PublishedService> serviceSubset) throws ServiceResolutionException {
        rwlock.readLock().lock();
        try {
            // since this only applies to http messages, we dont want to narrow down subset if msg is not http
            boolean notHttp = (request.getKnob(HttpRequestKnob.class) == null);
            if (notHttp) {
                return serviceSubset;
            } else {
                String requestValue = getRequestValue(request);
                // first look at repetitive failures
                if (knownToFail.contains(requestValue)) { // why is this suspicious?
                    logger.fine("cached failure @" + requestValue);
                    return EMPTYSERVICESET;
                }

                // second, try to get an exact match
                List<Long> res = uriToServiceMap.get(new URIResolutionParam(requestValue));
                if (res != null && res.size() > 0) {
                    logger.fine("we found a perfect non wildcard match for " + requestValue);
                    Set<PublishedService> output = narrowList(serviceSubset, res);
                    if (output.size() > 0) return output; // otherwise, we continue and try to find match using wildcard ones
                }

                // otherwise, try to match using wildcards
                Set<URIResolutionParam> keys = uriToServiceMap.keySet();
                boolean encounteredPathPattern = false;
                boolean encounteredExtensionPattern = false;
                ArrayList<URIResolutionParam> matchingRegexKeys = new ArrayList<URIResolutionParam>();
                for (URIResolutionParam key : keys) {
                    if (key.hasWildcards) {
                        // only consider cached URI associated to at least one service in the passed subset
                        // this ensures that further calls to narrowList will not yeild empty service sets and
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
                    knownToFail.add((String)requestValue);
                    logger.fine("no matching possible with uri " + requestValue);
                    return EMPTYSERVICESET;
                } else if (matchingRegexKeys.size() == 1) {
                    logger.fine("one wildcard match with uri " + requestValue);
                    res = uriToServiceMap.get(matchingRegexKeys.get(0));
                    return narrowList(serviceSubset, res);
                } else {
                    // choose best match
                    logger.fine("multiple wildcard matches. let's pick the best one with uri " + requestValue);
                    URIResolutionParam best = whichOneIsBest(matchingRegexKeys,
                                                            encounteredPathPattern,
                                                            encounteredExtensionPattern);
                    res = uriToServiceMap.get(best);
                    return narrowList(serviceSubset, res);
                }
            }
        } finally {
            rwlock.readLock().unlock();
        }
    }

    private boolean isInSubset(Set<PublishedService> serviceSubset, List<Long> criteria) {
        for (PublishedService svc : serviceSubset) {
            if (criteria.contains(svc.getOid())) {
                return true;
            }
        }
        return false;
    }

    private Set<PublishedService> narrowList(Set<PublishedService> serviceSubset, List<Long> criteria) {
        Set<PublishedService> output = new HashSet<PublishedService>();
        for (PublishedService svc : serviceSubset) {
            if (criteria.contains(svc.getOid())) {
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

    public void setServices(Set<PublishedService> services) {
        rwlock.writeLock().lock();
        try {
            for (PublishedService svc : services) {
                createnolock(svc);
            }
            knownToFail.clear();
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    protected boolean matches(PublishedService candidateService, PublishedService matchService) {
        return getTargetValue(candidateService).equals(getTargetValue(matchService));
    }

    public Set getDistinctParameters(PublishedService candidateService) {
        throw new UnsupportedOperationException();
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

    private URIResolutionParam whichOneIsBest(List<URIResolutionParam> in,
                                              boolean containsPathPattern,
                                              boolean containsExtensionPattern) {
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
        if (httpReqKnob == null) return null;
        String originalUrl;
        try {
            originalUrl = httpReqKnob.getHeaderSingleValue(SecureSpanConstants.HttpHeaders.ORIGINAL_URL);
        } catch (IOException e) {
            throw new ServiceResolutionException("Found multiple " + SecureSpanConstants.HttpHeaders.ORIGINAL_URL + " values"); // can't happen
        }
        if (originalUrl == null) {
            String uri = httpReqKnob.getRequestUri();
            if (uri == null || uri.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX) || uri.equals("/")) uri = "";
            logger.finest("returning uri " + uri);
            return uri;
        } else {
            try {
                URL url = new URL(originalUrl);
                String uri = url.getFile();
                if (uri.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX) || uri.equals("/")) uri = "";
                logger.finest("returning uri " + uri);
                return uri;
            } catch (MalformedURLException e) {
                String err = "Invalid L7-Original-URL value: '" + originalUrl + "'";
                logger.log( Level.WARNING, err, e );
                throw new ServiceResolutionException( err );
            }
        }
    }

    class URIResolutionParam {
        URIResolutionParam(final String uri) {
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
    private static final Set<PublishedService> EMPTYSERVICESET = new HashSet<PublishedService>();
}
