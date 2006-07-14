package com.l7tech.server.identity.mapping;

import com.l7tech.identity.mapping.SecurityTokenMapping;
import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.objectmodel.EntityHeader;

public class SecurityTokenMappingManager extends HibernateEntityManager<SecurityTokenMapping, EntityHeader> {
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
