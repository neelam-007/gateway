/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.security.saml;

import com.l7tech.policy.assertion.credential.LoginCredentials;

/**
 * @author emil
 * @version Feb 1, 2005
 */
class AuthorizationStatement extends SubjectStatement {
    private final String resource;
    private final String action;
    private final String actionNamespace;

    public AuthorizationStatement(LoginCredentials credentials,
                                  Confirmation confirmation,
                                  String resource, String action, String actionNamespace) {
        super(credentials, confirmation);
        this.resource = resource;
        this.action = action;
        this.actionNamespace = actionNamespace;
    }

    public String getAction() {
        return action;
    }

    public String getActionNamespace() {
        return actionNamespace;
    }

    public String getResource() {
        return resource;
    }
}