/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.saml;

import com.l7tech.policy.assertion.credential.LoginCredentials;

/**
 * @author emil
 * @version Feb 1, 2005
 */
public class AuthorizationStatement extends SubjectStatement {
    private String resource;
    private String action;
    private String actionNamespace;

    public AuthorizationStatement(LoginCredentials credentials,
                                  Confirmation confirmation,
                                  String resource, String action,
                                  String actionNamespace,
                                  KeyInfoInclusionType keyInfoType,
                                  NameIdentifierInclusionType nameIdType,
                                  String overrideNameValue,
                                  String overrideNameFormat)
    {
        super(credentials, confirmation, keyInfoType, nameIdType, overrideNameValue, overrideNameFormat);
        this.resource = resource;
        this.action = action;
        this.actionNamespace = actionNamespace;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActionNamespace() {
        return actionNamespace;
    }

    public void setActionNamespace(String actionNamespace) {
        this.actionNamespace = actionNamespace;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }
}