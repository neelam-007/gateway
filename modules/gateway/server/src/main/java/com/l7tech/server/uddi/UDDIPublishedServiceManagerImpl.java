package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIPublishedService;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;

import java.util.*;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
public class UDDIPublishedServiceManagerImpl extends HibernateEntityManager<UDDIPublishedService, EntityHeader>
        implements UDDIPublishedServiceManager {

    public UDDIPublishedServiceManagerImpl() {
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return UDDIPublishedService.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(final UDDIPublishedService uddiPublishedService) {
        Map<String,Object> serviceOidMap = new HashMap<String, Object>();
        serviceOidMap.put("proxiedServiceInfo", uddiPublishedService.getUddiProxiedServiceInfo());
        serviceOidMap.put("wsdlServiceName", uddiPublishedService.getWsdlServiceName());

        return Arrays.asList(serviceOidMap);
    }

    @Override
    public void deleteByServiceKey(String serviceKey) throws FindException, DeleteException {
        List<UDDIPublishedService> listToDelete = findByPropertyMaybeNull("publishedServiceOid", serviceKey);
        for(UDDIPublishedService ps: listToDelete){
            delete(ps);
        }
    }
}
