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
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves services based on the HTTP URI from which the incoming message came in through. Different services
 * can be assigned a resolution URI. By default, a service is not assigned a resolution URI. For resolution purposes
 * this special case is assigned the value of "". Requests coming in the ssg at a URI starting with
 * SecureSpanConstants.SSG_RESERVEDURI_PREFIX are considered as URIs of "".
 *
 * The wildcard '*' is allowed when assigning resolution URIs to services, these wildcards are considered at
 * resolution time.
 *
 * Say two services published at /service1* and /service1/foo*, if a message comes in at /service1/foo/bar then both resolution uris match. To resolve these conflicts, we adopt the mechanism described at http://www.roguewave.com/support/docs/leif/leif/html/servletug/7-3.html#732:
 *
 * 1. we prefer an exact path match over a wildcard path match
 * 2. we finally prefer path matches over filetype matches
 * 3. we then prefer to match the longest pattern
 *
 * @author franco
 */
public class HttpUriResolver extends NameValueServiceResolver {

    protected class URIResolutionParam {
        URIResolutionParam(final String uri) {
            this.uri = uri;
            knownToFail.clear();
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

    protected Object[] doGetTargetValues( PublishedService service ) {
        String uri = service.getRoutingUri();
        if (uri == null) uri = "";
        return new Object[] {new URIResolutionParam(uri)};
    }

    public Set<PublishedService> resolve(Message request, Set<PublishedService> serviceSubset)
                                                                throws ServiceResolutionException {
        // since this only applies to http messages, we dont want to narrow down subset if msg is not http
        boolean notHttp = (request.getKnob(HttpRequestKnob.class) == null);
        if (notHttp) {
            return serviceSubset;
        } else {
            return super.resolve(request, serviceSubset);
        }
    }

    // todo, cleanup this messy code instead of trying to make this fit the NameValueServiceResolver mold
    protected Map<Long, PublishedService> getServiceMap(Object value) {
        Lock read = _rwlock.readLock();
        read.lock();
        try {
            // when this is not called at resolution time... see note above
            if (!(value instanceof String)) {
                Map<Long, PublishedService> serviceMap = _valueToServiceMapMap.get(value);
                if ( serviceMap == null ) {
                    serviceMap = new HashMap<Long, PublishedService>();
                    read.unlock();
                    read = null;
                    _rwlock.writeLock().lock();
                    try {
                        _valueToServiceMapMap.put( value, serviceMap );
                    } finally {
                        _rwlock.writeLock().unlock();
                    }
                } else {
                    read.unlock();
                    read = null;
                }
                return serviceMap;
            }

            // resolution time check
            // first look at repetitive failures
            if (knownToFail.contains(value)) { // why is this suspicious?
                logger.fine("cached failure @" + value.toString());
                return EMPTYSERVICEMAP;
            }

            // second, look for exact matches
            Map<Long, PublishedService> serviceMap = _valueToServiceMapMap.get(new URIResolutionParam((String)value));
            if (serviceMap != null && serviceMap.keySet().size() > 0) {
                logger.fine("we found a perfect non wildcard match for " + value.toString());
                return serviceMap;
            }

            // last, look for possible regex matches
            ArrayList<URIResolutionParam> matchingRegexKeys = new ArrayList<URIResolutionParam>();
            Set keys = _valueToServiceMapMap.keySet();
            boolean encounteredPathPattern = false;
            boolean encounteredExtensionPattern = false;
            for (Object key : keys) {
                URIResolutionParam p = (URIResolutionParam) key;
                if (p.hasWildcards) {
                    if (((Pattern) p.pattern).matcher((String) value).matches()) {
                        if (p.pathPattern) encounteredPathPattern = true;
                        else encounteredExtensionPattern = true;
                        matchingRegexKeys.add(p);
                    }
                }
            }
            if (matchingRegexKeys.size() <= 0) {
                knownToFail.add((String)value);
                logger.fine("no matching possible with uri " + value.toString());
                return EMPTYSERVICEMAP;
            } else if (matchingRegexKeys.size() == 1) {
                logger.fine("one wildcard match with uri " + value.toString());
                return _valueToServiceMapMap.get(matchingRegexKeys.get(0));
            } else {
                // choose best match
                logger.fine("multiple wildcard matches. let's pick the best one with uri " + value.toString());
                URIResolutionParam res = whichOneIsBest(matchingRegexKeys,
                                                        encounteredPathPattern,
                                                        encounteredExtensionPattern);
                return _valueToServiceMapMap.get(res);
            }
        } finally {
            if (read != null) read.unlock();
        }
    }

    // todo, cleanup this messy code instead of trying to make this fit the NameValueServiceResolver mold
    Set<PublishedService> resolve(Object value, Set serviceSubset) throws ServiceResolutionException {
        Map<Long, PublishedService> serviceMap = getServiceMap( value );
        if (serviceMap == null || serviceMap.isEmpty()) return Collections.emptySet();
        Set<PublishedService> resultSet = null;
        for (Long oid : serviceMap.keySet()) {
            PublishedService service = serviceMap.get(oid);
            if (serviceSubset.contains(service)) {
                if (resultSet == null) resultSet = new HashSet<PublishedService>();
                resultSet.add(service);
            }
        }
        if (resultSet == null) resultSet = Collections.emptySet();
        return resultSet;
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

    protected Object getRequestValue(Message request) throws ServiceResolutionException {
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

    public int getSpeed() {
        return FAST;
    }

    public Set getDistinctParameters(PublishedService candidateService) {
        throw new UnsupportedOperationException();
    }

    protected final Logger logger = Logger.getLogger(getClass().getName());
    private static final Map<Long, PublishedService> EMPTYSERVICEMAP = new HashMap<Long, PublishedService>();
    private final ArrayList<String> knownToFail = new ArrayList<String>();
}
