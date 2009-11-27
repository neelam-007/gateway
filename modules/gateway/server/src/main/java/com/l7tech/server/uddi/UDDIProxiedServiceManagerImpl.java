package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;

import java.util.*;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
public class UDDIProxiedServiceManagerImpl extends HibernateEntityManager<UDDIProxiedService, EntityHeader>
        implements UDDIProxiedServiceManager{

    public UDDIProxiedServiceManagerImpl() {
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return UDDIProxiedService.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(final UDDIProxiedService uddiProxiedService) {
        Map<String,Object> serviceOidMap = new HashMap<String, Object>();
        serviceOidMap.put("proxiedServiceInfo", uddiProxiedService.getUddiProxiedServiceInfo());
        serviceOidMap.put("wsdlServiceName", uddiProxiedService.getWsdlServiceName());

        return Arrays.asList(serviceOidMap);
    }

    @Override
    public void deleteByServiceKey(String serviceKey) throws FindException, DeleteException {
        List<UDDIProxiedService> listToDelete = findByPropertyMaybeNull("publishedServiceOid", serviceKey);
        for(UDDIProxiedService ps: listToDelete){
            delete(ps);
        }
    }
}
