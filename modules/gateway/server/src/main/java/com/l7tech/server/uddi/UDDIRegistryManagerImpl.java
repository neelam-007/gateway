/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;

import java.util.*;

public class UDDIRegistryManagerImpl extends HibernateEntityManager<UDDIRegistry, EntityHeader>
    implements UDDIRegistryManager {

    private UDDIProxiedServiceInfoManager proxiedServiceInfoManager;

    public UDDIRegistryManagerImpl(UDDIProxiedServiceInfoManager proxiedServiceInfoManager) {
        this.proxiedServiceInfoManager = proxiedServiceInfoManager;
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
    public Collection<UDDIProxiedServiceInfo> findAllByRegistryOid(long registryOid) throws FindException {
        UDDIRegistry uddiRegistry = findByPrimaryKey(registryOid);
        if(uddiRegistry == null) throw new FindException("Could not find UDDI Registry");

        Collection<UDDIProxiedServiceInfo> allProxiedServices = proxiedServiceInfoManager.findAll();
        List<UDDIProxiedServiceInfo> returnList = new ArrayList<UDDIProxiedServiceInfo>();
        for(UDDIProxiedServiceInfo proxiedService: allProxiedServices){
            if(proxiedService.getUddiRegistryOid() != uddiRegistry.getOid()) continue;
            returnList.add(proxiedService);
        }
        return returnList;
    }
}
