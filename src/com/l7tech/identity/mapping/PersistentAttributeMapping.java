/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.identity.mapping;

import com.l7tech.policy.variable.DataType;

/**
 * Abstract superclass of {@link InternalAttributeMapping} and {@link FederatedAttributeMapping}, incorporating
 * attributes common to both 
 * TODO support custom internal attributes someday
 * @author alex
 */
public abstract class PersistentAttributeMapping extends IdentityMapping {
    public static final AttributeHeader PERSISTENT_OID = new AttributeHeader("oid", "Object Identifier", DataType.INTEGER, UsersOrGroups.BOTH, AttributeHeader.Builtin.BUILTIN);

    public static AttributeHeader[] getBuiltinAttributes() {
        AttributeHeader[] supers = IdentityMapping.getBuiltinAttributes();
        AttributeHeader[] my = new AttributeHeader[supers.length+2];
        System.arraycopy(supers, 0, my, 0, supers.length);
        my[my.length-2] = PERSISTENT_OID;
        my[my.length-1] = InternalAttributeMapping.INTERNAL_USER_EXPIRATION;
        return my;
    }

    protected PersistentAttributeMapping(AttributeConfig parent, long providerOid, UsersOrGroups uog) {
        super(parent, providerOid, uog);
    }
}
