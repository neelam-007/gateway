/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service.resolution;

import com.l7tech.message.Request;
import com.l7tech.service.PublishedService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves services based on the URI part of the WSDL's soap:address extensibility
 * element.  Currently relies on all "unknown" requests being mapped to the
 *
 * @author alex
 * @version $Revision$
 */
public class HttpUriResolver extends NameValueServiceResolver {
    protected String getParameterName() {
        return Request.PARAM_HTTP_REQUEST_URI;
    }

    protected Object[] doGetTargetValues( PublishedService service ) {
        String uri = service.getRoutingUri();
        if (uri == null) uri = "";
        return new String[] {uri};
    }

    protected Object getRequestValue(Request request) throws ServiceResolutionException {
        String originalUrl = (String)request.getParameter(Request.PARAM_HTTP_ORIGINAL_URL);
        if (originalUrl == null) {
            String uri = (String)request.getParameter(Request.PARAM_HTTP_REQUEST_URI);
            if (uri == null || !uri.startsWith("/xml")) uri = "";
            logger.finest("returning uri " + uri);
            return uri;
        } else {
            try {
                URL url = new URL( originalUrl );
                String uri = url.getFile();
                if (!uri.startsWith("/xml")) uri = "";
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
}
