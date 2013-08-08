/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.HibernateGoidEntityManager;

import java.util.*;

public class UDDIRegistryManagerImpl extends HibernateGoidEntityManager<UDDIRegistry, EntityHeader>
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
        
        return Arrays.asList(nameMap);
    }

    @Override
    protected String describeAttributes(Collection<Map<String, Object>> maps) {
        return "UDDI Registry Name must be unique across all UDDI Registries";
    }

    @Override
    public Collection<UDDIProxiedServiceInfo> findAllByRegistryGoid(Goid registryGoid) throws FindException {
        UDDIRegistry uddiRegistry = findByPrimaryKey(registryGoid);
        if(uddiRegistry == null) throw new FindException("Could not find UDDI Registry");

        Collection<UDDIProxiedServiceInfo> allProxiedServices = proxiedServiceInfoManager.findAll();
        List<UDDIProxiedServiceInfo> returnList = new ArrayList<UDDIProxiedServiceInfo>();
        for(UDDIProxiedServiceInfo proxiedService: allProxiedServices){
            if(!Goid.equals(proxiedService.getUddiRegistryGoid(), uddiRegistry.getGoid())) continue;
            returnList.add(proxiedService);
        }
        return returnList;
    }
}
