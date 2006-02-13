package com.l7tech.server.identity.mapping;

import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.mapping.AttributeConfig;

public class AttributeConfigManager extends HibernateEntityManager {
    public AttributeConfig findByPrimaryKey(long oid) throws FindException {
        return (AttributeConfig)findByPrimaryKey(getInterfaceClass(), oid);
    }

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
