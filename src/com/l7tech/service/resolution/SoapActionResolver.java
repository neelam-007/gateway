/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.resolution;

import com.l7tech.message.Request;
import com.l7tech.service.PublishedService;

/**
 * @author alex
 * @version $Revision$
 */
public class SoapActionResolver extends NameValueServiceResolver {
    public int getSpeed() {
        return FAST;
    }

    protected String getParameterName() {
        return Request.PARAM_SOAPACTION;
    }

    protected Object getTargetValue( PublishedService service ) {
        return service.getSoapAction();
    }

    protected Object getRequestValue( Request request ) {
        return request.getParameter( request.PARAM_SOAPACTION );
    }

}
