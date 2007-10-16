/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.saml;

import com.l7tech.policy.assertion.credential.LoginCredentials;

/**
 *
 */
public class AttributeStatement extends SubjectStatement {
    private Attribute[] attributes;

    public AttributeStatement() { }

    public AttributeStatement(LoginCredentials credentials,
                              Confirmation confirmation,
                              Attribute[] attributes,
                              KeyInfoInclusionType keyInfoType,
                              NameIdentifierInclusionType nameIdType,
                              String overrideNameValue,
                              String overrideNameFormat)
    {
        super(credentials, confirmation, keyInfoType, nameIdType, overrideNameValue, overrideNameFormat);
        this.attributes = attributes;
    }

    public Attribute[] getAttributes() {
        return attributes;
    }

    public void setAttributes(Attribute[] attributes) {
        this.attributes = attributes;
    }
}