/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * This is a SOAPWebService because there are <i>theoretically</i> non-web SOAP services.
 *
 * @author alex
 * @version $Revision$
 */
public class SOAPWebService extends WebService {
    public SOAPWebService( Set operations, String hostname, int port, String uri, String urn ) {
        super( operations, WebService.METHOD_POST, hostname, port, uri );
        _urn = urn;
    }

    public SOAPWebService( Set operations, URL url, String urn ) {
        super( operations, WebService.METHOD_POST, url );
        _urn = urn;
    }

    protected String _urn;
}
