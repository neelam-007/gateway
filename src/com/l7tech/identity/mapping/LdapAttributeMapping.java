/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.identity.mapping;

import com.l7tech.identity.IdentityProviderConfig;

/**
 * Mappings of {@link AttributeConfig}s to LDAP identity providers
 */
public class LdapAttributeMapping extends IdentityMapping {
    public static AttributeHeader[] getBuiltinAttributes() {
        AttributeHeader[] supers = IdentityMapping.getBuiltinAttributes();
        AttributeHeader[] my = new AttributeHeader[supers.length+2];
        System.arraycopy(supers, 0, my, 0, supers.length);
        my[my.length-2] = AttributeHeader.ID;
        my[my.length-1] = AttributeHeader.SUBJECT_DN;
        return my;
    }

    public LdapAttributeMapping() {
        super(null, IdentityProviderConfig.DEFAULT_OID, UsersOrGroups.USERS);
    }

    public LdapAttributeMapping(AttributeConfig parent, long providerOid, UsersOrGroups uog) {
        super(parent, providerOid, uog);
    }
}
