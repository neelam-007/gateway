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

    protected Object[] getTargetValues( PublishedService service ) {
        // TODO: Multiple URNs?
        return new Object[] { service.getUrn() };
    }

    protected Object getRequestValue( Request request ) throws ServiceResolutionException {
        try {
            Element body = SoapUtil.getBodyElement( request );
            Node n = body.getFirstChild();
            while ( n != null ) {
                if ( n.getNodeType() == Node.ELEMENT_NODE )
                    return n.getNamespaceURI();

                n = n.getNextSibling();
            }
            return null;
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
