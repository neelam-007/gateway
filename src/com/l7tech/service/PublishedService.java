/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * @author alex
 */
public class PublishedService extends NamedEntityImp {
    public Assertion rootAssertion() {
        if ( _rootAssertion == null ) {
            // TODO: Parse the policy
        }
        return _rootAssertion;
    }

    public ProtectedService getProtectedService() {
        return _protectedService;
    }

    public void setProtectedService( ProtectedService protServ ) {
        _protectedService = protServ;
    }

    public String getPolicyXml() {
        return _policyXml;
    }

    public void setPolicyXml( String policyXml ) {
        _policyXml = policyXml;
        _rootAssertion = null;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    protected String _policyXml;
    protected ProtectedService _protectedService;

    protected transient Assertion _rootAssertion;
}
