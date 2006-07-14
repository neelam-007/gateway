package com.l7tech.server.identity.mapping;

import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.objectmodel.EntityHeader;

public class IdentityMappingManager extends HibernateEntityManager<IdentityMapping, EntityHeader> {
    public Class getImpClass() {
        return IdentityMapping.class;
    }

    public Class getInterfaceClass() {
        return IdentityMapping.class;
    }

    public String getTableName() {
        return "identity_mappings";
    }
}
