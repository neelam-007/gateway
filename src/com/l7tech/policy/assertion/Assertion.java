/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.util.Locator;

import java.io.Serializable;

/**
 * Immutable except for de-persistence.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class Assertion implements Cloneable, Serializable {
    protected CompositeAssertion parent;

    /**
     * SSG Server-side processing of the given request.
     * @param request       (In/Out) The request to check.  May be modified by processing.
     * @param response      (Out) The response to send back.  May be replaced during processing.
     * @return AssertionError.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public abstract AssertionError checkRequest( Request request, Response response ) throws PolicyAssertionException;

    /**
     * ClientProxy client-side processing of the given request.
     * @param request    The request to decorate.
     * @return AssertionError.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public abstract AssertionError decorateRequest( PendingRequest request ) throws PolicyAssertionException;

    public Assertion() {
        this.parent = null;
    }

    public Assertion(CompositeAssertion parent) {
        this.parent = parent;
    }

    public CompositeAssertion getParent() {
        return parent;
    }

    public void setParent(CompositeAssertion parent) {
        this.parent = parent;
    }

    /** Properly clone this Assertion.  The clone will have its parent set to null. */
    public Object clone() throws CloneNotSupportedException {
        Assertion clone = (Assertion)super.clone();
        clone.setParent(null);
        return clone;
    }

    public String toString() {
        return "<" + this.getClass().getName() + ">";
    }
}
