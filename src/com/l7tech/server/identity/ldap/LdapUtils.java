/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.ldap;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.NamingException;

/**
 * @author alex
 */
final class LdapUtils {
    private LdapUtils() { }

    static boolean attrContainsCaseIndependent(Attribute attr, String valueToLookFor) {
        return attr.contains(valueToLookFor) || attr.contains(valueToLookFor.toLowerCase());
    }

    static Object extractOneAttributeValue(Attributes attributes, String attrName) throws NamingException {
        Attribute valuesWereLookingFor = attributes.get(attrName);
        if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
            return valuesWereLookingFor.get(0);
        }
        return null;
    }
}
