/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.message.Request;
import com.l7tech.message.Response;

import java.util.Map;

/**
 * @author alex
 */
public class IdentityAssertion extends Assertion {
    public String getEntity() {
        return _entity;
    }

    public void setEntity(String entity) {
        _entity = entity;
    }

    public IdentityProvider getIdentityProvider() {
        return _identityProvider;
    }

    public void setIdentityProvider(IdentityProvider identityProvider) {
        _identityProvider = identityProvider;
    }

    public int checkRequest(Request request, Response response) throws PolicyAssertionExcepion {
        return 0;
    }

    public void init(Map params) {
    }

    protected IdentityProvider _identityProvider;
    protected String _entity;
}
