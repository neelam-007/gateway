package com.l7tech.server.identity.mapping;

import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.mapping.IdentityMapping;

public class IdentityMappingManager extends HibernateEntityManager {
    public IdentityMapping findByPrimaryKey(long oid) throws FindException {
        return (IdentityMapping)findByPrimaryKey(getInterfaceClass(), oid);
    }

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
