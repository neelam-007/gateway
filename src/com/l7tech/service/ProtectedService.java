/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import java.util.Set;

/**
 * @author alex
 */
public class ProtectedService extends Service {
    /**
     * Constructs a ProtectedService with a pre-existing Set of Operations.
     *
     * @param operations
     */
    public ProtectedService( String name, Set operations ) {
        super( name, operations );
        _wsdlUrl = null;
    }

    /**
     * Constructs a ProtectedService by parsing a WSDL document referred to by an URL.  This method will probably take some time to complete!
     *
     * @param wsdlUrl A String containing an URL to a WSDL document.
     */
    public ProtectedService( String wsdlUrl ) {
        _wsdlUrl = wsdlUrl;
        // TODO: Call the bloody WSDL parser
    }

    protected String _wsdlUrl;
    protected Service _service;
}
