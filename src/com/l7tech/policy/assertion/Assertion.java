/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;

import java.io.Serializable;

/**
 * Immutable except for de-persistence.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class Assertion implements Serializable {
    public abstract AssertionError checkRequest( Request request, Response response ) throws PolicyAssertionException;

    public String toString() {
        return "<" + this.getClass().getName() + ">";
    }
}
