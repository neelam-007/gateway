/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.resolution;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.message.Request;
import com.l7tech.service.PublishedService;

import javax.wsdl.Port;
import javax.wsdl.WSDLException;
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
        try {
            Wsdl wsdl = service.parsedWsdl();
            Port soapPort = wsdl.getSoapPort();
            URL url = null;
            if ( soapPort != null ) url = wsdl.getUrlFromPort( soapPort );
            if ( url == null )
                return new String[0];
            else
                return new String[] { url.getFile() };
        } catch ( WSDLException we ) {
            logger.throwing( getClass().getName(), "getTargetValues", we );
            return new String[0];
        } catch ( MalformedURLException mue ) {
            logger.throwing( getClass().getName(), "getTargetValues", mue );
            return new String[0];
        }
    }

    protected Object getRequestValue(Request request) throws ServiceResolutionException {
        String originalUrl = (String)request.getParameter( Request.PARAM_HTTP_ORIGINAL_URL );
        if ( originalUrl == null )
            return (String)request.getParameter( Request.PARAM_HTTP_REQUEST_URI );
        else {
            try {
                URL url = new URL( originalUrl );
                return url.getFile();
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
