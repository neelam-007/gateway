/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.resolution;

import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.service.PublishedService;
import com.l7tech.service.Wsdl;

import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpUriResolver extends NameValueServiceResolver {
    protected String getParameterName() {
        return Request.PARAM_HTTP_REQUEST_URI;
    }

    protected Object[] getTargetValues( PublishedService service ) {
        try {
            Wsdl wsdl = service.parsedWsdl();
            Port soapPort = wsdl.getSoapPort();
            URL url = wsdl.getUrlFromPort( soapPort );
            if ( url == null )
                return new String[0];
            else
                return new String[] { url.getFile() };
        } catch ( WSDLException we ) {
            _log.throwing( getClass().getName(), "getTargetValues", we );
            return new String[0];
        } catch ( MalformedURLException mue ) {
            _log.throwing( getClass().getName(), "getTargetValues", mue );
            return new String[0];
        }
    }

    protected Object getRequestValue(Request request) throws ServiceResolutionException {
        return request.getParameter( Request.PARAM_HTTP_REQUEST_URI );
    }

    public int getSpeed() {
        return FAST;
    }

    protected Logger _log = LogManager.getInstance().getSystemLogger();
}
