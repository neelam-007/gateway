/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;

import java.util.Map;
import java.util.Collections;

/**
 * Immutable except for de-persistence.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class Assertion {
    public Assertion() {
    }

    public Assertion( Map params ) {
        _params = params;
    }

    public Map getParams() {
        return _params;
    }

    public void setParams( Map params ) {
        _params = params;
    }

    public abstract AssertionError checkRequest( Request request, Response response ) throws PolicyAssertionException;

    protected Map _params = Collections.EMPTY_MAP;
}
