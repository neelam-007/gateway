/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 14, 2009
 * Time: 12:52:53 PM
 */
package com.l7tech.server.uddi;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.gateway.common.uddi.UDDIRegistry;

import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.Arrays;

public class UDDIRegistryManagerImpl extends HibernateEntityManager<UDDIRegistry, EntityHeader>
    implements UDDIRegistryManager {

    @Override
    public String getTableName() {
        return "uddi_registries";
    }

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return UDDIRegistry.class;
    }
    
    @Override
    public Class<? extends Entity> getImpClass() {
        return UDDIRegistry.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(final UDDIRegistry uddiRegistry) {
        Map<String,Object> nameMap = new HashMap<String, Object>();
        nameMap.put("name", uddiRegistry.getName());
        
        Map<String,Object> baseUrlMap = new HashMap<String, Object>();
        baseUrlMap.put("baseUrl", uddiRegistry.getBaseUrl());

        return Arrays.asList(nameMap, baseUrlMap);
    }

    @Override
    protected String describeAttributes(Collection<Map<String, Object>> maps) {
        return "UDDI Registry Name and the UDDI Base URL are unique across all UDDI Registries";
    }
}
