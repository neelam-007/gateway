/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.identity.mapping;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.policy.variable.DataType;

/**
 * Mappings of {@link AttributeConfig}s to Internal Identity Providers
 * @author alex
 */
public class InternalAttributeMapping extends PersistentAttributeMapping {
    public static final AttributeHeader INTERNAL_USER_EXPIRATION = new AttributeHeader("expiration", "Expiration Date", DataType.DATE_TIME, UsersOrGroups.USERS, AttributeHeader.Builtin.BUILTIN);

    public static AttributeHeader[] getBuiltinAttributes() {
        AttributeHeader[] superAtts = PersistentAttributeMapping.getBuiltinAttributes();
        AttributeHeader[] atts = new AttributeHeader[superAtts.length + 1];
        System.arraycopy(superAtts, 0, atts, 0, superAtts.length);
        atts[atts.length-1] = INTERNAL_USER_EXPIRATION;
        return atts;
    }

    public InternalAttributeMapping() {
        super(null, IdentityProviderConfig.DEFAULT_OID, UsersOrGroups.USERS);
    }

    public InternalAttributeMapping(AttributeConfig parent, UsersOrGroups uog) {
        super(parent, IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, uog);
    }
}
