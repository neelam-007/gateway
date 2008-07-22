/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.mapping;

import com.l7tech.identity.Identity;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.mapping.InternalAttributeMapping;
import com.l7tech.objectmodel.AttributeHeader;

import java.util.Date;

/**
 * @author alex
 */
public class InternalAttributeExtractor
        extends PersistentAttributeExtractor<InternalAttributeMapping>
        implements AttributeExtractor
{
    public InternalAttributeExtractor(InternalAttributeMapping mapping) {
        super(mapping);
    }

    public Object[] extractValues(Identity identity) {
        Object[] vals = super.extractValues(identity);
        if (vals != null && vals.length > 0) return vals;
        AttributeHeader header = mapping.getAttributeConfig().getHeader();
        if (identity instanceof InternalUser) {
            InternalUser internalUser = (InternalUser) identity;
            if (header == InternalAttributeMapping.INTERNAL_USER_EXPIRATION) return a(new Date(internalUser.getExpiration()));
            if (header == AttributeHeader.L7_DIGEST_PASSWORD) return a(internalUser.getHashedPassword());
        } // No custom properties in InternalGroup, we're OK with super here
        return EMPTY;
    }
}
