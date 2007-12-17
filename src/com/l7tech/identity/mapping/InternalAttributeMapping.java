/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.identity.mapping;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;

/**
 * Mappings of {@link AttributeConfig}s to Internal Identity Providers
 * @author alex
 */
public class InternalAttributeMapping extends PersistentAttributeMapping {
    public static final AttributeHeader INTERNAL_USER_EXPIRATION = PersistentAttributeMapping.PERSISTENT_USER_EXPIRATION;

    public static AttributeHeader[] getBuiltinAttributes() {
        return PersistentAttributeMapping.getBuiltinAttributes();
    }

    public InternalAttributeMapping() {
        super(null, IdentityProviderConfig.DEFAULT_OID, UsersOrGroups.USERS);
    }

    public InternalAttributeMapping(AttributeConfig parent, UsersOrGroups uog) {
        super(parent, IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, uog);
    }
}
