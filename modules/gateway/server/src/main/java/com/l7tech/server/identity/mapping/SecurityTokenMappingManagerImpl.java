package com.l7tech.server.identity.mapping;

import com.l7tech.identity.mapping.SecurityTokenMapping;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.objectmodel.EntityHeader;

public class SecurityTokenMappingManagerImpl
        extends HibernateEntityManager<SecurityTokenMapping, EntityHeader>
        implements SecurityTokenMappingManager
{
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
