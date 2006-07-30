/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.ldap;

/**
 * @author alex
*/
class LdapSearchTerm {
    public LdapSearchTerm(String objectclass, String searchAttribute, String searchValue) {
        this.objectclass = objectclass;
        this.searchAttribute = searchAttribute;
        this.searchValue = searchValue;
    }

    final String objectclass;
    final String searchAttribute;
    final String searchValue;
}
