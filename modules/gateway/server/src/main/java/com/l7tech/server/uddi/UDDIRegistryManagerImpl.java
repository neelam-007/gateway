/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 14, 2009
 * Time: 12:52:53 PM
 */
package com.l7tech.server.uddi;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIProxiedService;

import java.util.*;

public class UDDIRegistryManagerImpl extends HibernateEntityManager<UDDIRegistry, EntityHeader>
    implements UDDIRegistryManager {

    private UDDIProxiedServiceManager proxiedServiceManager;

    public UDDIRegistryManagerImpl(UDDIProxiedServiceManager proxiedServiceManager) {
        this.proxiedServiceManager = proxiedServiceManager;
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

    @Override
    public Collection<UDDIProxiedService> findAllByRegistryOid(long registryOid) throws FindException {
        UDDIRegistry uddiRegistry = findByPrimaryKey(registryOid);
        if(uddiRegistry == null) throw new FindException("Could not find UDDI Registry");

        Collection<UDDIProxiedService> allProxiedServices = proxiedServiceManager.findAll();
        List<UDDIProxiedService> returnList = new ArrayList<UDDIProxiedService>();
        for(UDDIProxiedService proxiedService: allProxiedServices){
            if(proxiedService.getUddiRegistryOid() != uddiRegistry.getOid()) continue;
            returnList.add(proxiedService);
        }
        return returnList;
    }
}
