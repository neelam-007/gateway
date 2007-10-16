/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.mapping;

import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.identity.mapping.LdapAttributeMapping;
import com.l7tech.identity.mapping.InternalAttributeMapping;
import com.l7tech.identity.mapping.FederatedAttributeMapping;

/**
 * Creates the appropriate {@link AttributeExtractor} implementation for an {@link IdentityMapping}.
 * @author alex
 */
public final class ExtractorFactory {
    public static AttributeExtractor getExtractor(IdentityMapping mapping) {
        if (mapping instanceof LdapAttributeMapping) {
            LdapAttributeMapping lm = (LdapAttributeMapping) mapping;
            return new LdapAttributeExtractor(lm);
        } else if (mapping instanceof InternalAttributeMapping) {
            InternalAttributeMapping im = (InternalAttributeMapping) mapping;
            return new InternalAttributeExtractor(im);
        } else if (mapping instanceof FederatedAttributeMapping) {
            FederatedAttributeMapping fm = (FederatedAttributeMapping) mapping;
            return new FederatedAttributeExtractor(fm);
        } else {
            throw new IllegalArgumentException("Can't make an extractor for " + mapping.getClass().getName());
        }
    }
}
