package com.l7tech.identity.attribute;

import com.l7tech.identity.Identity;
import com.l7tech.identity.ldap.LdapGroup;
import com.l7tech.identity.ldap.LdapUser;

public class LdapAttributeMapping extends IdentityMapping {
    private String attributeName;
    
    public Object[] extractValues(Identity identity) {
        if (identity instanceof LdapUser) {
            LdapUser ldapUser = (LdapUser)identity;
            return new Object[0];
        } else if (identity instanceof LdapGroup) {
            LdapGroup group = (LdapGroup)identity;
            return new Object[0];
        } else {
            return new Object[0];
        }
    }
}
