package com.l7tech.server.identity.mapping;

import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.mapping.SecurityTokenMapping;

public class SecurityTokenMappingManager extends HibernateEntityManager {
    public SecurityTokenMapping findByPrimaryKey(long oid) throws FindException {
        return (SecurityTokenMapping)findByPrimaryKey(getInterfaceClass(), oid);
    }

    public Class getImpClass() {
        return SecurityTokenMapping.class;
    }

    public Class getInterfaceClass() {
        return SecurityTokenMapping.class;
    }

    public String getTableName() {
        return "token_mappings";
    }
}
