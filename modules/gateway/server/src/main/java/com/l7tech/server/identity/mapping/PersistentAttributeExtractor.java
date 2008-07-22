/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.mapping;

import com.l7tech.identity.mapping.PersistentAttributeMapping;
import com.l7tech.objectmodel.AttributeHeader;
import com.l7tech.identity.Identity;
import com.l7tech.objectmodel.PersistentEntity;

/**
 * @author alex
 */
abstract class PersistentAttributeExtractor<MT extends PersistentAttributeMapping>
        extends DefaultAttributeExtractor<MT> 
{
    protected PersistentAttributeExtractor(MT mapping) {
        super(mapping);
    }

    public Object[] extractValues(Identity identity) {
        Object[] supers = super.extractValues(identity);
        if (supers != null && supers.length > 0) return supers;

        AttributeHeader header = mapping.getAttributeConfig().getHeader();
        if (identity instanceof PersistentEntity) {
            if (header == PersistentAttributeMapping.PERSISTENT_OID) return a(((PersistentEntity)identity).getOid());
        }
        return EMPTY;
    }
}
