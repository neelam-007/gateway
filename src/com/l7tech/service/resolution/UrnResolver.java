/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.resolution;

import com.l7tech.service.PublishedService;
import com.l7tech.message.Request;
import com.l7tech.util.SoapUtil;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class UrnResolver extends NameValueServiceResolver {

    protected String getParameterName() {
        return Request.PARAM_URN;
    }

    protected Object getTargetValue( PublishedService service ) {
        return service.getUrn();
    }

    protected Object getRequestValue( Request request ) throws ServiceResolutionException {
        try {
            Element body = SoapUtil.getBodyElement( request );
            String urn = body.getNamespaceURI();
            return urn;
        } catch ( SAXException se ) {
            throw new ServiceResolutionException( se.getMessage(), se );
        } catch ( IOException ioe ) {
            throw new ServiceResolutionException( ioe.getMessage(), ioe );
        }
    }

    public int getSpeed() {
        return SLOW;
    }
}
