/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.mapping;

import com.l7tech.identity.mapping.PersistentAttributeMapping;
import com.l7tech.objectmodel.AttributeHeader;
import com.l7tech.identity.Identity;
import com.l7tech.objectmodel.GoidEntity;
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
        if (identity instanceof GoidEntity) {
            if (header == PersistentAttributeMapping.PERSISTENT_OID || header == PersistentAttributeMapping.PERSISTENT_GOID) return a(((GoidEntity)identity).getGoid());
        }
        return EMPTY;
    }
}
