/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.identity.mapping;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UsersOrGroups;
import com.l7tech.objectmodel.AttributeHeader;

/**
 * Mappings of {@link AttributeConfig}s to LDAP identity providers
 */
public class LdapAttributeMapping extends IdentityMapping {
    public static AttributeHeader[] getBuiltinAttributes() {
        AttributeHeader[] supers = IdentityMapping.getBuiltinAttributes();
        AttributeHeader[] my = new AttributeHeader[supers.length+1];
        System.arraycopy(supers, 0, my, 0, supers.length);
        my[my.length-1] = AttributeHeader.SUBJECT_DN;
        return my;
    }

    public LdapAttributeMapping() {
        super(null, IdentityProviderConfig.DEFAULT_GOID, UsersOrGroups.USERS);
    }

    public LdapAttributeMapping(AttributeConfig parent, Goid providerOid, UsersOrGroups uog) {
        super(parent, providerOid, uog);
    }
}
