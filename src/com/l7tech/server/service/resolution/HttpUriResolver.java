/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service.resolution;

import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.service.PublishedService;

import java.io.IOException;
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

    protected Object[] doGetTargetValues( PublishedService service ) {
        String uri = service.getRoutingUri();
        if (uri == null) uri = "";
        return new String[] {uri};
    }

    protected Object getRequestValue(Message request) throws ServiceResolutionException {
        HttpRequestKnob httpReqKnob = (HttpRequestKnob)request.getKnob(HttpRequestKnob.class);
        if (httpReqKnob == null) return null;
        String originalUrl = null;
        try {
            originalUrl = httpReqKnob.getHeaderSingleValue(SecureSpanConstants.HttpHeaders.ORIGINAL_URL);
        } catch (IOException e) {
            throw new ServiceResolutionException("Found multiple " + SecureSpanConstants.HttpHeaders.ORIGINAL_URL + " values"); // can't happen
        }
        if (originalUrl == null) {
            String uri = httpReqKnob.getRequestUri();
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
