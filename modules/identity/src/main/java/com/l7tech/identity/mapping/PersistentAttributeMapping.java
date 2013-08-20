/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.identity.mapping;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.variable.DataType;
import com.l7tech.objectmodel.UsersOrGroups;
import com.l7tech.objectmodel.AttributeHeader;

/**
 * Abstract superclass of {@link InternalAttributeMapping} and {@link FederatedAttributeMapping}, incorporating
 * attributes common to both 
 * TODO support custom internal attributes someday
 * @author alex
 */
public abstract class PersistentAttributeMapping extends IdentityMapping {
    @Deprecated
    public static final AttributeHeader PERSISTENT_OID = new AttributeHeader("oid", "Object Identifier", DataType.INTEGER, UsersOrGroups.BOTH, AttributeHeader.Builtin.BUILTIN);
    public static final AttributeHeader PERSISTENT_GOID = new AttributeHeader("goid", "Object Identifier", DataType.STRING, UsersOrGroups.BOTH, AttributeHeader.Builtin.BUILTIN);
    public static final AttributeHeader PERSISTENT_USER_EXPIRATION = new AttributeHeader("expiration", "Expiration Date", DataType.DATE_TIME, UsersOrGroups.USERS, AttributeHeader.Builtin.BUILTIN);

    public static AttributeHeader[] getBuiltinAttributes() {
        AttributeHeader[] supers = IdentityMapping.getBuiltinAttributes();
        AttributeHeader[] my = new AttributeHeader[supers.length+2];
        System.arraycopy(supers, 0, my, 0, supers.length);
        my[my.length-2] = PERSISTENT_GOID;
        my[my.length-1] = PERSISTENT_USER_EXPIRATION;
        return my;
    }

    protected PersistentAttributeMapping(AttributeConfig parent, Goid providerOid, UsersOrGroups uog) {
        super(parent, providerOid, uog);
    }
}
