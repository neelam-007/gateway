package com.l7tech.server.identity.mapping;

import com.l7tech.identity.mapping.AttributeConfig;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.objectmodel.EntityHeader;

public class AttributeConfigManagerImpl
        extends HibernateEntityManager<AttributeConfig, EntityHeader>
        implements AttributeConfigManager 
{
    public Class getImpClass() {
        return AttributeConfig.class;
    }

    public Class getInterfaceClass() {
        return AttributeConfig.class;
    }

    public String getTableName() {
        return "attributes";
    }
}
