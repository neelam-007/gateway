package com.l7tech.server.identity.mapping;

import com.l7tech.identity.mapping.AttributeConfig;
import com.l7tech.objectmodel.HibernateEntityManager;

public class AttributeConfigManager extends HibernateEntityManager {
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
