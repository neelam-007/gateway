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
class AttributeStatement extends SubjectStatement {
    private final Attribute[] attributes;

    public AttributeStatement(LoginCredentials credentials, Confirmation confirmation, Attribute[] attributes) {
        super(credentials, confirmation);
        this.attributes = attributes;
    }

    public Attribute[] getAttributes() {
        return attributes;
    }

    public static class Attribute {
        private final String name;
        private final String nameSpace;
        private final String value;

        public Attribute(String name, String nameSpace, String value) {
            this.name = name;
            this.nameSpace = nameSpace;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getNameSpace() {
            return nameSpace;
        }

        public String getValue() {
            return value;
        }
    }
}