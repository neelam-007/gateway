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
 * Immutable.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class Assertion {
    public abstract int checkRequest( Request request, Response response ) throws PolicyAssertionExcepion;
    public void init( Map params ) {
        _params = params;
    }

    protected Map _params = Collections.EMPTY_MAP;
}
