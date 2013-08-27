package com.l7tech.server.wsdm.util;

import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.server.wsdm.faults.ResourceUnknownFault;
import com.l7tech.gateway.common.service.PublishedService;

import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: May 23, 2008
 * Time: 3:08:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class EsmUtils {

    private static final Logger logger = Logger.getLogger(EsmUtils.class.getName());

    public final static Pattern serviceOidQueryStringPattern = Pattern.compile(".*serviceoid=([0-9a-fA-F]{32}|\\d{1,20})(&.*)?$");
    public final static Pattern serviceOidByUrl = Pattern.compile(".*/service/([0-9a-fA-F]{32}|\\d{1,20})([/\\?].*)?$");
    public final static Pattern serviceUriQueryStringPattern = Pattern.compile(".*serviceuri=(.*)");

    public static String determineServiceFromUrl(String url, ServiceCache serviceCache) throws ResourceUnknownFault {

        String serviceOid = getServiceOidFromQueryString(url);

        if (serviceOid == null) {
            serviceOid = getServiceOidFromServiceUrl(url);
        }

        if (serviceOid == null) {
            serviceOid = getServiceOidFromUriQueryString(url, serviceCache);
        }

        return serviceOid;
    }

    public static String getServiceOidFromQueryString(String url) {
        Matcher matcher = serviceOidQueryStringPattern.matcher(url);
        if (matcher.find() && matcher.groupCount() == 2) {
            return matcher.group(1);
        }

        return null;
    }

    public static String getServiceOidFromServiceUrl(String url) {
        Matcher matcher = serviceOidByUrl.matcher(url);
        if (matcher.find() && matcher.groupCount() == 2) {
            return matcher.group(1);
        }

        return null;
    }

    public static String getServiceOidFromUriQueryString(String url, ServiceCache serviceCache) throws ResourceUnknownFault {
        Matcher matcher = serviceUriQueryStringPattern.matcher(url);
        if (matcher.find() && matcher.groupCount() == 1) {
            String serviceUri = matcher.group(1);
            logger.info("Looking up service(s) that match uri prefix " + serviceUri);
            try {
                Collection<PublishedService> foundServices = serviceCache.resolve(serviceUri, null, null);
                if (foundServices == null || foundServices.isEmpty()) {
                    logger.warning("No services were found matching uri prefix " + serviceUri);
                    return null;
                }

                if (foundServices.size() > 1) {
                    throw new ResourceUnknownFault("Multiple services match uri prefix " + serviceUri + ". Use serviceoid instead");
                } else {
                    PublishedService ps = foundServices.iterator().next();
                    String id = ps.getId();
                    logger.info("Found matching service (name=" + ps.getName() + ",id=" + id + ") for uri prefix " + serviceUri + ".");
                    return ps.getId();
                }
            } catch ( ServiceResolutionException e ) {
                logger.log( Level.WARNING, "Error finding services for uri prefix " + serviceUri, e );
                return null;
            }
        }

        return null;
    }
}
