/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import java.net.URL;
import java.util.Set;

/**
 * This is a SOAPWebService because there are <i>theoretically</i> non-web SOAP services.
 *
 * @author alex
 * @version $Revision$
 */
public class SOAPWebService extends WebService {
    public SOAPWebService( String name, Set operations, String hostname, int port, String uri, String urn ) {
        super( name, operations, WebService.METHOD_POST, hostname, port, uri );
        _urn = urn;
    }

    public SOAPWebService( String name, Set operations, URL url, String urn ) {
        super( name, operations, WebService.METHOD_POST, url );
        _urn = urn;
    }

    /** Default constructor. Only for Hibernate, don't call! */
    public SOAPWebService() {
        super();
    }

    protected String _urn;
}
