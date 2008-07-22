/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.mapping;

import com.l7tech.identity.mapping.FederatedAttributeMapping;
import com.l7tech.objectmodel.AttributeHeader;
import com.l7tech.identity.Identity;
import com.l7tech.identity.fed.VirtualGroup;

/**
 * @author alex
 */
public class FederatedAttributeExtractor extends PersistentAttributeExtractor<FederatedAttributeMapping> {
    public FederatedAttributeExtractor(FederatedAttributeMapping mapping) {
        super(mapping);
    }

    @Override
    public Object[] extractValues(Identity identity) {
        Object[] supers = super.extractValues(identity);
        if (supers != null && supers.length > 0) return supers;

        if (identity instanceof VirtualGroup) {
            // FederatedUser and non-virtual FederatedGroup don't add any properties to their superclasses
            VirtualGroup virtualGroup = (VirtualGroup) identity;
            AttributeHeader header = mapping.getAttributeConfig().getHeader();
            if (header == FederatedAttributeMapping.VIRTUAL_SAML_PATTERN) return a(virtualGroup.getSamlEmailPattern());
            if (header == FederatedAttributeMapping.VIRTUAL_X509_PATTERN) return a(virtualGroup.getX509SubjectDnPattern());
        }
        return EMPTY;
    }
}
