/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.policy.assertion.Assertion;

/**
 * @author alex
 */
public class PublishedService {

    public long getOid() {
        return oid;
    }

    public void setOid(long oid) {
        this.oid = oid;
    }

    public Assertion getRootAssertion() {
        return _rootAssertion;
    }

    public void setRootAssertion(Assertion _rootAssertion) {
        this._rootAssertion = _rootAssertion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProtectedService getProtectedService() {
        return _protectedService;
    }

    public void setProtectedService( ProtectedService protServ ) {
        _protectedService = protServ;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    protected long oid;
    protected Assertion _rootAssertion;
    protected String name;
    protected ProtectedService _protectedService;
}
