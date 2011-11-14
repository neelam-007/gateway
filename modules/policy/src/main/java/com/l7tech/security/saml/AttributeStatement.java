/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.security.saml;

import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.xml.KeyInfoInclusionType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class AttributeStatement extends SubjectStatement {
    private Attribute[] attributes;

    public AttributeStatement() { }

    public AttributeStatement(@NotNull LoginCredentials credentials,
                              Confirmation confirmation,
                              Attribute[] attributes,
                              KeyInfoInclusionType keyInfoType,
                              NameIdentifierInclusionType nameIdType,
                              String overrideNameValue,
                              String overrideNameFormat,
                              String nameQualifier)
    {
        super(credentials, confirmation, keyInfoType, nameIdType, overrideNameValue, overrideNameFormat, nameQualifier);
        this.attributes = attributes;
    }

    public Attribute[] getAttributes() {
        return attributes;
    }

    public void setAttributes(Attribute[] attributes) {
        this.attributes = attributes;
    }
}