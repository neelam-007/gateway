/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.protect;

import com.l7tech.service.Service;

import java.util.Set;

/**
 * @author alex
 */
public class ProtectedService {
    public ProtectedService( Service service ) {
        _service = service;
        _wsdlUrl = null;
    }

    public ProtectedService( Service service, String wsdlUrl ) {
        _service = service;
        _wsdlUrl = wsdlUrl;
    }

    protected String _wsdlUrl;
    protected Service _service;
}
